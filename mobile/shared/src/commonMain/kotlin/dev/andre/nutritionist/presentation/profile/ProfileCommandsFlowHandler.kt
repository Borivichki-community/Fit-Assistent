package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ProfileCommandsFlowHandler(
    private val profileRepository: ProfileRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<ProfileCommand, ProfileEvent> {

    override fun handle(commands: Flow<ProfileCommand>): Flow<ProfileEvent> = flow {
        commands.collect { command ->
            when (command) {
                is ProfileCommand.LoadProfile -> {
                    val event = withContext(dispatchers.io) {
                        runCatching { profileRepository.getProfile(command.userId) }
                            .fold(
                                onSuccess = { ProfileEvent.Internal.ProfileLoaded(it) },
                                onFailure = { ProfileEvent.Internal.ProfileLoadFailed(it) },
                            )
                    }
                    emit(event)
                }
            }
        }
    }
}
