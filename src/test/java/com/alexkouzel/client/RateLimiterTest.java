package com.alexkouzel.client;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimiterTest {

    @Test
    public void startsNoThreads() {
        int before = Thread.activeCount();

        List<RateLimiter> limiters = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            limiters.add(new RateLimiter(TimeUnit.SECONDS, 10));
        }

        assertEquals(50, limiters.size());
        assertEquals(before, Thread.activeCount());
    }

    @Test
    public void allowsAtMostRatePerWindow() throws Exception {
        RateLimiter limiter = new RateLimiter(TimeUnit.SECONDS, 5);
        List<Long> starts = new ArrayList<>();

        for (int i = 0; i < 11; i++) {
            limiter.acquirePermit();
            starts.add(System.nanoTime());
        }

        assertTrue(starts.get(4) - starts.get(0) < TimeUnit.MILLISECONDS.toNanos(200), "first burst was throttled");
        for (int i = 5; i < starts.size(); i++) {
            long window = starts.get(i) - starts.get(i - 5);
            assertTrue(window >= TimeUnit.MILLISECONDS.toNanos(990), "6 permits within " + window / 1_000_000 + " ms");
        }
    }

    @Test
    public void pauseDelaysNextPermit() throws Exception {
        RateLimiter limiter = new RateLimiter(TimeUnit.SECONDS, 10);
        limiter.pause(Duration.ofMillis(300));

        long start = System.nanoTime();
        limiter.acquirePermit();

        assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(290));
    }
}
