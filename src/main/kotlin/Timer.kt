package com.github.valentinaebi.capybara

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class Timer {
    private var startTime: Long? = null

    fun start() {
        startTime = System.nanoTime()
    }

    fun stop(): Duration {
        val now = System.nanoTime()
        val durationNanos = now - startTime!!
        return durationNanos.nanoseconds
    }

}
