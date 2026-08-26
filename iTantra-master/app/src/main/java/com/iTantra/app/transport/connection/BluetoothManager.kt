package com.iTantra.app.transport.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.iTantra.app.transport.bluetooth.BluetoothClient
import com.iTantra.app.transport.bluetooth.BluetoothPermissionHelper
import com.iTantra.app.transport.bluetooth.BluetoothServer
import com.iTantra.app.transport.protocol.StreamFramingBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

data class ConnectedPeer(
    val address: String,
    val name: String
)

data class PeerFrame(
    val peerAddress: String,
    val frame: String
)

/**
 * Multi-peer Bluetooth Classic RFCOMM manager.
 *
 * One device can simultaneously retain multiple direct neighbours.
 */
class BluetoothManager(
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG =
            "BluetoothMesh"
    }

    private data class PeerConnection(
        val address: String,
        val name: String,
        val socket: BluetoothSocket,
        val input: InputStream,
        val output: OutputStream,
        val framingBuffer:
        StreamFramingBuffer =
            StreamFramingBuffer(),
        val sendQueue:
        SendQueue =
            SendQueue(),
        var readJob:
        Job? = null
    )

    private val peers =
        ConcurrentHashMap<
                String,
                PeerConnection
                >()

    private val _connectionState =
        MutableStateFlow(
            if (
                BluetoothPermissionHelper
                    .isBluetoothSupported()
            ) {
                ConnectionState.DISCONNECTED
            } else {
                ConnectionState.UNAVAILABLE
            }
        )

    val connectionState:
            StateFlow<ConnectionState> =
        _connectionState
            .asStateFlow()

    private val _connectedPeers =
        MutableStateFlow<
                List<ConnectedPeer>
                >(
            emptyList()
        )

    val connectedPeers:
            StateFlow<List<ConnectedPeer>> =
        _connectedPeers
            .asStateFlow()

    private val _incomingFrames =
        MutableSharedFlow<PeerFrame>(
            extraBufferCapacity =
                128
        )

    val incomingFrames:
            SharedFlow<PeerFrame> =
        _incomingFrames
            .asSharedFlow()

    private var server:
            BluetoothServer? = null

    private var serverJob:
            Job? = null

    private val connectionJobs =
        ConcurrentHashMap<
                String,
                Job
                >()

    fun startListeningServer() {
        if (
            serverJob?.isActive ==
            true
        ) {
            return
        }

        if (
            _connectionState.value ==
            ConnectionState.UNAVAILABLE
        ) {
            return
        }

        val bluetoothServer =
            BluetoothServer()

        server =
            bluetoothServer

        if (peers.isEmpty()) {
            _connectionState.value =
                ConnectionState.WAITING
        }

        serverJob =
            scope.launch(
                Dispatchers.IO
            ) {
                try {
                    bluetoothServer.listenLoop {
                            socket ->
                        registerSocket(
                            socket
                        )
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Bluetooth server failed",
                        e
                    )

                    if (peers.isEmpty()) {
                        _connectionState.value =
                            ConnectionState.ERROR
                    }
                }
            }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        device: BluetoothDevice
    ) {
        val address =
            try {
                device.address
            } catch (_: Exception) {
                return
            }

        if (
            peers.containsKey(
                address
            ) ||
            connectionJobs[address]
                ?.isActive ==
            true
        ) {
            return
        }

        if (peers.isEmpty()) {
            _connectionState.value =
                ConnectionState.CONNECTING
        }

        val job =
            scope.launch(
                Dispatchers.IO
            ) {
                try {
                    val socket =
                        BluetoothClient()
                            .connect(
                                device
                            )

                    if (socket != null) {
                        registerSocket(
                            socket
                        )
                    } else if (peers.isEmpty()) {
                        _connectionState.value =
                            if (
                                serverJob?.isActive ==
                                true
                            ) {
                                ConnectionState.WAITING
                            } else {
                                ConnectionState.ERROR
                            }
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Bluetooth connection failed: $address",
                        e
                    )

                    if (peers.isEmpty()) {
                        _connectionState.value =
                            ConnectionState.ERROR
                    }
                } finally {
                    connectionJobs.remove(
                        address
                    )
                }
            }

        connectionJobs[
            address
        ] = job
    }

    @SuppressLint("MissingPermission")
    private fun registerSocket(
        socket: BluetoothSocket
    ) {
        try {
            val remote =
                socket.remoteDevice

            val address =
                remote.address

            val name =
                try {
                    remote.name
                        ?: address
                } catch (_: SecurityException) {
                    address
                }

            if (
                peers.containsKey(
                    address
                )
            ) {
                socket.close()
                return
            }

            val peer =
                PeerConnection(
                    address =
                        address,
                    name =
                        name,
                    socket =
                        socket,
                    input =
                        socket.inputStream,
                    output =
                        socket.outputStream
                )

            val previous =
                peers.putIfAbsent(
                    address,
                    peer
                )

            if (previous != null) {
                socket.close()
                return
            }

            peer.readJob =
                startReader(
                    peer
                )

            refreshPeerState()

            Log.d(
                TAG,
                "Peer connected: $name ($address)"
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to register peer",
                e
            )

            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun startReader(
        peer: PeerConnection
    ): Job =
        scope.launch(
            Dispatchers.IO
        ) {
            val buffer =
                ByteArray(
                    4096
                )

            try {
                while (
                    peers.containsKey(
                        peer.address
                    )
                ) {
                    val count =
                        peer.input.read(
                            buffer
                        )

                    if (count == -1) {
                        break
                    }

                    val frames =
                        peer.framingBuffer
                            .appendAndExtractFrames(
                                buffer,
                                count
                            )

                    for (frame in frames) {
                        _incomingFrames.emit(
                            PeerFrame(
                                peerAddress =
                                    peer.address,
                                frame =
                                    frame
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Peer disconnected: ${peer.address}",
                    e
                )
            } finally {
                disconnectPeer(
                    peer.address
                )
            }
        }

    suspend fun sendToPeer(
        peerAddress: String,
        bytes: ByteArray
    ): Boolean {
        val peer =
            peers[
                peerAddress
            ] ?: return false

        val success =
            peer.sendQueue
                .enqueueAndWrite(
                    peer.output,
                    bytes
                )

        if (!success) {
            disconnectPeer(
                peerAddress
            )
        }

        return success
    }

    /**
     * Sends to every direct neighbour except optionally the source neighbour.
     */
    suspend fun broadcast(
        bytes: ByteArray,
        excludePeerAddress: String? = null
    ): Int {
        var successfulWrites =
            0

        val snapshot =
            peers.values
                .toList()

        for (peer in snapshot) {
            if (
                peer.address ==
                excludePeerAddress
            ) {
                continue
            }

            if (
                peer.sendQueue
                    .enqueueAndWrite(
                        peer.output,
                        bytes
                    )
            ) {
                successfulWrites++
            }
        }

        return successfulWrites
    }

    fun disconnectPeer(
        address: String
    ) {
        val peer =
            peers.remove(
                address
            ) ?: return

        try {
            peer.readJob?.cancel()
        } catch (_: Exception) {
        }

        try {
            peer.input.close()
        } catch (_: Exception) {
        }

        try {
            peer.output.close()
        } catch (_: Exception) {
        }

        try {
            peer.socket.close()
        } catch (_: Exception) {
        }

        refreshPeerState()
    }

    private fun refreshPeerState() {
        _connectedPeers.value =
            peers.values
                .map {
                    ConnectedPeer(
                        address =
                            it.address,
                        name =
                            it.name
                    )
                }
                .sortedBy {
                    it.name
                }

        _connectionState.value =
            when {
                peers.isNotEmpty() ->
                    ConnectionState.CONNECTED

                serverJob?.isActive ==
                        true ->
                    ConnectionState.WAITING

                else ->
                    ConnectionState.DISCONNECTED
            }
    }

    fun disconnect() {
        try {
            server
                ?.stopListening()
        } catch (_: Exception) {
        }

        server =
            null

        try {
            serverJob
                ?.cancel()
        } catch (_: Exception) {
        }

        serverJob =
            null

        connectionJobs
            .values
            .forEach {
                try {
                    it.cancel()
                } catch (_: Exception) {
                }
            }

        connectionJobs.clear()

        peers.keys
            .toList()
            .forEach {
                disconnectPeer(
                    it
                )
            }

        if (
            _connectionState.value !=
            ConnectionState.UNAVAILABLE
        ) {
            _connectionState.value =
                ConnectionState.DISCONNECTED
        }
    }
}
