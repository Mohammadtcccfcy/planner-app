package com.example.myapplication.audio

import kotlin.math.*

class OptimizedRealFFT(private val n: Int) {

    private val real = DoubleArray(n)
    private val imag = DoubleArray(n)

    fun compute(input: ShortArray): DoubleArray {

        for (i in 0 until n) {
            real[i] = input[i].toDouble()
            imag[i] = 0.0
        }

        bitReverse()
        fft()

        val magnitude = DoubleArray(n / 2)
        for (i in magnitude.indices) {
            magnitude[i] = sqrt(real[i]*real[i] + imag[i]*imag[i])
        }

        return magnitude
    }

    private fun bitReverse() {
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                real[i] = real[j].also { real[j] = real[i] }
                imag[i] = imag[j].also { imag[j] = imag[i] }
            }
            var m = n shr 1
            while (j >= m && m >= 2) {
                j -= m
                m = m shr 1
            }
            j += m
        }
    }

    private fun fft() {
        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val wlenCos = cos(angle)
            val wlenSin = sin(angle)

            for (i in 0 until n step len) {
                var wr = 1.0
                var wi = 0.0

                for (j in 0 until len/2) {

                    val uReal = real[i + j]
                    val uImag = imag[i + j]

                    val vReal = real[i + j + len/2] * wr -
                            imag[i + j + len/2] * wi
                    val vImag = real[i + j + len/2] * wi +
                            imag[i + j + len/2] * wr

                    real[i + j] = uReal + vReal
                    imag[i + j] = uImag + vImag
                    real[i + j + len/2] = uReal - vReal
                    imag[i + j + len/2] = uImag - vImag

                    val nextWr = wr * wlenCos - wi * wlenSin
                    wi = wr * wlenSin + wi * wlenCos
                    wr = nextWr
                }
            }

            len *= 2
        }
    }
}
