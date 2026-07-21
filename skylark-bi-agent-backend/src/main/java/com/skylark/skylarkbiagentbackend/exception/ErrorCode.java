package com.skylark.skylarkbiagentbackend.exception;

/**
 * Stable machine-readable codes returned to the frontend alongside the HTTP status.
 * The frontend should branch on these, never on the human-readable message.
 */
public enum ErrorCode {
    VALIDATION_FAILED,
    MONDAY_AUTHENTICATION_FAILED,
    MONDAY_RATE_LIMITED,
    MONDAY_TIMEOUT,
    MONDAY_API_ERROR,
    LLM_RATE_LIMITED,
    LLM_TIMEOUT,
    LLM_ERROR,
    ENTITY_RESOLUTION_ERROR,
    CONVERSATION_NOT_FOUND,
    INTERNAL_ERROR
}
