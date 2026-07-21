package com.skylark.skylarkbiagentbackend.exception;

import java.util.List;

/**
 * Application-level validation failure that isn't naturally expressed as a Bean
 * Validation constraint (e.g. a business-rule check spanning multiple fields, or a
 * date range resolved from natural language that turns out to be invalid).
 */
public class ValidationException extends BiAgentException {

    private final List<String> details;

    public ValidationException(String message) {
        this(message, List.of());
    }

    public ValidationException(String message, List<String> details) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.details = details;
    }

    public List<String> details() {
        return details;
    }
}
