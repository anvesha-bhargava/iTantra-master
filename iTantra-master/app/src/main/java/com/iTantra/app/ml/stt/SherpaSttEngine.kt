package com.iTantra.app.ml.stt

import android.content.Context
import android.util.Log
import com.iTantra.app.domain.AudioData
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineDolphinModelConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig

/**
 * Offline STT using the Dolphin assets already present in this repository:
 *
 * assets/models/dolphin/model.int8.onnx
 * assets/models/dolphin/tokens.txt
 */
class SherpaSttEngine(
    context: Context
) : SpeechToText {

    companion object {
        private const val TAG =
            "SherpaSttEngine"
    }

    private val recognizer:
            OfflineRecognizer

    init {
        val modelConfig =
            OfflineModelConfig(
                dolphin =
                    OfflineDolphinModelConfig(
                        model =
                            "models/dolphin/model.int8.onnx"
                    ),
                tokens =
                    "models/dolphin/tokens.txt",
                numThreads =
                    2,
                provider =
                    "cpu"
            )

        val recognizerConfig =
            OfflineRecognizerConfig(
                featConfig =
                    FeatureConfig(
                        sampleRate =
                            16_000,
                        featureDim =
                            80
                    ),
                modelConfig =
                    modelConfig
            )

        recognizer =
            OfflineRecognizer(
                assetManager =
                    context.assets,
                config =
                    recognizerConfig
            )
    }

    override fun transcribe(
        audio: AudioData
    ): String {

        if (audio.samples.isEmpty()) {
            return ""
        }

        val stream =
            recognizer.createStream()

        return try {
            val normalized =
                FloatArray(
                    audio.samples.size
                ) { index ->
                    audio.samples[index]
                        .toFloat() /
                            32768.0f
                }

            stream.acceptWaveform(
                samples =
                    normalized,
                sampleRate =
                    audio.sampleRate
            )

            recognizer.decode(
                stream
            )

            recognizer
                .getResult(
                    stream
                )
                .text
                .trim()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Speech recognition failed",
                e
            )
            ""
        } finally {
            stream.release()
        }
    }

    override fun release() {
        recognizer.release()
    }
}
