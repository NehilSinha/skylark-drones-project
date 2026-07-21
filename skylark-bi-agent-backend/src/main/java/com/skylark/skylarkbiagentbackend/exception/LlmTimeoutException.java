package com.skylark.skylarkbiagentbackend.exception;

/** The LLM provider did not respond within the configured timeout. */
public class LlmTimeoutException extends LlmException {

    public LlmTimeoutException(String message, Throwable cause) {
        super(ErrorCode.LLM_TIMEOUT, message, cause);
    }
}
