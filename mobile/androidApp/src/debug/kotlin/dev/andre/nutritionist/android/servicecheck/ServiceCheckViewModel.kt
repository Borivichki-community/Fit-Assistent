package dev.andre.nutritionist.android.servicecheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andre.nutritionist.core.kotea.KoteaStoreFactory
import dev.andre.nutritionist.core.kotea.Store

internal class ServiceCheckViewModel(
    target: ServiceCheckTarget,
    commandsFlowHandler: ServiceCheckCommandsFlowHandler,
) : ViewModel() {
    private val store: Store<ServiceCheckState, ServiceCheckEvent.Ui, Nothing> =
        KoteaStoreFactory.create(
            initialState = ServiceCheckState(target),
            update = ServiceCheckUpdate(),
            commandsFlowHandler = commandsFlowHandler,
            scope = viewModelScope,
        )

    val state = store.state

    fun dispatch(event: ServiceCheckEvent.Ui) = store.dispatch(event)
}
