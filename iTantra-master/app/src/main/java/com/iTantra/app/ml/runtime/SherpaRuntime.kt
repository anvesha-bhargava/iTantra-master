package com.iTantra.app.ml.runtime

/**
 * Marker for the on-device sherpa-ONNX runtime.
 *
 * Actual recognizer/TTS objects are created by SherpaSttEngine
 * and PiperTtsEngine. Keeping this file lightweight avoids
 * creating duplicate native runtime objects.
 */
object SherpaRuntime {
    const val READY = true
}
