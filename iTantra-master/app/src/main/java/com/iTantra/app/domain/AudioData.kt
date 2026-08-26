package com.iTantra.app.domain

data class AudioData(
    val samples: ShortArray,
    val sampleRate: Int = 16_000,
    val channelCount: Int = 1
)
