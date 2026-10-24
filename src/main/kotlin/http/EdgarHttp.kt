package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

// Retries I/O errors, 429 and 5xx, waiting baseRetryDelay, then twice that, and so on, up to
// maxRetryDelay. A 429 waits at least its Retry-After (also capped) and pauses the rate limiter,
// so concurrent requests back off too. Other statuses fail at once.
internal class EdgarHttp(
    private val userAgent: String,
    private val baseRetryDelay: Duration = Duration.ofSeconds(1),
    private val maxRetryDelay: Duration = Duration.ofMinutes(1),
) : Http,
    AutoCloseable {
    private val client = HttpClient.newHttpClient()
    private val rateLimiter = RateLimiter(REQUESTS_PER_SECOND, Duration.ofSeconds(1))

    @Volatile
    private var isClosed = false

    override fun get(url: String): ByteArray {
        if (isClosed) {
            throw SecException.Http("Client is closed: $url")
        }
        val request =
            HttpRequest
                .newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", userAgent)
                .header("Accept-Encoding", "gzip, deflate")
                .build()
        try {
            return send(request)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw SecException.Http("Interrupted: $url", e)
        }
    }

    override fun close() {
        isClosed = true
        client.close()
    }

    private fun send(request: HttpRequest): ByteArray {
        var failure = ""
        for (attempt in 1..ATTEMPTS) {
            var wait = cap(baseRetryDelay.multipliedBy(1L shl (attempt - 1)))
            rateLimiter.acquire()
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
                val status = response.statusCode()
                response.body().use { body ->
                    if (status == OK) {
                        return read(body, response.headers())
                    }
                }
                if (status == TOO_MANY_REQUESTS) {
                    val header = response.headers().firstValue("Retry-After").orElse(null)
                    wait = cap(maxOf(wait, retryAfter(header)))
                    rateLimiter.pause(wait)
                } else if (status < SERVER_ERROR) {
                    throw SecException.Http("HTTP $status for ${request.uri()}")
                }
                failure = "HTTP $status"
            } catch (e: IOException) {
                failure = e.toString()
            }
            if (attempt < ATTEMPTS) {
                Thread.sleep(wait)
            }
        }
        throw SecException.Http("Failed after $ATTEMPTS attempts ($failure): ${request.uri()}")
    }

    private fun cap(delay: Duration): Duration = minOf(delay, maxRetryDelay)

    private companion object {
        const val ATTEMPTS = 3

        // EDGAR's fair access policy.
        const val REQUESTS_PER_SECOND = 10

        const val OK = 200
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500

        // EDGAR's cgi-bin pages can take 20 seconds when it is busy.
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}

// Retry-After is either seconds or an HTTP date.
internal fun retryAfter(
    header: String?,
    now: Instant = Instant.now(),
): Duration {
    val value = header?.trim().orEmpty()
    val seconds = value.toLongOrNull()
    val delay = if (seconds != null) Duration.ofSeconds(seconds) else untilDate(value, now)
    return maxOf(delay, Duration.ZERO)
}

private fun untilDate(
    value: String,
    now: Instant,
): Duration {
    try {
        val date = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
        return Duration.between(now, date)
    } catch (ignored: DateTimeParseException) {
        return Duration.ZERO
    }
}

private fun read(
    body: InputStream,
    headers: HttpHeaders,
): ByteArray {
    val encoding =
        headers
            .firstValue("Content-Encoding")
            .orElse("identity")
            .trim()
            .lowercase()
    val decoded =
        when (encoding) {
            "identity" -> body
            "gzip" -> GZIPInputStream(body)
            "deflate" -> inflate(body)
            else -> throw IOException("Unsupported Content-Encoding: $encoding")
        }
    return decoded.use { it.readBytes() }
}

// HTTP "deflate" means zlib-wrapped data, but some servers send raw deflate. A zlib stream
// starts with two bytes: the compression method (8) in the low bits, and a check (multiple of 31).
private fun inflate(body: InputStream): InputStream {
    val buffered = BufferedInputStream(body)
    buffered.mark(2)
    val first = buffered.read()
    val second = buffered.read()
    buffered.reset()

    val isDeflateMethod = (first and ZLIB_METHOD_MASK) == ZLIB_DEFLATE
    val hasValidCheck = ((first shl Byte.SIZE_BITS) or second) % ZLIB_CHECK == 0
    val isZlib = second != -1 && isDeflateMethod && hasValidCheck
    return InflaterInputStream(buffered, Inflater(!isZlib))
}

private const val ZLIB_METHOD_MASK = 0x0F
private const val ZLIB_DEFLATE = 8
private const val ZLIB_CHECK = 31
