package dev.andre.nutritionist.android.servicecheck

internal data class ServiceCheckCommand(
    val action: ServiceCheckAction,
    val port: String,
    val entry: DiaryCheckEntry?,
    val gatewayUrl: String,
) {
    fun grpcPort(): Int = requireNotNull(port.toIntOrNull()?.takeIf { it in 1..65535 }) {
        "Port must be in 1..65535"
    }
}
