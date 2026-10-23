package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.UnknownFieldListener
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
    override val unknownFields: UnknownFieldListener? = null,
) : Http,
    AutoCloseable {
    private val client = HttpClient.newHttpClient()

    private val rateLimiter = RateLimiter(REQUESTS_PER_SECOND, Duration.ofSeconds(1))

    @Volatile
    private var isClosed = false

    override fun get(url: String): ByteArray {
        if (isClosed) throw SecException.Http("Client is closed: $url", url)
        val request =
            HttpRequest
                .newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", userAgent)
                .header("Accept-Encoding", "gzip, deflate")
                .build()
        return try {
            send(request)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw SecException.Http("Interrupted: $url", url, e)
        }
    }

    override fun close() {
        isClosed = true
        client.close()
    }

    private fun send(request: HttpRequest): ByteArray {
        var reason = ""
        for (attempt in 1..ATTEMPTS) {
            when (val outcome = attempt(request, backoff(attempt))) {
                is Outcome.Body -> {
                    return outcome.bytes
                }

                is Outcome.Retry -> {
                    reason = outcome.reason
                    if (attempt < ATTEMPTS) Thread.sleep(outcome.after)
                }
            }
        }
        throw SecException.Http("Failed after $ATTEMPTS attempts ($reason): ${request.uri()}", request.uri().toString())
    }

    private fun attempt(
        request: HttpRequest,
        backoff: Duration,
    ): Outcome =
        try {
            rateLimiter.acquire()
            outcome(client.send(request, HttpResponse.BodyHandlers.ofInputStream()), backoff)
        } catch (e: IOException) {
            Outcome.Retry(e.toString(), backoff)
        }

    private fun outcome(
        response: HttpResponse<InputStream>,
        backoff: Duration,
    ): Outcome =
        response.body().use { body ->
            val status = response.statusCode()
            when {
                status == OK -> {
                    Outcome.Body(decode(body, response.headers()).use { it.readBytes() })
                }

                status == TOO_MANY_REQUESTS -> {
                    val header = response.headers().firstValue("Retry-After").orElse(null)
                    val after = cap(maxOf(backoff, retryAfter(header)))
                    rateLimiter.pause(after)
                    Outcome.Retry("HTTP $status", after)
                }

                status >= SERVER_ERROR -> {
                    Outcome.Retry("HTTP $status", backoff)
                }

                else -> {
                    throw SecException.Http("HTTP $status for ${response.uri()}", response.uri().toString())
                }
            }
        }

    private fun backoff(attempt: Int): Duration = cap(baseRetryDelay.multipliedBy(1L shl (attempt - 1)))

    private fun cap(delay: Duration): Duration = minOf(delay, maxRetryDelay)

    private sealed interface Outcome {
        class Body(
            val bytes: ByteArray,
        ) : Outcome

        class Retry(
            val reason: String,
            val after: Duration,
        ) : Outcome
    }

    private companion object {
        const val ATTEMPTS = 3

        // EDGAR's fair access policy.
        const val REQUESTS_PER_SECOND = 10
        const val OK = 200
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500
        val TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}

// Retry-After is either seconds or an HTTP date.
internal fun retryAfter(
    header: String?,
    now: Instant = Instant.now(),
): Duration {
    val value = header?.trim() ?: return Duration.ZERO
    val until =
        value.toLongOrNull()?.let { Duration.ofSeconds(it) }
            ?: try {
                Duration.between(now, ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME))
            } catch (ignored: DateTimeParseException) {
                Duration.ZERO
            }
    return maxOf(until, Duration.ZERO)
}

private fun decode(
    body: InputStream,
    headers: HttpHeaders,
): InputStream =
    when (
        val encoding =
            headers
                .firstValue("Content-Encoding")
                .orElse("identity")
                .trim()
                .lowercase()
    ) {
        "identity" -> body
        "gzip" -> GZIPInputStream(body)
        "deflate" -> inflate(body)
        else -> throw IOException("Unsupported Content-Encoding: $encoding")
    }

// HTTP "deflate" means zlib-wrapped data, but some servers send raw deflate.
private fun inflate(body: InputStream): InputStream {
    val buffered = BufferedInputStream(body)
    buffered.mark(2)
    val cmf = buffered.read()
    val flg = buffered.read()
    buffered.reset()
    val isDeflate = (cmf and ZLIB_METHOD_MASK) == ZLIB_DEFLATE
    val isChecked = ((cmf shl Byte.SIZE_BITS) or flg) % ZLIB_CHECK == 0
    val isZlib = flg != -1 && isDeflate && isChecked
    return InflaterInputStream(buffered, Inflater(!isZlib))
}

private const val ZLIB_METHOD_MASK = 0x0F
private const val ZLIB_DEFLATE = 8
private const val ZLIB_CHECK = 31
