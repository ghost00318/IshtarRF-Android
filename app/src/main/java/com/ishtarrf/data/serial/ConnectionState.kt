package com.ishtarrf.data.serial

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val deviceName: String) : ConnectionState
    data class Error(val message: String) : ConnectionState

    val isConnected: Boolean get() = this is Connected
}
