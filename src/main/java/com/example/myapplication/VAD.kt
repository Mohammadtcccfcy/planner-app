package com.example.myapplication

class VAD {

    companion object {
        init {
            System.loadLibrary("native-audio")
        }
    }

    external fun init(sampleRate: Int, mode: Int): Long
    external fun process(handle: Long, audioFrame: ShortArray): Int
    external fun release(handle: Long)
}
