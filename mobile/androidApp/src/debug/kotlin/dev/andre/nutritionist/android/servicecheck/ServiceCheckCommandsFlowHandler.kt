package dev.andre.nutritionist.android.servicecheck

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalDate

internal class ServiceCheckCommandsFlowHandler(
    private val client: ServiceCheckClient,
    private val dispatchers: CoroutineDispatchers,
    private val clock: Clock,
) : CommandsFlowHandler<ServiceCheckCommand, ServiceCheckEvent> {
    override fun handle(commands: Flow<ServiceCheckCommand>): Flow<ServiceCheckEvent> = commands.map { command ->
        val timeout = if (command.action == ServiceCheckAction.GENERATE_WEEK_MENU) 65_000L else 10_000L
        try {
            withTimeoutOrNull(timeout) { withContext(dispatchers.io) { execute(command) } }
                ?: ServiceCheckEvent.Internal.Failed(command.action, "DEADLINE_EXCEEDED")
        } catch (error: CancellationException) {
            throw error
        } catch (error: StatusException) {
            ServiceCheckEvent.Internal.Failed(command.action, error.message ?: error.status.code.name)
        } catch (error: StatusRuntimeException) {
            ServiceCheckEvent.Internal.Failed(command.action, error.message ?: error.status.code.name)
        } catch (error: Exception) {
            ServiceCheckEvent.Internal.Failed(command.action, error.message ?: error.javaClass.simpleName)
        }
    }

    private suspend fun execute(command: ServiceCheckCommand): ServiceCheckEvent.Internal.Completed {
        val action = command.action
        val entry = command.entry
        val date = entry?.date ?: LocalDate.now(clock)
        val response = when (action) {
            ServiceCheckAction.HEALTH -> client.health(command.grpcPort())
            ServiceCheckAction.KEYCLOAK_DISCOVERY -> client.keycloakDiscovery()
            ServiceCheckAction.KEYCLOAK_SESSION -> client.keycloakSession()
            ServiceCheckAction.GET_PROFILE -> client.getProfile(command.grpcPort())
            ServiceCheckAction.UPDATE_PROFILE -> client.updateProfile(command.grpcPort())
            ServiceCheckAction.PREPARE_ENTRY -> {
                command.grpcPort()
                val prepared = DiaryCheckEntry.create(clock.instant())
                val details = "entry_id = ${prepared.request.entryId}\ndate = ${prepared.date} (UTC)"
                return ServiceCheckEvent.Internal.Completed(action, details, prepared)
            }
            ServiceCheckAction.ADD_ENTRY -> client.addEntry(command.grpcPort(), checkNotNull(entry))
            ServiceCheckAction.GET_DAILY_ENTRIES -> client.getDailyEntries(command.grpcPort(), date)
            ServiceCheckAction.GET_RANGE -> client.getRange(command.grpcPort(), date)
            ServiceCheckAction.DELETE_ENTRY -> {
                val deleted = checkNotNull(entry)
                val details = client.deleteEntry(command.grpcPort(), deleted)
                return ServiceCheckEvent.Internal.Completed(action, details, deleted.copy(deleted = true))
            }
            ServiceCheckAction.SEARCH_PRODUCTS -> client.searchProducts(command.grpcPort())
            ServiceCheckAction.GENERATE_WEEK_MENU -> client.generateWeekMenu(command.gatewayUrl)
        }
        return ServiceCheckEvent.Internal.Completed(action, response, entry)
    }
}
