package com.iTantra.app.transport.protocol

import org.json.JSONObject
import java.nio.charset.StandardCharsets

object MessageSerializer {

    const val DELIMITER =
        "\n"

    fun serializeEnvelope(
        envelope: ProtocolEnvelope
    ): String =
        JSONObject()
            .apply {
                put(
                    "envelopeId",
                    envelope.envelopeId
                )

                put(
                    "type",
                    envelope.type.name
                )

                envelope
                    .ackMessageId
                    ?.let {
                        put(
                            "ackMessageId",
                            it
                        )
                    }

                envelope
                    .message
                    ?.let {
                        put(
                            "message",
                            serializeMessageToJson(
                                it
                            )
                        )
                    }

                envelope
                    .roomFrame
                    ?.let {
                        put(
                            "roomFrame",
                            it
                        )
                    }
            }
            .toString() +
                DELIMITER

    fun serializeEnvelopeToBytes(
        envelope: ProtocolEnvelope
    ): ByteArray =
        serializeEnvelope(
            envelope
        ).toByteArray(
            StandardCharsets.UTF_8
        )

    fun serializeMessageToJson(
        message: Message
    ): JSONObject =
        JSONObject()
            .apply {
                put(
                    "id",
                    message.id
                )

                put(
                    "type",
                    message.type
                )

                put(
                    "language",
                    message.language
                )

                put(
                    "timestamp",
                    message.timestamp
                )

                put(
                    "text",
                    message.text
                )

                put(
                    "originNodeId",
                    message.originNodeId
                )

                put(
                    "ttl",
                    message.ttl
                )

                message
                    .targetNodeId
                    ?.let {
                        put(
                            "targetNodeId",
                            it
                        )
                    }

                message
                    .roomId
                    ?.let {
                        put(
                            "roomId",
                            it
                        )
                    }

                message
                    .roomName
                    ?.let {
                        put(
                            "roomName",
                            it
                        )
                    }

                message
                    .hostNodeId
                    ?.let {
                        put(
                            "hostNodeId",
                            it
                        )
                    }
            }

    fun serializeMessage(
        message: Message
    ): String =
        serializeMessageToJson(
            message
        ).toString() +
                DELIMITER

    fun deserializeEnvelope(
        raw: String
    ): ProtocolEnvelope? {

        val trimmed =
            raw.trim()

        if (trimmed.isEmpty()) {
            return null
        }

        return try {
            val json =
                JSONObject(
                    trimmed
                )

            if (
                json.has(
                    "envelopeId"
                ) &&
                json.has(
                    "type"
                )
            ) {
                ProtocolEnvelope(
                    envelopeId =
                        json.getString(
                            "envelopeId"
                        ),
                    type =
                        ProtocolMessageType
                            .valueOf(
                                json.getString(
                                    "type"
                                )
                            ),
                    ackMessageId =
                        json.optionalString(
                            "ackMessageId"
                        ),
                    message =
                        if (
                            json.has(
                                "message"
                            ) &&
                            !json.isNull(
                                "message"
                            )
                        ) {
                            deserializeMessageObject(
                                json.getJSONObject(
                                    "message"
                                )
                            )
                        } else {
                            null
                        },
                    roomFrame =
                        json.optionalString(
                            "roomFrame"
                        )
                )
            } else if (
                json.has(
                    "id"
                ) &&
                json.has(
                    "text"
                )
            ) {
                val message =
                    deserializeMessageObject(
                        json
                    ) ?: return null

                ProtocolEnvelope(
                    envelopeId =
                        "env-${message.id}",
                    type =
                        ProtocolMessageType.DATA,
                    message =
                        message
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deserializeMessage(
        raw: String
    ): Message? =
        deserializeEnvelope(
            raw
        )?.message

    private fun deserializeMessageObject(
        json: JSONObject
    ): Message? =
        try {
            Message(
                id =
                    json.getString(
                        "id"
                    ),
                type =
                    json.optString(
                        "type",
                        Message.TYPE_NORMAL
                    ),
                language =
                    json.optString(
                        "language",
                        "hi"
                    ),
                timestamp =
                    json.optLong(
                        "timestamp",
                        System.currentTimeMillis()
                    ),
                text =
                    json.optString(
                        "text",
                        ""
                    ),
                originNodeId =
                    json.optString(
                        "originNodeId",
                        ""
                    ),
                targetNodeId =
                    json.optionalString(
                        "targetNodeId"
                    ),
                roomId =
                    json.optionalString(
                        "roomId"
                    ),
                roomName =
                    json.optionalString(
                        "roomName"
                    ),
                hostNodeId =
                    json.optionalString(
                        "hostNodeId"
                    ),
                ttl =
                    json.optInt(
                        "ttl",
                        0
                    )
            )
        } catch (_: Exception) {
            null
        }

    private fun JSONObject.optionalString(
        key: String
    ): String? =
        if (
            has(
                key
            ) &&
            !isNull(
                key
            )
        ) {
            optString(
                key
            ).takeIf {
                it.isNotBlank()
            }
        } else {
            null
        }
}
