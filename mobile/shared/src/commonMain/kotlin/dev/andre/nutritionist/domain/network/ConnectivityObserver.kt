package dev.andre.nutritionist.domain.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Реактивный наблюдатель состояния сетевого подключения устройства.
 */
interface ConnectivityObserver {
    val isConnected: StateFlow<Boolean>
}
