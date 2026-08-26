package com.iTantra.app.ml.tts

data class SynthesizedAudio(
    val samples: FloatArray,
    val sampleRate: Int
)

interface TextToSpeech {

    fun synthesize(
        text: String
    ): SynthesizedAudio

    fun release()
}
