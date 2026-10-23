package com.alexkouzel.sec.http

import com.alexkouzel.sec.LocalServer
import com.alexkouzel.sec.LocalServer.Request
import com.alexkouzel.sec.LocalServer.Response
import com.alexkouzel.sec.SecException
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EdgarHttpTest {
    private val userAgent = "Test Company test@example.com"
    private val baseRetryDelay = Duration.ofMillis(100)
    private val server = LocalServer()

    // A short base delay keeps retries fast; the cap still allows Retry-After: 2.
    private val http = EdgarHttp(userAgent, baseRetryDelay, maxRetryDelay = Duration.ofSeconds(5))

    @AfterTest
    fun close() {
        http.close()
        server.close()
    }

    private fun get(path: String = "/a") = http.get(server.url(path)).decodeToString()

    private fun gap(
        first: Request,
        second: Request,
    ) = Duration.ofNanos(second.nanoTime - first.nanoTime)

    @Test
    fun `identifies itself and accepts compression`() {
        server.enqueue(Response.ok("hello"))

        get()

        val headers = server.requests.first().headers
        assertEquals(userAgent, headers.getFirst("User-Agent"))
        assertEquals("gzip, deflate", headers.getFirst("Accept-Encoding"))
    }

    @Test
    fun `returns a plain body`() {
        server.enqueue(Response.ok("plain body"))

        assertEquals("plain body", get())
    }

    @Test
    fun `decodes a gzip body`() {
        server.enqueue(Response.gzip("zipped body"))

        assertEquals("zipped body", get())
    }

    @Test
    fun `decodes a deflate body`() {
        server.enqueue(Response.deflate("deflated body"))

        assertEquals("deflated body", get())
    }

    @Test
    fun `decodes a raw deflate body`() {
        server.enqueue(Response.rawDeflate("raw deflated body"))

        assertEquals("raw deflated body", get())
    }

    @Test
    fun `retries a server error then succeeds`() {
        server.enqueue(Response.status(500, "oops"), Response.ok("done"))

        assertEquals("done", get())
        assertEquals(2, server.requests.size)
    }

    @Test
    fun `gives up after three attempts`() {
        server.enqueue(Response.status(503, "down"))

        assertFailsWith<SecException.Http> { get() }
        assertEquals(3, server.requests.size)
    }

    @Test
    fun `does not retry a client error`() {
        server.enqueue(Response.status(404, "missing"))

        val error = assertFailsWith<SecException.Http> { get() }

        assertContains(error.message.orEmpty(), "404")
        assertEquals(1, server.requests.size)
    }

    @Test
    fun `retries a dropped connection`() {
        server.enqueue(Response.dropped(), Response.ok("done"))

        assertEquals("done", get())
    }

    @Test
    fun `backs off before retrying a dropped connection`() {
        server.enqueue(Response.dropped(), Response.dropped(), Response.ok("done"))

        assertEquals("done", get())

        // The JDK client silently retries a dropped connection once by itself, so the two
        // drops are one attempt; the retry comes after the base delay.
        val requests = server.requests
        assertEquals(3, requests.size)
        val gap = gap(requests[1], requests[2])
        assertTrue(gap >= baseRetryDelay.minusMillis(10), "retried after ${gap.toMillis()} ms")
    }

    @Test
    fun `stays under ten requests per second`() {
        server.enqueue(Response.ok("x"))

        repeat(25) { get("/$it") }

        val requests = server.requests
        assertEquals(25, requests.size)
        for (i in 10 until requests.size) {
            val window = gap(requests[i - 10], requests[i])
            assertTrue(
                window >= Duration.ofMillis(900),
                "11 requests within ${window.toMillis()} ms",
            )
        }
    }

    @Test
    @Timeout(20)
    fun `recovers from too many requests without hanging`() {
        server.enqueue(Response.status(429, "slow down"), Response.ok("done"))

        assertEquals("done", get())
    }

    @Test
    fun `waits for Retry-After on too many requests`() {
        server.enqueue(
            Response.status(429, "slow down").withHeader("Retry-After", "2"),
            Response.ok("done"),
        )

        get()

        val gap = gap(server.requests[0], server.requests[1])
        assertTrue(gap >= Duration.ofMillis(1900), "retried after ${gap.toMillis()} ms")
    }

    @Test
    @Timeout(20)
    fun `caps Retry-After`() {
        EdgarHttp(userAgent, baseRetryDelay, maxRetryDelay = Duration.ofMillis(500)).use { capped ->
            server.enqueue(
                Response.status(429, "slow down").withHeader("Retry-After", "3600"),
                Response.ok("done"),
            )

            assertEquals("done", capped.get(server.url("/a")).decodeToString())

            val gap = gap(server.requests[0], server.requests[1])
            assertTrue(gap >= Duration.ofMillis(450), "retried after ${gap.toMillis()} ms")
            assertTrue(gap < Duration.ofSeconds(5), "retried after ${gap.toMillis()} ms")
        }
    }

    @Test
    fun `fails requests after close`() {
        http.close()

        assertFailsWith<SecException.Http> { get() }
        assertEquals(0, server.requests.size)
    }

    @Test
    fun `reads Retry-After as seconds or as an HTTP date`() {
        val now = Instant.parse("2026-10-23T15:00:00Z")

        assertEquals(Duration.ofSeconds(2), retryAfter("2", now))
        assertEquals(Duration.ofSeconds(30), retryAfter("Fri, 23 Oct 2026 15:00:30 GMT", now))
        assertEquals(Duration.ZERO, retryAfter("Fri, 23 Oct 2026 14:00:00 GMT", now))
        assertEquals(Duration.ZERO, retryAfter("soon", now))
        assertEquals(Duration.ZERO, retryAfter(null, now))
    }
}
