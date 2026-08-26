package com.iTantra.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

class AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    fun play(
        samples: FloatArray,
        sampleRate: Int
    ) {
        if (samples.isEmpty() || sampleRate <= 0) {
            return
        }

        val minBufferBytes =
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )

        if (minBufferBytes <= 0) {
            Log.e(TAG, "Unable to determine AudioTrack buffer size")
            return
        }

        val bufferBytes =
            maxOf(
                minBufferBytes,
                samples.size * Float.SIZE_BYTES
            )

        val track =
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

        try {
            track.play()

            val written =
                track.write(
                    samples,
                    0,
                    samples.size,
                    AudioTrack.WRITE_BLOCKING
                )

            Log.d(
                TAG,
                "Played $written / ${samples.size} samples"
            )

            val durationMs =
                samples.size * 1000L / sampleRate

            if (durationMs > 0L) {
                Thread.sleep(durationMs + 80L)
            }

            if (
                track.playState ==
                AudioTrack.PLAYSTATE_PLAYING
            ) {
                track.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback failed", e)
        } finally {
            try {
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}
