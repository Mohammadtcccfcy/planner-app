package com.example.myapplication.audio

import android.media.*
import android.os.Process

class AudioPlaybackEngine {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    ) * 2

    fun start() {

        if (isPlaying) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(audioFormat)
            .setChannelMask(channelConfig)
            .build()

        audioTrack = AudioTrack(
            attributes,
            format,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.play()
        isPlaying = true
    }

    fun playFrame(pcmData: ShortArray) {
        if (!isPlaying) return
        audioTrack?.write(pcmData, 0, pcmData.size)
    }

    fun stop() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
