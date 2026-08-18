package com.example.myapplication.audio

import kotlin.math.*

class SpectralAnalyzer {

    fun computeSpectralEnergy(frame: ShortArray): Double {

        val n = frame.size
        val real = DoubleArray(n)
        val imag = DoubleArray(n)

        for (i in 0 until n) {
            real[i] = frame[i].toDouble()
            imag[i] = 0.0
        }

        fft(real, imag)

        var energy = 0.0
        for (i in 0 until n / 2) {
            val magnitude = sqrt(real[i]*real[i] + imag[i]*imag[i])
            energy += magnitude
        }

        return energy / n
    }

    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n <= 1) return

        val evenReal = DoubleArray(n / 2)
        val evenImag = DoubleArray(n / 2)
        val oddReal = DoubleArray(n / 2)
        val oddImag = DoubleArray(n / 2)

        for (i in 0 until n / 2) {
            evenReal[i] = real[2 * i]
            evenImag[i] = imag[2 * i]
            oddReal[i] = real[2 * i + 1]
            oddImag[i] = imag[2 * i + 1]
        }

        fft(evenReal, evenImag)
        fft(oddReal, oddImag)

        for (k in 0 until n / 2) {
            val angle = -2 * PI * k / n
            val cos = cos(angle)
            val sin = sin(angle)

            val treal = cos * oddReal[k] - sin * oddImag[k]
            val timag = sin * oddReal[k] + cos * oddImag[k]

            real[k] = evenReal[k] + treal
            imag[k] = evenImag[k] + timag
            real[k + n/2] = evenReal[k] - treal
            imag[k + n/2] = evenImag[k] - timag
        }
    }
}
