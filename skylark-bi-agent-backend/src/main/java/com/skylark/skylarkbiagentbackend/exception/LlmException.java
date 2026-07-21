package com.skylark.skylarkbiagentbackend.exception;

/** Thrown for any failure calling the LLM provider that isn't a more specific subtype. */
public class LlmException extends BiAgentException {

    public LlmException(String message, Throwable cause) {
        this(ErrorCode.LLM_ERROR, message, cause);
    }

    protected LlmException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
