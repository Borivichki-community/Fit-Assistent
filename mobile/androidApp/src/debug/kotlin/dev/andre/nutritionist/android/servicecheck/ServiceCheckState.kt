package dev.andre.nutritionist.android.servicecheck

internal data class ServiceCheckState(
    val target: ServiceCheckTarget,
    val port: String = target.defaultGrpcPort?.toString().orEmpty(),
    val entry: DiaryCheckEntry? = null,
    val gatewayUrl: String = "",
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data class Loading(val action: ServiceCheckAction) : Content
        data class Success(val action: ServiceCheckAction, val response: String) : Content
        data class Error(val action: ServiceCheckAction, val response: String) : Content
    }

    val canEditPort: Boolean
        get() = content !is Content.Loading && (entry == null || entry.deleted)

    fun canRun(action: ServiceCheckAction): Boolean {
        if (content is Content.Loading) return false
        return when (action) {
            ServiceCheckAction.PREPARE_ENTRY -> entry == null || entry.deleted
            ServiceCheckAction.ADD_ENTRY -> entry != null && !entry.deleted
            ServiceCheckAction.DELETE_ENTRY -> entry != null
            ServiceCheckAction.HEALTH,
            ServiceCheckAction.KEYCLOAK_DISCOVERY,
            ServiceCheckAction.KEYCLOAK_SESSION,
            ServiceCheckAction.GET_PROFILE,
            ServiceCheckAction.UPDATE_PROFILE,
            ServiceCheckAction.GET_DAILY_ENTRIES,
            ServiceCheckAction.GET_RANGE,
            ServiceCheckAction.SEARCH_PRODUCTS,
            ServiceCheckAction.GENERATE_WEEK_MENU,
            -> true
        }
    }
}
