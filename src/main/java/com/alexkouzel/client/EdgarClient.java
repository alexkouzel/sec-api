package com.alexkouzel.client;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EdgarClient extends DataClient {

    private final String userAgent;

    private static final int DEFAULT_ATTEMPT_LIMIT = 3;

    private EdgarClient(String userAgent, int attemptLimit, Consumer<Integer> errorHandler, RateLimiter rateLimiter) {
        super(attemptLimit, chainErrorHandler(errorHandler, rateLimiter), rateLimiter);
        this.userAgent = userAgent;
    }

    public EdgarClient(String userAgent, int attemptLimit, Consumer<Integer> errorHandler) {
        this(userAgent, attemptLimit, errorHandler, new RateLimiter(TimeUnit.SECONDS, 10));
    }

    public EdgarClient(String userAgent) {
        this(userAgent, DEFAULT_ATTEMPT_LIMIT, (statusCode) -> {});
    }

    @Override
    protected HttpRequest buildRequest(String url, String contentType) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .header("User-Agent", userAgent)
                .header("Content-Type", contentType + ";charset=UTF-8")
                .header("Accept-Encoding", "gzip, deflate")
                .build();
    }

    private static Consumer<Integer> chainErrorHandler(Consumer<Integer> errorHandler, RateLimiter rateLimiter) {
        return (statusCode) -> {
            if (statusCode == 429) {
                rateLimiter.applyDelaySeq(TimeUnit.MINUTES, 30, 60, 120);
            } else {
                rateLimiter.applyFixedDelay(TimeUnit.SECONDS, 1);
            }
            errorHandler.accept(statusCode);
        };
    }

}
