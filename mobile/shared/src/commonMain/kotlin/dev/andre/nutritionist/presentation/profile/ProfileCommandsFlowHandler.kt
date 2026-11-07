package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class ProfileCommandsFlowHandler(
    private val profileRepository: ProfileRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<ProfileCommand, ProfileEvent> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun handle(commands: Flow<ProfileCommand>): Flow<ProfileEvent> =
        commands.flatMapLatest { command ->
            when (command) {
                is ProfileCommand.LoadProfile -> channelFlow {
                    // 1. Запуск фонового сетевого обновления без блокировки UI
                    launch(dispatchers.io) {
                        profileRepository.refreshProfile(command.userId)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                val cached = profileRepository.getProfile(command.userId)
                                if (cached == null) {
                                    send(ProfileEvent.Internal.ProfileLoadFailed(error))
                                }
                            }
                    }

                    profileRepository.observeProfile(command.userId)
                        .collect { profile ->
                            if (profile != null) {
                                send(ProfileEvent.Internal.ProfileLoaded(profile))
                            }
                        }
                }
                is ProfileCommand.RefreshProfile -> flow {
                    profileRepository.refreshProfile(command.userId)
                        .onFailure { error ->
                            if (error is CancellationException) throw error
                            emit(ProfileEvent.Internal.ProfileLoadFailed(error))
                        }
                }
            }
        }
}