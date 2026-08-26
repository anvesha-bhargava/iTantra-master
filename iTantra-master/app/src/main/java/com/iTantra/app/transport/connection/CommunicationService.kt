package com.iTantra.app.transport.connection

import android.bluetooth.BluetoothDevice
import com.iTantra.app.transport.protocol.Message
import com.iTantra.app.transport.protocol.MessageSerializer
import com.iTantra.app.transport.protocol.ProtocolEnvelope
import com.iTantra.app.transport.protocol.ProtocolMessageType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

/**
 * Direct one-to-one transport plus controlled-flooding Room mesh transport.
 */
class CommunicationService(
    private val scope: CoroutineScope,
    private val ackTimeoutMs: Long = 2_000L,
    private val maxRetries: Int = 3
) {

    private val bluetoothManager =
        BluetoothManager(
            scope
        )

    val connectionState:
            StateFlow<ConnectionState> =
        bluetoothManager
            .connectionState

    val connectedPeers:
            StateFlow<List<ConnectedPeer>> =
        bluetoothManager
            .connectedPeers

    private val _incomingMessages =
        MutableSharedFlow<Message>(
            extraBufferCapacity =
                128
        )

    val incomingMessages:
            SharedFlow<Message> =
        _incomingMessages
            .asSharedFlow()

    private val _incomingRoomSignals =
        MutableSharedFlow<Message>(
            extraBufferCapacity =
                128
        )

    val incomingRoomSignals:
            SharedFlow<Message> =
        _incomingRoomSignals
            .asSharedFlow()

    private val _sendFailures =
        MutableSharedFlow<String>(
            extraBufferCapacity =
                64
        )

    val sendFailures:
            SharedFlow<String> =
        _sendFailures
            .asSharedFlow()

    private val pendingAcks =
        ConcurrentHashMap<
                String,
                CompletableDeferred<Unit>
                >()

    /**
     * Loop prevention for Room flooding.
     */
    private val processedMessageIds =
        ConcurrentHashMap
            .newKeySet<String>()

    private var processorJob:
            Job? = null

    init {
        startIncomingProcessor()
    }

    fun startServer() {
        bluetoothManager
            .startListeningServer()
    }

    fun connectToDevice(
        device: BluetoothDevice
    ) {
        bluetoothManager
            .connectToDevice(
                device
            )
    }

    private fun startIncomingProcessor() {
        processorJob =
            scope.launch(
                Dispatchers.Default
            ) {
                bluetoothManager
                    .incomingFrames
                    .collect {
                            peerFrame ->

                        val envelope =
                            MessageSerializer
                                .deserializeEnvelope(
                                    peerFrame.frame
                                )
                                ?: return@collect

                        when (
                            envelope.type
                        ) {
                            ProtocolMessageType.ACK -> {
                                envelope
                                    .ackMessageId
                                    ?.let {
                                        pendingAcks[
                                            it
                                        ]?.complete(
                                            Unit
                                        )
                                    }
                            }

                            ProtocolMessageType.DATA -> {
                                val message =
                                    envelope.message
                                        ?: return@collect

                                /*
                                 * Always ACK the direct hop before duplicate filtering.
                                 * A retry of an already-seen direct packet must still get ACKed.
                                 */
                                sendAck(
                                    peerAddress =
                                        peerFrame.peerAddress,
                                    messageId =
                                        message.id
                                )

                                val isNew =
                                    processedMessageIds
                                        .add(
                                            message.id
                                        )

                                if (!isNew) {
                                    return@collect
                                }

                                /*
                                 * Relay Room traffic to every neighbour except
                                 * the neighbour from which this copy arrived.
                                 */
                                if (
                                    !message.roomId
                                        .isNullOrBlank() &&
                                    message.ttl > 0
                                ) {
                                    val forwarded =
                                        message.copy(
                                            ttl =
                                                message.ttl - 1
                                        )

                                    bluetoothManager.broadcast(
                                        bytes =
                                            MessageSerializer
                                                .serializeEnvelopeToBytes(
                                                    ProtocolEnvelope(
                                                        envelopeId =
                                                            "env-${forwarded.id}",
                                                        type =
                                                            ProtocolMessageType.DATA,
                                                        message =
                                                            forwarded
                                                    )
                                                ),
                                        excludePeerAddress =
                                            peerFrame.peerAddress
                                    )
                                }

                                if (
                                    message.type in
                                    Message.ROOM_CONTROL_TYPES
                                ) {
                                    _incomingRoomSignals.emit(
                                        message
                                    )
                                } else {
                                    _incomingMessages.emit(
                                        message
                                    )
                                }
                            }

                            ProtocolMessageType.ROOM_SIGNAL -> {
                                /*
                                 * Retained for compatibility with older packets.
                                 * The new Room implementation uses typed DATA packets.
                                 */
                            }
                        }
                    }
            }
    }

    private fun sendAck(
        peerAddress: String,
        messageId: String
    ) {
        scope.launch(
            Dispatchers.IO
        ) {
            val envelope =
                ProtocolEnvelope(
                    envelopeId =
                        "env-ack-$messageId",
                    type =
                        ProtocolMessageType.ACK,
                    ackMessageId =
                        messageId
                )

            bluetoothManager
                .sendToPeer(
                    peerAddress =
                        peerAddress,
                    bytes =
                        MessageSerializer
                            .serializeEnvelopeToBytes(
                                envelope
                            )
                )
        }
    }

    /**
     * Reliable one-to-one send to one selected Bluetooth neighbour.
     */
    suspend fun sendDirectMessage(
        peerAddress: String,
        message: Message
    ): Boolean =
        withContext(
            Dispatchers.IO
        ) {
            if (peerAddress.isBlank()) {
                return@withContext false
            }

            val direct =
                message.copy(
                    type =
                        Message.TYPE_NORMAL,
                    targetNodeId =
                        null,
                    roomId =
                        null,
                    roomName =
                        null,
                    hostNodeId =
                        null,
                    ttl =
                        0
                )

            val bytes =
                MessageSerializer
                    .serializeEnvelopeToBytes(
                        ProtocolEnvelope(
                            envelopeId =
                                "env-${direct.id}",
                            type =
                                ProtocolMessageType.DATA,
                            message =
                                direct
                        )
                    )

            repeat(
                maxRetries
            ) {
                val ack =
                    CompletableDeferred<Unit>()

                pendingAcks[
                    direct.id
                ] = ack

                val sent =
                    bluetoothManager
                        .sendToPeer(
                            peerAddress,
                            bytes
                        )

                if (sent) {
                    try {
                        withTimeout(
                            ackTimeoutMs
                        ) {
                            ack.await()
                        }

                        pendingAcks.remove(
                            direct.id
                        )

                        return@withContext true
                    } catch (_: Exception) {
                        // Retry.
                    } finally {
                        pendingAcks.remove(
                            direct.id
                        )
                    }
                } else {
                    pendingAcks.remove(
                        direct.id
                    )
                }
            }

            _sendFailures.emit(
                direct.id
            )

            false
        }

    /**
     * Injects a Room packet into the multi-hop peer graph.
     */
    suspend fun broadcastRoomMessage(
        message: Message
    ): Boolean =
        withContext(
            Dispatchers.IO
        ) {
            require(
                !message.roomId
                    .isNullOrBlank()
            ) {
                "roomId is required for mesh broadcast"
            }

            processedMessageIds.add(
                message.id
            )

            val envelope =
                ProtocolEnvelope(
                    envelopeId =
                        "env-${message.id}",
                    type =
                        ProtocolMessageType.DATA,
                    message =
                        message
                )

            bluetoothManager
                .broadcast(
                    MessageSerializer
                        .serializeEnvelopeToBytes(
                            envelope
                        )
                ) > 0
        }

    /**
     * Compatibility helper. Sends one-to-one to the first connected neighbour.
     */
    suspend fun sendMessage(
        message: Message
    ): Boolean {
        val firstPeer =
            connectedPeers.value
                .firstOrNull()
                ?: return false

        return sendDirectMessage(
            peerAddress =
                firstPeer.address,
            message =
                message
        )
    }

    fun clearDuplicateHistory() {
        processedMessageIds.clear()
    }

    fun disconnect() {
        try {
            processorJob?.cancel()
        } catch (_: Exception) {
        }

        bluetoothManager
            .disconnect()
    }
}
