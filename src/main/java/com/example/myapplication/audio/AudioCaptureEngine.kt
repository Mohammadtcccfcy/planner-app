package com.example.myapplication.audio

import android.Manifest
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AutomaticGainControl
import android.os.Process
import androidx.annotation.RequiresPermission

class AudioCaptureEngine {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SIZE = 160 // 10ms @ 16kHz (IMPORTANT for VAD)
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    private val adaptiveGain = AdaptiveGainProcessor()

    private val minBufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(onAudioFrame: (ShortArray) -> Unit) {

        if (isRecording) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2
        )

        setupEffects(audioRecord!!.audioSessionId)

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {

            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val readBuffer = ShortArray(minBufferSize)
            var frameBuffer = ShortArray(0)

            while (isRecording) {

                val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0

                if (read > 0) {

                    // جمع کردن داده‌ها
                    frameBuffer += readBuffer.copyOf(read)

                    // هر وقت به 160 نمونه رسیدیم → فریم 10ms بساز
                    while (frameBuffer.size >= FRAME_SIZE) {

                        val frame = frameBuffer.copyOfRange(0, FRAME_SIZE)
                        frameBuffer = frameBuffer.copyOfRange(FRAME_SIZE, frameBuffer.size)

                        val processed = adaptiveGain.process(frame)

                        onAudioFrame(processed)
                    }
                }
            }
        }

        recordingThread?.start()
    }

    fun stop() {

        isRecording = false
        recordingThread?.join()

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        releaseEffects()
    }

    private fun setupEffects(sessionId: Int) {

        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(sessionId)
            echoCanceler?.enabled = true
        }

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(sessionId)
            noiseSuppressor?.enabled = true
        }

        if (AutomaticGainControl.isAvailable()) {
            agc = AutomaticGainControl.create(sessionId)
            agc?.enabled = true
        }
    }

    private fun releaseEffects() {
        echoCanceler?.release()
        noiseSuppressor?.release()
        agc?.release()

        echoCanceler = null
        noiseSuppressor = null
        agc = null
    }
}
