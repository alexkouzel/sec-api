package com.alexkouzel.client;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Allows at most {@code rate} requests in any window of one {@code timeUnit}.
 * <p>
 * Keeps the start times of the last {@code rate} requests; a new request starts no
 * earlier than one window after the oldest of them. Uses no background threads.
 * {@link #pause} delays every request until the given time has passed (e.g. after
 * the server says "too many requests").
 */
public class RateLimiter {
    private final long windowNanos;
    private final long[] starts;
    private int oldest;
    private long pausedUntil;

    public RateLimiter(TimeUnit timeUnit, int rate) {
        if (rate < 1) {
            throw new IllegalArgumentException("rate must be positive: " + rate);
        }
        this.windowNanos = timeUnit.toNanos(1);
        this.starts = new long[rate];
        long now = System.nanoTime();
        for (int i = 0; i < rate; i++) {
            starts[i] = now - windowNanos;
        }
        this.pausedUntil = now;
    }

    /**
     * Blocks until a request may start.
     */
    public void acquirePermit() throws InterruptedException {
        long start;
        synchronized (this) {
            start = latest(System.nanoTime(), starts[oldest] + windowNanos, pausedUntil);
            starts[oldest] = start;
            oldest = (oldest + 1) % starts.length;
        }
        long wait = start - System.nanoTime();
        if (wait > 0) {
            TimeUnit.NANOSECONDS.sleep(wait);
        }
    }

    /**
     * Delays all requests that haven't started yet by at least {@code duration} from now.
     */
    public synchronized void pause(Duration duration) {
        pausedUntil = latest(pausedUntil, System.nanoTime() + duration.toNanos());
    }

    // nanoTime values may overflow, so compare by difference.
    private static long latest(long first, long... others) {
        long result = first;
        for (long other : others) {
            if (other - result > 0) {
                result = other;
            }
        }
        return result;
    }
}
