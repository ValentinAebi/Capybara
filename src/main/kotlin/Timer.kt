package com.github.valentinaebi.capybara

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class Timer {
    private var startTime: Long? = null

    fun reset() {
        startTime = System.nanoTime()
    }

    fun elapsedTime(): Duration {
        val now = System.nanoTime()
        val durationNanos = now - startTime!!
        return durationNanos.nanoseconds
    }

}
