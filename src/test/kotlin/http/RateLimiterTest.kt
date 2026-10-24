package com.alexkouzel.sec.http

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimiterTest {
    @Test
    fun `starts no threads`() {
        val before = Thread.activeCount()

        val limiters = List(50) { RateLimiter(10, Duration.ofSeconds(1)) }

        assertEquals(50, limiters.size)
        assertEquals(before, Thread.activeCount())
    }

    @Test
    fun `allows at most rate permits per window`() {
        val limiter = RateLimiter(5, Duration.ofSeconds(1))

        val starts =
            List(11) {
                limiter.acquire()
                System.nanoTime()
            }

        assertTrue(
            Duration.ofNanos(starts[4] - starts[0]) < Duration.ofMillis(200),
            "first burst was throttled",
        )
        for (i in 5 until starts.size) {
            val window = Duration.ofNanos(starts[i] - starts[i - 5])
            assertTrue(window >= Duration.ofMillis(990), "6 permits within ${window.toMillis()} ms")
        }
    }

    @Test
    fun `pause delays the next permit`() {
        val limiter = RateLimiter(10, Duration.ofSeconds(1))
        limiter.pause(Duration.ofMillis(300))

        val start = System.nanoTime()
        limiter.acquire()

        assertTrue(Duration.ofNanos(System.nanoTime() - start) >= Duration.ofMillis(290))
    }
}
