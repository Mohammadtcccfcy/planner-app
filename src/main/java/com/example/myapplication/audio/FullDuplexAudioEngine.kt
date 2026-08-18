package com.example.myapplication.audio

class FullDuplexAudioEngine {

    private val captureEngine = AudioCaptureEngine()
    private val playbackEngine = AudioPlaybackEngine()
    private val vad = VoiceActivityDetector()
    private val jitterBuffer = JitterBuffer()
    private val profiler = LatencyProfiler()

    private var isRunning = false
    private var isAiSpeaking = false

    fun start() {

        if (isRunning) return
        isRunning = true

        playbackEngine.start()

        captureEngine.start { inputFrame ->

            profiler.markCapture()

            // 🎙 VAD
            val userSpeaking = vad.isSpeech(inputFrame)

            if (userSpeaking && isAiSpeaking) {
                // 🧠 Barge-in: stop AI playback
                playbackEngine.stop()
                playbackEngine.start()
                isAiSpeaking = false
            }

            // 🔄 Push to jitter buffer
            jitterBuffer.push(inputFrame)

            // Playback thread simulation
            val frame = jitterBuffer.pop()
            frame?.let {
                playbackEngine.playFrame(it)
                profiler.markPlayback()
                val latency = profiler.getLatencyMs()
                println("E2E Latency = $latency ms")
            }
        }
    }

    fun stop() {
        isRunning = false
        captureEngine.stop()
        playbackEngine.stop()
        jitterBuffer.clear()
    }
}
