package com.skylark.skylarkbiagentbackend.exception;

/** The configured Monday.com API token was rejected. Never retried automatically. */
public class MondayAuthenticationException extends MondayApiException {

    public MondayAuthenticationException(String message) {
        super(ErrorCode.MONDAY_AUTHENTICATION_FAILED, message);
    }
}
