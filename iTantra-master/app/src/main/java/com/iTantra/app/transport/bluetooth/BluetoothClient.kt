package com.iTantra.app.transport.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class BluetoothClient(
    private val serviceUuid: UUID =
        UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )
) {

    @SuppressLint("MissingPermission")
    suspend fun connect(
        device: BluetoothDevice
    ): BluetoothSocket? =
        withContext(
            Dispatchers.IO
        ) {
            try {
                BluetoothAdapter
                    .getDefaultAdapter()
                    ?.cancelDiscovery()
            } catch (_: Exception) {
            }

            var socket:
                    BluetoothSocket? = null

            try {
                socket =
                    device
                        .createRfcommSocketToServiceRecord(
                            serviceUuid
                        )

                socket.connect()

                socket
            } catch (_: Exception) {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }

                null
            }
        }
}
