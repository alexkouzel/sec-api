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

    private EdgarClient(String userAgent, Consumer<Integer> statusCodeHandler, RateLimiter rateLimiter) {
        super(REQUEST_ATTEMPT_LIMIT, handleStatusCode(statusCodeHandler, rateLimiter), rateLimiter);
        this.userAgent = userAgent;
    }

    public EdgarClient(String userAgent, Consumer<Integer> statusCodeHandler) {
        this(userAgent, statusCodeHandler, buildRateLimiter());
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

    private static Consumer<Integer> handleStatusCode(Consumer<Integer> statusCodeHandler, RateLimiter rateLimiter) {
        return (statusCode) -> {
            if (statusCode == 429) { // TOO_MANY_REQUESTS
                rateLimiter.applyDelaySeq(TimeUnit.MINUTES, 30, 60, 120);
            } else if (statusCode != 200) {
                rateLimiter.applyFixedDelay(TimeUnit.SECONDS, 1);
            }
            statusCodeHandler.accept(statusCode);
        };
    }

    private static RateLimiter buildRateLimiter() {
        return new RateLimiter(TimeUnit.SECONDS, REQUEST_RATE_LIMIT_PER_SECOND);
    }
}
