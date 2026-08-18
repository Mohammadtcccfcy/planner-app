package com.example.myapplication.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AdaptiveGainProcessor {

    private val targetRms = 0.1f
    private val maxGain = 10f
    private val minGain = 0.5f

    private var currentGain = 1f

    fun process(input: ShortArray): ShortArray {

        var sum = 0.0
        for (sample in input) {
            val normalized = sample / 32768.0
            sum += normalized * normalized
        }

        val rms = sqrt(sum / input.size)

        if (rms > 0) {
            val desiredGain = (targetRms / rms).toFloat()
            val clampedGain = min(max(desiredGain, minGain), maxGain)

            // smooth transition
            currentGain = 0.9f * currentGain + 0.1f * clampedGain
        }

        val output = ShortArray(input.size)

        for (i in input.indices) {
            val amplified = (input[i] * currentGain).toInt()
            output[i] = amplified
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        return output
    }
}
