package com.skylark.skylarkbiagentbackend.dto.error;

import com.skylark.skylarkbiagentbackend.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * The single shape every error response takes, regardless of what failed. The
 * frontend should branch on {@code errorCode}, never parse {@code message}.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode errorCode,
        String message,
        String traceId,
        List<String> details
) {

    public static ErrorResponse of(int status, ErrorCode errorCode, String message, String traceId) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, traceId, List.of());
    }

    public static ErrorResponse of(int status, ErrorCode errorCode, String message, String traceId, List<String> details) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, traceId, details);
    }
}
