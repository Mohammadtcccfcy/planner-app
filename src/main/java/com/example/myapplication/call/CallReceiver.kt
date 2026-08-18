package com.example.myapplication.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            CallOrchestrator.onIncomingCall(context)
        }
    }
}
