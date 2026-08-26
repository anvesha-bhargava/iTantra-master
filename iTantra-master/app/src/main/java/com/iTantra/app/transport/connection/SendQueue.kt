package com.iTantra.app.transport.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * Serializes writes to one RFCOMM OutputStream.
 */
class SendQueue {

    private val mutex =
        Mutex()

    suspend fun enqueueAndWrite(
        outputStream: OutputStream?,
        bytes: ByteArray
    ): Boolean =
        withContext(
            Dispatchers.IO
        ) {
            if (outputStream == null) {
                return@withContext false
            }

            mutex.withLock {
                try {
                    outputStream.write(
                        bytes
                    )

                    outputStream.flush()

                    true
                } catch (_: Exception) {
                    false
                }
            }
        }
}
