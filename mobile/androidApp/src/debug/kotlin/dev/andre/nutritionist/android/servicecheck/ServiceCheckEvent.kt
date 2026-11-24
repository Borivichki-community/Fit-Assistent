package dev.andre.nutritionist.android.servicecheck

internal sealed interface ServiceCheckEvent {
    sealed interface Ui : ServiceCheckEvent {
        data class Run(val action: ServiceCheckAction) : Ui
        data class ChangePort(val port: String) : Ui
        data class ChangeGateway(val url: String) : Ui
    }

    sealed interface Internal : ServiceCheckEvent {
        data class Completed(
            val action: ServiceCheckAction,
            val response: String,
            val entry: DiaryCheckEntry?,
        ) : Internal

        data class Failed(val action: ServiceCheckAction, val response: String) : Internal
    }
}
