package com.iTantra.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.iTantra.app.domain.AudioData

class AudioRecorder(
    private val context: Context
) {

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNEL_COUNT = 1
        private const val TAG = "AudioRecorder"
    }

    private val channelConfig =
        AudioFormat.CHANNEL_IN_MONO

    private val encoding =
        AudioFormat.ENCODING_PCM_16BIT

    @Volatile
    private var activeRecorder:
            AudioRecord? = null

    @Volatile
    private var recording =
        false

    /**
     * Blocks until stop() is called or maxDurationMs is reached.
     * Call this from a background coroutine.
     */
    fun record(
        maxDurationMs: Long = 15_000L
    ): AudioData {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException(
                "RECORD_AUDIO permission has not been granted"
            )
        }

        check(!recording) {
            "Audio recording is already in progress"
        }

        val minBufferBytes =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                channelConfig,
                encoding
            )

        require(minBufferBytes > 0) {
            "Unable to determine AudioRecord buffer size"
        }

        val maximumSamples =
            (
                    SAMPLE_RATE *
                            maxDurationMs /
                            1000L
                    )
                .toInt()
                .coerceAtLeast(SAMPLE_RATE)

        val samples =
            ShortArray(maximumSamples)

        val recorder =
            AudioRecord.Builder()
                .setAudioSource(
                    MediaRecorder.AudioSource.MIC
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(encoding)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(
                    maxOf(
                        minBufferBytes * 2,
                        4096
                    )
                )
                .build()

        if (
            recorder.state !=
            AudioRecord.STATE_INITIALIZED
        ) {
            recorder.release()
            error("AudioRecord failed to initialize")
        }

        activeRecorder =
            recorder

        recording =
            true

        var offset =
            0

        try {
            recorder.startRecording()

            while (
                recording &&
                offset < samples.size
            ) {
                val count =
                    recorder.read(
                        samples,
                        offset,
                        samples.size - offset,
                        AudioRecord.READ_BLOCKING
                    )

                when {
                    count > 0 ->
                        offset += count

                    count == 0 ->
                        Unit

                    else ->
                        error(
                            "AudioRecord.read failed: $count"
                        )
                }
            }
        } finally {
            recording =
                false

            try {
                if (
                    recorder.recordingState ==
                    AudioRecord.RECORDSTATE_RECORDING
                ) {
                    recorder.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recorder", e)
            }

            try {
                recorder.release()
            } catch (_: Exception) {
            }

            activeRecorder =
                null
        }

        return AudioData(
            samples =
                if (offset == samples.size) {
                    samples
                } else {
                    samples.copyOf(offset)
                },
            sampleRate =
                SAMPLE_RATE,
            channelCount =
                CHANNEL_COUNT
        )
    }

    fun stop() {
        recording =
            false

        try {
            activeRecorder?.let { recorder ->
                if (
                    recorder.recordingState ==
                    AudioRecord.RECORDSTATE_RECORDING
                ) {
                    recorder.stop()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping active recorder", e)
        }
    }
}
