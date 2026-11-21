package dev.andre.nutritionist.domain.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Реактивный наблюдатель состояния интернет-соединения.
 */
interface ConnectivityObserver {
    val isConnected: StateFlow<Boolean>
}
