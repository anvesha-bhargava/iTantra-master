package com.iTantra.app.transport.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothPermissionHelper {

    fun isBluetoothSupported():
            Boolean =
        BluetoothAdapter
            .getDefaultAdapter() !=
                null

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled():
            Boolean =
        try {
            BluetoothAdapter
                .getDefaultAdapter()
                ?.isEnabled ==
                    true
        } catch (_: Exception) {
            false
        }

    fun getRequiredPermissions():
            Array<String> =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    fun hasRequiredPermissions(
        context: Context
    ): Boolean =
        getRequiredPermissions()
            .all { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) ==
                        PackageManager.PERMISSION_GRANTED
            }

    @SuppressLint("MissingPermission")
    fun getPairedDevices():
            Set<BluetoothDevice> =
        try {
            BluetoothAdapter
                .getDefaultAdapter()
                ?.bondedDevices
                ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
}
