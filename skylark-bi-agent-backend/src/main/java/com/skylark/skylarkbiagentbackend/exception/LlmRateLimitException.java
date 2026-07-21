package com.skylark.skylarkbiagentbackend.exception;

/** The LLM provider (Groq) rejected a request for exceeding its rate limit. */
public class LlmRateLimitException extends LlmException {

    public LlmRateLimitException(String message, Throwable cause) {
        super(ErrorCode.LLM_RATE_LIMITED, message, cause);
    }
}
