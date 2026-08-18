package com.example.myapplication.call

import android.content.Context
import android.telephony.TelephonyManager

object CallStateMachine {

    enum class State {
        IDLE,
        RINGING,
        ACTIVE
    }

    private var currentState = State.IDLE
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun onCallStateChanged(state: Int) {

        when (state) {

            TelephonyManager.CALL_STATE_IDLE -> {
                currentState = State.IDLE
                CallOrchestrator.onCallEnded()
            }

            TelephonyManager.CALL_STATE_RINGING -> {
                if (currentState == State.IDLE) {
                    currentState = State.RINGING
                    appContext?.let {
                        CallOrchestrator.onIncomingCall(it)
                    }
                }
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                currentState = State.ACTIVE
                CallOrchestrator.onCallActive()
            }
        }
    }

    fun isCallActive(): Boolean {
        return currentState == State.ACTIVE
    }
}
