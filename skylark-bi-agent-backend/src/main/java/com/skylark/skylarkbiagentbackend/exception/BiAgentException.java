package com.skylark.skylarkbiagentbackend.exception;

/**
 * Base of every exception this application throws deliberately. Anything that
 * reaches {@link GlobalExceptionHandler} without extending this type is treated
 * as an unexpected bug, not a handled failure mode.
 */
public abstract class BiAgentException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BiAgentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BiAgentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
