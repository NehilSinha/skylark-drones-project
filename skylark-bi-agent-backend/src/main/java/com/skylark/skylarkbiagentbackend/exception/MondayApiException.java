package com.skylark.skylarkbiagentbackend.exception;

/**
 * Thrown for any failure talking to Monday.com that callers should treat as an
 * upstream failure rather than a local bug. Prefer the more specific subtypes
 * ({@link MondayAuthenticationException}, {@link MondayRateLimitException},
 * {@link MondayTimeoutException}) where the failure mode is known.
 */
public class MondayApiException extends BiAgentException {

    public MondayApiException(String message) {
        this(ErrorCode.MONDAY_API_ERROR, message);
    }

    public MondayApiException(String message, Throwable cause) {
        this(ErrorCode.MONDAY_API_ERROR, message, cause);
    }

    protected MondayApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected MondayApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
