package com.skylark.skylarkbiagentbackend.client.monday;

/**
 * Internal signal used to mark a failure as retryable (5xx, 429, connect/read
 * timeout). Public only so {@code RetryConfig} can reference its class token when
 * building the retry policy — callers of {@link MondayGraphQLClient} never receive
 * this type; it is always translated into a public {@code MondayApiException}
 * subtype once retries are exhausted.
 */
public class TransientMondayFailureException extends RuntimeException {

    private final boolean rateLimit;

    TransientMondayFailureException(String message, boolean rateLimit, Throwable cause) {
        super(message, cause);
        this.rateLimit = rateLimit;
    }

    boolean isRateLimit() {
        return rateLimit;
    }
}
