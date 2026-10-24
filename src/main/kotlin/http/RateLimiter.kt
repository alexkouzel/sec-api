package com.alexkouzel.sec.http

import java.time.Duration

// At most `rate` requests start in any `window`: a request starts no earlier than one window
// after the oldest of the last `rate` starts. No background threads.
internal class RateLimiter(
    rate: Int,
    window: Duration,
) {
    private val windowNanos = window.toNanos()
    private val starts: LongArray
    private var oldest = 0
    private var pausedUntil: Long

    init {
        require(rate > 0) { "rate must be positive: $rate" }
        val now = System.nanoTime()
        starts = LongArray(rate) { now - windowNanos }
        pausedUntil = now
    }

    fun acquire() {
        val start: Long
        synchronized(this) {
            val windowFree = starts[oldest] + windowNanos
            start = latest(System.nanoTime(), latest(windowFree, pausedUntil))
            starts[oldest] = start
            oldest = (oldest + 1) % starts.size
        }
        val wait = start - System.nanoTime()
        if (wait > 0) {
            Thread.sleep(Duration.ofNanos(wait))
        }
    }

    // Delays every request that hasn't started yet until `duration` from now.
    @Synchronized
    fun pause(duration: Duration) {
        pausedUntil = latest(pausedUntil, System.nanoTime() + duration.toNanos())
    }

    // nanoTime values may overflow, so they are compared by difference.
    private fun latest(
        a: Long,
        b: Long,
    ): Long = if (b - a > 0) b else a
}
