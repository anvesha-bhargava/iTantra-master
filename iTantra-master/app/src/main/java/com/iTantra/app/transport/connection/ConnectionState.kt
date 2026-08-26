package com.iTantra.app.transport.connection

enum class ConnectionState {
    UNAVAILABLE,
    WAITING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}