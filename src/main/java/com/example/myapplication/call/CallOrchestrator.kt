package com.example.myapplication.call

import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.myapplication.audio.AudioSessionController
import com.example.myapplication.audio.FullDuplexAudioEngine
import com.example.myapplication.service.CallForegroundService

object CallOrchestrator {

    private var audioController: AudioSessionController? = null
    private var fullDuplexEngine: FullDuplexAudioEngine? = null

    fun onIncomingCall(context: Context) {

        val serviceIntent = Intent(context, CallForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)

        audioController = AudioSessionController(context)
        audioController?.prepareForCall()

        acceptCall(context)
    }

    fun onCallActive() {

        // 🔹 Samsung defensive fix first
        audioController?.reapplyAfterOffhook()

        // 🔹 Start Full Duplex after routing is stable
        fullDuplexEngine = FullDuplexAudioEngine()
        fullDuplexEngine?.start()
    }

    fun onCallEnded() {

        fullDuplexEngine?.stop()
        fullDuplexEngine = null

        audioController?.teardown()
        audioController = null
    }

    fun verifyHealth() {
        audioController?.verifyAndRepair()
    }

    @android.annotation.SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun acceptCall(context: Context) {
        val telecomManager =
            context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        try {
            telecomManager.acceptRingingCall()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
