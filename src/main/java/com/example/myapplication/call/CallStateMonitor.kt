package com.example.myapplication.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallStateMonitor : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        CallStateMachine.init(context)

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        val callState = when (state) {
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            else -> return
        }

        CallStateMachine.onCallStateChanged(callState)
    }
}
