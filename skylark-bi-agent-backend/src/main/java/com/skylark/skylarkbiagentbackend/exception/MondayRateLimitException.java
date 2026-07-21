package com.skylark.skylarkbiagentbackend.exception;

/**
 * Monday.com rejected a request for exceeding its complexity/rate budget. The client
 * already retries transient 429s internally with backoff; this exception means the
 * budget was still exhausted after those retries were exhausted.
 */
public class MondayRateLimitException extends MondayApiException {

    public MondayRateLimitException(String message) {
        super(ErrorCode.MONDAY_RATE_LIMITED, message);
    }
}
