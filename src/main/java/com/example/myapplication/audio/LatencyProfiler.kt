package com.example.myapplication.audio

class LatencyProfiler {

    private var captureTime = 0L
    private var playbackTime = 0L

    fun markCapture() {
        captureTime = System.nanoTime()
    }

    fun markPlayback() {
        playbackTime = System.nanoTime()
    }

    fun getLatencyMs(): Long {
        return (playbackTime - captureTime) / 1_000_000
    }
}
