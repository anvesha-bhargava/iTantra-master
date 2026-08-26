package com.iTantra.app.transport.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Persistent RFCOMM accept loop.
 *
 * Unlike the original one-client server, this remains open after accepting
 * a neighbour so the phone can participate in a multi-peer relay graph.
 */
class BluetoothServer(
    private val serviceName: String =
        "iTantraRFCOMM",
    private val serviceUuid: UUID =
        UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )
) {

    @Volatile
    private var running =
        false

    private var serverSocket:
            BluetoothServerSocket? =
        null

    @SuppressLint("MissingPermission")
    suspend fun listenLoop(
        onConnected:
        suspend (BluetoothSocket) -> Unit
    ) {
        withContext(
            Dispatchers.IO
        ) {
            val adapter =
                BluetoothAdapter
                    .getDefaultAdapter()
                    ?: return@withContext

            try {
                serverSocket =
                    adapter
                        .listenUsingRfcommWithServiceRecord(
                            serviceName,
                            serviceUuid
                        )

                running =
                    true

                while (
                    running &&
                    currentCoroutineContext()
                        .isActive
                ) {
                    val socket =
                        try {
                            serverSocket
                                ?.accept()
                        } catch (_: Exception) {
                            null
                        } ?: break

                    onConnected(
                        socket
                    )
                }
            } finally {
                running =
                    false

                closeServer()
            }
        }
    }

    fun stopListening() {
        running =
            false

        closeServer()
    }

    private fun closeServer() {
        try {
            serverSocket
                ?.close()
        } catch (_: Exception) {
        } finally {
            serverSocket =
                null
        }
    }
}
