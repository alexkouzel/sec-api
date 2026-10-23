package com.alexkouzel.client;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EdgarClient extends HttpDataClient {
    private static final int REQUEST_ATTEMPT_LIMIT = 3;
    private static final int REQUEST_TIMEOUT_IN_SECONDS = 5;
    private static final int REQUEST_RATE_LIMIT_PER_SECOND = 10;

    private final String userAgent;

    /**
     * @param userAgent         identifies you to EDGAR, e.g. "Sample Company admin@example.com"
     * @param statusCodeHandler called with the status code of every response
     * @param baseRetryDelay    wait before the first retry; doubles on each further retry
     * @param maxRetryDelay     longest wait between attempts, including for HTTP 429
     */
    public EdgarClient(String userAgent, Consumer<Integer> statusCodeHandler,
                       Duration baseRetryDelay, Duration maxRetryDelay) {
        super(REQUEST_ATTEMPT_LIMIT, baseRetryDelay, maxRetryDelay, statusCodeHandler, buildRateLimiter());
        this.userAgent = userAgent;
    }

    public EdgarClient(String userAgent, Consumer<Integer> statusCodeHandler) {
        this(userAgent, statusCodeHandler, DEFAULT_BASE_RETRY_DELAY, DEFAULT_MAX_RETRY_DELAY);
    }

    public EdgarClient(String userAgent) {
        this(userAgent, (statusCode) -> {
        });
    }

    @Override
    protected HttpRequest buildRequest(String url, String contentType) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.of(REQUEST_TIMEOUT_IN_SECONDS, ChronoUnit.SECONDS))
                .header("User-Agent", userAgent)
                .header("Content-Type", contentType + ";charset=UTF-8")
                .header("Accept-Encoding", "gzip, deflate")
                .build();
    }

    private static RateLimiter buildRateLimiter() {
        return new RateLimiter(TimeUnit.SECONDS, REQUEST_RATE_LIMIT_PER_SECOND);
    }
}
