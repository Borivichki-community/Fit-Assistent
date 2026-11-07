package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class ProfileCommandsFlowHandler(
    private val profileRepository: ProfileRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<ProfileCommand, ProfileEvent> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun handle(commands: Flow<ProfileCommand>): Flow<ProfileEvent> =
        commands.flatMapLatest { command ->
            when (command) {
                is ProfileCommand.LoadProfile -> {
                    val observeFlow = profileRepository.observeProfile(command.userId)
                        .filterNotNull()
                        .map { profile -> ProfileEvent.Internal.ProfileLoaded(profile) }

                    val refreshFlow = flow {
                        profileRepository.refreshProfile(command.userId)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                val cached = profileRepository.getProfile(command.userId)
                                if (cached == null) {
                                    emit(ProfileEvent.Internal.ProfileLoadFailed(error))
                                }
                            }
                    }.flowOn(dispatchers.io)

                    merge(observeFlow, refreshFlow)
                }
                is ProfileCommand.RefreshProfile -> flow {
                    profileRepository.refreshProfile(command.userId)
                        .onFailure { error ->
                            if (error is CancellationException) throw error
                            emit(ProfileEvent.Internal.ProfileLoadFailed(error))
                        }
                }.flowOn(dispatchers.io)
            }
        }
}
