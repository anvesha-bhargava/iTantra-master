package com.iTantra.app.transport.protocol

/**
 * Logical iTANTRA payload.
 *
 * Direct:
 *   roomId = null
 *   ttl = 0
 *
 * Room mesh:
 *   roomId != null
 *   ttl > 0
 */
data class Message(
    val id: String,
    val type: String = TYPE_NORMAL,
    val language: String = "hi",
    val timestamp: Long =
        System.currentTimeMillis(),
    val text: String = "",
    val originNodeId: String = "",
    val targetNodeId: String? = null,
    val roomId: String? = null,
    val roomName: String? = null,
    val hostNodeId: String? = null,
    val ttl: Int = 0
) {

    companion object {
        const val TYPE_NORMAL =
            "NORMAL"

        const val TYPE_ROOM_TEXT =
            "ROOM_TEXT"

        const val TYPE_ROOM_ANNOUNCE =
            "ROOM_ANNOUNCE"

        const val TYPE_ROOM_JOIN_REQUEST =
            "ROOM_JOIN_REQUEST"

        const val TYPE_ROOM_JOIN_ACCEPT =
            "ROOM_JOIN_ACCEPT"

        const val TYPE_ROOM_JOIN_REJECT =
            "ROOM_JOIN_REJECT"

        const val TYPE_ROOM_PRESENCE =
            "ROOM_PRESENCE"

        const val TYPE_ROOM_LEAVE =
            "ROOM_LEAVE"

        val ROOM_CONTROL_TYPES =
            setOf(
                TYPE_ROOM_ANNOUNCE,
                TYPE_ROOM_JOIN_REQUEST,
                TYPE_ROOM_JOIN_ACCEPT,
                TYPE_ROOM_JOIN_REJECT,
                TYPE_ROOM_PRESENCE,
                TYPE_ROOM_LEAVE
            )
    }
}
