package com.alexkouzel.client;

import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * HTTP client with rate limiting and retries.
 * <p>
 * A request is retried after an I/O error, HTTP 429 or HTTP 5xx, waiting
 * {@code baseRetryDelay}, then twice that, and so on, up to {@code maxRetryDelay}.
 * On 429 it waits at least as long as the server's {@code Retry-After} (also capped),
 * and pauses the rate limiter so concurrent requests back off too. Other statuses
 * fail immediately.
 */
public abstract class HttpDataClient implements AutoCloseable {
    public static final Duration DEFAULT_BASE_RETRY_DELAY = Duration.ofSeconds(1);
    public static final Duration DEFAULT_MAX_RETRY_DELAY = Duration.ofMinutes(1);

    private static final int TOO_MANY_REQUESTS = 429;

    private final int attemptLimit;
    private final Duration baseRetryDelay;
    private final Duration maxRetryDelay;
    private final Consumer<Integer> statusCodeHandler;
    protected final RateLimiter rateLimiter;
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;
    private final HttpClient client;

    public HttpDataClient(
            int attemptLimit,
            Duration baseRetryDelay,
            Duration maxRetryDelay,
            Consumer<Integer> statusCodeHandler,
            RateLimiter rateLimiter
    ) {
        if (attemptLimit < 1) {
            throw new IllegalArgumentException("attemptLimit must be positive: " + attemptLimit);
        }
        this.attemptLimit = attemptLimit;
        this.baseRetryDelay = baseRetryDelay;
        this.maxRetryDelay = maxRetryDelay;
        this.statusCodeHandler = statusCodeHandler;
        this.rateLimiter = rateLimiter;
        this.jsonMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        this.client = HttpClient.newHttpClient();

        JavaTimeModule timeModule = new JavaTimeModule();
        jsonMapper.registerModule(timeModule);
        xmlMapper.registerModule(timeModule);

        // EDGAR adds fields over time; unknown ones must not break parsing.
        jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public HttpDataClient(int attemptLimit, Consumer<Integer> statusCodeHandler, RateLimiter rateLimiter) {
        this(attemptLimit, DEFAULT_BASE_RETRY_DELAY, DEFAULT_MAX_RETRY_DELAY, statusCodeHandler, rateLimiter);
    }

    protected abstract HttpRequest buildRequest(String url, String contentType);

    public <T> T loadJson(String url, Class<T> type) throws HttpRequestException, ParsingException {
        return loadType(url, type, jsonMapper);
    }

    public <T> T loadXml(String url, Class<T> type) throws HttpRequestException, ParsingException {
        return loadType(url, type, xmlMapper);
    }

    public <T> T loadType(String url, Class<T> type, ObjectMapper mapper) throws HttpRequestException, ParsingException {
        String text = loadText(url);
        try {
            return mapper.readValue(text, type);
        } catch (JsonProcessingException e) {
            throw new ParsingException("Failed mapping " + type.getSimpleName() + " from " + url + ": " + e.getMessage());
        }
    }

    public String loadText(String url) throws HttpRequestException {
        try (InputStream stream = loadStream(url, "text/html")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new HttpRequestException("Failed reading " + url + ": " + e);
        }
    }

    public InputStream loadStream(String url, String contentType) throws HttpRequestException {
        HttpRequest request = buildRequest(url, contentType);
        String lastError = null;

        for (int attempt = 1; attempt <= attemptLimit; attempt++) {
            Duration retryDelay = backoff(attempt);
            try {
                if (rateLimiter != null) {
                    rateLimiter.acquirePermit();
                }
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int statusCode = response.statusCode();
                statusCodeHandler.accept(statusCode);

                if (statusCode == 200) {
                    return decode(response);
                }
                response.body().close();
                lastError = "HTTP " + statusCode;

                if (statusCode == TOO_MANY_REQUESTS) {
                    retryDelay = cap(longest(retryDelay, retryAfter(response)));
                    if (rateLimiter != null) {
                        rateLimiter.pause(retryDelay);
                    }
                } else if (statusCode < 500) {
                    throw new HttpRequestException(lastError + " for " + url);
                }
            } catch (IOException e) {
                lastError = e.toString();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HttpRequestException("Request interrupted: " + url);
            }

            if (attempt < attemptLimit) {
                sleep(retryDelay, url);
            }
        }
        throw new HttpRequestException("Failed after " + attemptLimit + " attempts (" + lastError + "): " + url);
    }

    @Override
    public void close() {
        client.close();
    }

    private Duration backoff(int attempt) {
        return cap(baseRetryDelay.multipliedBy(1L << Math.min(attempt - 1, 20)));
    }

    private Duration cap(Duration delay) {
        return delay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : delay;
    }

    private static Duration longest(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    // Retry-After is either seconds or an HTTP date.
    private static Duration retryAfter(HttpResponse<?> response) {
        Optional<String> header = response.headers().firstValue("Retry-After");
        if (header.isEmpty()) {
            return Duration.ZERO;
        }
        String value = header.get().trim();
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value)));
        } catch (NumberFormatException e) {
            try {
                Duration until = Duration.between(ZonedDateTime.now(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME));
                return until.isNegative() ? Duration.ZERO : until;
            } catch (DateTimeParseException ignored) {
                return Duration.ZERO;
            }
        }
    }

    private static void sleep(Duration delay, String url) throws HttpRequestException {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRequestException("Request interrupted: " + url);
        }
    }

    private static InputStream decode(HttpResponse<InputStream> response) throws IOException {
        Optional<String> encoding = response.headers().firstValue("Content-Encoding");
        InputStream stream = response.body();

        if (encoding.isEmpty()) {
            return stream;
        }
        return switch (encoding.get().trim().toLowerCase()) {
            case "gzip" -> new GZIPInputStream(stream);
            case "deflate" -> inflate(stream);
            case "identity" -> stream;
            default -> {
                stream.close();
                throw new IOException("Unsupported Content-Encoding: " + encoding.get());
            }
        };
    }

    // HTTP "deflate" means zlib-wrapped data, but some servers send raw deflate.
    private static InputStream inflate(InputStream stream) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(stream);
        buffered.mark(2);
        int cmf = buffered.read();
        int flg = buffered.read();
        buffered.reset();
        boolean zlib = cmf != -1 && flg != -1 && (cmf & 0x0F) == 8 && ((cmf << 8) | flg) % 31 == 0;
        return new InflaterInputStream(buffered, new Inflater(!zlib));
    }
}
