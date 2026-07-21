package com.skylark.skylarkbiagentbackend.exception;

/** Monday.com did not respond within the configured connect/read timeout. */
public class MondayTimeoutException extends MondayApiException {

    public MondayTimeoutException(String message, Throwable cause) {
        super(ErrorCode.MONDAY_TIMEOUT, message, cause);
    }
}
