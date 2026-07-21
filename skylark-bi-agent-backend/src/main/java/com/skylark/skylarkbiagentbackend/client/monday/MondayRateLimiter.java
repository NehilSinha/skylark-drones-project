package com.skylark.skylarkbiagentbackend.client.monday;

import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.exception.MondayRateLimitException;
import org.springframework.stereotype.Component;

/**
 * Self-imposed token bucket, sized under Monday's documented per-minute complexity
 * budget, so this client throttles itself proactively instead of relying solely on
 * reacting to 429s. A short wait is absorbed by blocking the caller briefly; a long
 * wait fails fast rather than tying up a request thread.
 */
@Component
public class MondayRateLimiter {

    private static final long MAX_WAIT_MILLIS = 2_000;

    private final int permitsPerMinute;
    private final Object lock = new Object();
    private double availableTokens;
    private long lastRefillNanos;

    public MondayRateLimiter(MondayProperties properties) {
        this.permitsPerMinute = Math.max(1, properties.rateLimit().permitsPerMinute());
        this.availableTokens = this.permitsPerMinute;
        this.lastRefillNanos = System.nanoTime();
    }

    public void acquire() {
        long waitMillis;
        synchronized (lock) {
            refillLocked();
            if (availableTokens >= 1) {
                availableTokens -= 1;
                return;
            }
            double tokensNeeded = 1 - availableTokens;
            waitMillis = Math.round(tokensNeeded / permitsPerMinute * 60_000);
        }

        if (waitMillis > MAX_WAIT_MILLIS) {
            throw new MondayRateLimitException(
                    "Local rate-limit budget exhausted; next slot in ~" + waitMillis + "ms");
        }

        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MondayRateLimitException("Interrupted while waiting for rate-limit budget");
        }

        synchronized (lock) {
            refillLocked();
            availableTokens = Math.max(0, availableTokens - 1);
        }
    }

    private void refillLocked() {
        long now = System.nanoTime();
        double elapsedMinutes = (now - lastRefillNanos) / 60_000_000_000.0;
        availableTokens = Math.min(permitsPerMinute, availableTokens + elapsedMinutes * permitsPerMinute);
        lastRefillNanos = now;
    }
}
