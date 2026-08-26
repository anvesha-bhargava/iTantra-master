package com.iTantra.app.transport.connection

data class PeerFrame(

    /*
     * Bluetooth MAC address of the directly connected
     * neighbour that sent this frame.
     */

    val peerId: String,

    /*
     * Raw newline-delimited JSON frame.
     */

    val frame: String
)