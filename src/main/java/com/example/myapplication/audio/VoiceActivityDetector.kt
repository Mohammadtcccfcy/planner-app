package com.example.myapplication.audio

import kotlin.math.abs

class VoiceActivityDetector {

    private val threshold = 1500  // قابل تنظیم
    private var speechFrames = 0
    private var silenceFrames = 0

    fun isSpeech(frame: ShortArray): Boolean {

        var energy = 0L

        for (sample in frame) {
            energy += abs(sample.toInt())
        }

        val avgEnergy = energy / frame.size

        return if (avgEnergy > threshold) {
            speechFrames++
            silenceFrames = 0
            speechFrames > 2
        } else {
            silenceFrames++
            speechFrames = 0
            false
        }
    }
}
