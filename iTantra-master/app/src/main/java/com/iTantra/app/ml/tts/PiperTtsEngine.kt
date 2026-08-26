package com.iTantra.app.ml.tts

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Offline Hindi Piper/VITS TTS using the voice currently bundled in assets.
 */
class PiperTtsEngine(
    private val context: Context
) : TextToSpeech {

    private val tts:
            OfflineTts

    init {
        val espeakDataDirectory =
            copyEspeakData()

        val modelConfig =
            OfflineTtsModelConfig(
                vits =
                    OfflineTtsVitsModelConfig(
                        model =
                            "models/tts/hi-IN/hi_IN-priyamvada-medium.onnx",
                        tokens =
                            "models/tts/hi-IN/tokens.txt",
                        dataDir =
                            espeakDataDirectory
                    )
            )

        tts =
            OfflineTts(
                assetManager =
                    context.assets,
                config =
                    OfflineTtsConfig(
                        model =
                            modelConfig
                    )
            )
    }

    override fun synthesize(
        text: String
    ): SynthesizedAudio {

        if (text.isBlank()) {
            return SynthesizedAudio(
                samples =
                    FloatArray(0),
                sampleRate =
                    22_050
            )
        }

        val generated =
            tts.generate(
                text =
                    text
            )

        return SynthesizedAudio(
            samples =
                generated.samples,
            sampleRate =
                generated.sampleRate
        )
    }

    private fun copyEspeakData():
            String {

        val assetPath =
            "models/tts/hi-IN/espeak-ng-data"

        val baseDirectory =
            context.getExternalFilesDir(null)
                ?: context.filesDir

        val targetDirectory =
            File(
                baseDirectory,
                assetPath
            )

        if (!targetDirectory.exists()) {
            copyAssetDirectory(
                assetPath,
                targetDirectory
            )
        }

        return targetDirectory.absolutePath
    }

    private fun copyAssetDirectory(
        assetPath: String,
        targetDirectory: File
    ) {
        targetDirectory.mkdirs()

        val children =
            context.assets.list(
                assetPath
            ) ?: return

        for (child in children) {
            val sourcePath =
                "$assetPath/$child"

            val destination =
                File(
                    targetDirectory,
                    child
                )

            val nestedChildren =
                context.assets.list(
                    sourcePath
                )

            if (
                !nestedChildren
                    .isNullOrEmpty()
            ) {
                copyAssetDirectory(
                    sourcePath,
                    destination
                )
            } else {
                context.assets.open(
                    sourcePath
                ).use { input ->
                    FileOutputStream(
                        destination
                    ).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    override fun release() {
        tts.release()
    }
}
