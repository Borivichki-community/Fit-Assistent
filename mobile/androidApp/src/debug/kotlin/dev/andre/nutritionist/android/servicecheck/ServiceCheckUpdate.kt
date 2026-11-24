package dev.andre.nutritionist.android.servicecheck

import dev.andre.nutritionist.core.kotea.DslUpdate

internal class ServiceCheckUpdate : DslUpdate<ServiceCheckState, ServiceCheckEvent, ServiceCheckCommand, Nothing>() {
    override fun NextBuilder<ServiceCheckState, ServiceCheckCommand, Nothing>.update(event: ServiceCheckEvent) {
        when (event) {
            is ServiceCheckEvent.Ui.ChangePort -> {
                if (state.canEditPort) {
                    state { copy(port = event.port, entry = null, content = ServiceCheckState.Content.Idle) }
                }
            }

            is ServiceCheckEvent.Ui.ChangeGateway -> {
                if (state.target == ServiceCheckTarget.RECIPE && state.canEditPort) {
                    state { copy(gatewayUrl = event.url, content = ServiceCheckState.Content.Idle) }
                }
            }

            is ServiceCheckEvent.Ui.Run -> {
                if (event.action in state.target.actions && state.canRun(event.action)) {
                    val command = ServiceCheckCommand(event.action, state.port, state.entry, state.gatewayUrl)
                    state { copy(content = ServiceCheckState.Content.Loading(event.action)) }
                    commands(command)
                }
            }

            is ServiceCheckEvent.Internal.Completed -> state {
                copy(
                    entry = event.entry,
                    content = ServiceCheckState.Content.Success(event.action, event.response),
                )
            }

            is ServiceCheckEvent.Internal.Failed -> state {
                copy(content = ServiceCheckState.Content.Error(event.action, event.response))
            }
        }
    }
}
