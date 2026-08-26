package com.iTantra.app.transport.protocol

enum class ProtocolMessageType {
    DATA,
    ACK,
    ROOM_SIGNAL
}

data class ProtocolEnvelope(
    val envelopeId: String,
    val type: ProtocolMessageType,
    val ackMessageId: String? = null,
    val message: Message? = null,
    val roomFrame: String? = null
)


====================================================================================================
app/src/main/java/com/iTantra/app/transport/protocol/StreamFramingBuffer.kt
====================================================================================================
package com.iTantra.app.transport.protocol

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Newline framing for RFCOMM.
 *
 * Pending data is retained as raw bytes so UTF-8 Hindi/Indic characters are
 * not corrupted when a multibyte character is split across two socket reads.
 */
class StreamFramingBuffer {

    private val pending =
        ByteArrayOutputStream()

    @Synchronized
    fun appendAndExtractFrames(
        bytes: ByteArray,
        length: Int
    ): List<String> {

        if (length <= 0) {
            return emptyList()
        }

        pending.write(
            bytes,
            0,
            length
        )

        val data =
            pending.toByteArray()

        val frames =
            mutableListOf<String>()

        var frameStart =
            0

        for (
        index in
        data.indices
        ) {
            if (
                data[index] ==
                '\n'.code.toByte()
            ) {
                if (
                    index >
                    frameStart
                ) {
                    val frame =
                        String(
                            data,
                            frameStart,
                            index -
                                    frameStart,
                            StandardCharsets.UTF_8
                        ).trim()

                    if (
                        frame.isNotEmpty()
                    ) {
                        frames.add(
                            frame
                        )
                    }
                }

                frameStart =
                    index + 1
            }
        }

        pending.reset()

        if (
            frameStart <
            data.size
        ) {
            pending.write(
                data,
                frameStart,
                data.size -
                        frameStart
            )
        }

        return frames
    }

    @Synchronized
    fun reset() {
        pending.reset()
    }
}
