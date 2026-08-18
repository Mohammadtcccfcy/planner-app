package com.example.myapplication.audio

import java.util.concurrent.LinkedBlockingQueue

class JitterBuffer {

    private val bufferQueue = LinkedBlockingQueue<ShortArray>(10)

    fun push(frame: ShortArray) {
        if (!bufferQueue.offer(frame)) {
            bufferQueue.poll()
            bufferQueue.offer(frame)
        }
    }

    fun pop(): ShortArray? {
        return bufferQueue.poll()
    }

    fun clear() {
        bufferQueue.clear()
    }
}
