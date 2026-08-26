package com.iTantra.app.ml.stt

import com.iTantra.app.domain.AudioData

/**
 * Filename is kept as SpeetToText.kt to match the repository.
 * The interface itself is correctly named SpeechToText.
 */
interface SpeechToText {

    fun transcribe(
        audio: AudioData
    ): String

    fun release()
}
