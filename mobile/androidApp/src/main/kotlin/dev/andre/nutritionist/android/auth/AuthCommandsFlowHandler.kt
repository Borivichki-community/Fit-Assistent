package dev.andre.nutritionist.android.auth

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext


class AuthCommandsFlowHandler(
    private val authRepository: AuthRepository,
    private val authIntentProvider: AuthIntentProvider,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<AuthCommand, AuthEvent> {

    override fun handle(commands: Flow<AuthCommand>): Flow<AuthEvent> = flow {
        commands.collect { command ->
            when (command) {
                is AuthCommand.LaunchAuthTab -> {
                    val event = withContext(dispatchers.io) {
                        runCatching { authIntentProvider.createAuthIntent() }
                            .fold(
                                onSuccess = { AuthEvent.Internal.AuthIntentPrepared(it) },
                                onFailure = { AuthEvent.Internal.AuthFailed(it) },
                            )
                    }
                    emit(event)
                }
                is AuthCommand.ProcessAuthResponse -> {
                    val event = withContext(dispatchers.io) {
                        authRepository.processAuthResponse(command.intent)
                            .fold(
                                onSuccess = { AuthEvent.Internal.AuthSuccess },
                                onFailure = { AuthEvent.Internal.AuthFailed(it) },
                            )
                    }
                    emit(event)
                }
                is AuthCommand.Logout -> {
                    val event = withContext(dispatchers.io) {
                        authRepository.logout()
                            .fold(
                                onSuccess = { AuthEvent.Internal.LoggedOut },
                                onFailure = { AuthEvent.Internal.AuthFailed(it) },
                            )
                    }
                    emit(event)
                }
            }
        }
    }
}
