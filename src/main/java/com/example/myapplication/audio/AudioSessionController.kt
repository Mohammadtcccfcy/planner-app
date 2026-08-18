package com.example.myapplication.audio

import android.content.Context
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager

class AudioSessionController(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var focusRequest: AudioFocusRequest? = null

    private val handler = Handler(Looper.getMainLooper())

    /* ============================= */
    /* 1️⃣ INITIAL AUDIO PREP       */
    /* ============================= */

    fun prepareForCall() {
        requestAudioFocus()
        applyRouting()
    }

    /* ============================= */
    /* 2️⃣ SAMSUNG 200ms REAPPLY    */
    /* ============================= */

    fun reapplyAfterOffhook() {
        handler.postDelayed({
            applyRouting()
        }, 200) // Samsung fix
    }

    /* ============================= */
    /* 3️⃣ APPLY ROUTING LOGIC      */
    /* ============================= */

    private fun applyRouting() {

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val devices = audioManager.availableCommunicationDevices

            val bluetooth = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }

            val wired = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            }

            val earpiece = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }

            when {
                bluetooth != null -> {
                    audioManager.setCommunicationDevice(bluetooth)
                }

                wired != null -> {
                    audioManager.setCommunicationDevice(wired)
                }

                earpiece != null -> {
                    audioManager.setCommunicationDevice(earpiece)
                }
            }
        }
    }

    /* ============================= */
    /* 4️⃣ AUDIO FOCUS MANAGEMENT   */
    /* ============================= */

    private fun requestAudioFocus() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            focusRequest = AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { change ->
                    if (change <= 0) {
                        // focus lost → re-request
                        requestAudioFocus()
                    }
                }
                .build()

            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    /* ============================= */
    /* 5️⃣ SELF-HEALING CHECK       */
    /* ============================= */

    fun verifyAndRepair() {
        if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
            applyRouting()
        }
    }

    /* ============================= */
    /* 6️⃣ CLEANUP                  */
    /* ============================= */

    fun teardown() {
        abandonFocus()
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
