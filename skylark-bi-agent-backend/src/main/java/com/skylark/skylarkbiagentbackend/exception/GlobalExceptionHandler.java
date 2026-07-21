package com.skylark.skylarkbiagentbackend.exception;

import com.skylark.skylarkbiagentbackend.dto.error.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.UUID;

/**
 * The single place every exception this application can throw is translated into a
 * consistent {@link ErrorResponse}. The frontend never has to parse a raw stack
 * trace or guess at a status code — see ADR-001 "Error Handling Strategy".
 *
 * <p>Deliberately extends {@link ResponseEntityExceptionHandler} so Spring MVC's own
 * exceptions (malformed JSON, bean validation, unsupported media type, etc.) funnel
 * through the same {@code ErrorResponse} shape instead of Spring's default problem
 * detail format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        String traceId = newTraceId();
        log.warn("[{}] validation failed: {}", traceId, ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.errorCode(), ex.getMessage(), traceId, ex.details());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String traceId = newTraceId();
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        log.warn("[{}] constraint violation: {}", traceId, details);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Request validation failed", traceId, details);
    }

    @ExceptionHandler(MondayAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleMondayAuth(MondayAuthenticationException ex) {
        String traceId = newTraceId();
        log.error("[{}] Monday.com authentication failed: {}", traceId, ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, ex.errorCode(), "Unable to authenticate with Monday.com. Check the configured API token.", traceId, List.of());
    }

    @ExceptionHandler(MondayRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleMondayRateLimit(MondayRateLimitException ex) {
        String traceId = newTraceId();
        log.warn("[{}] Monday.com rate limit exhausted: {}", traceId, ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, ex.errorCode(), "Monday.com rate limit reached. Please try again shortly.", traceId, List.of());
    }

    @ExceptionHandler(MondayTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleMondayTimeout(MondayTimeoutException ex) {
        String traceId = newTraceId();
        log.error("[{}] Monday.com request timed out: {}", traceId, ex.getMessage());
        return build(HttpStatus.GATEWAY_TIMEOUT, ex.errorCode(), "Monday.com did not respond in time. Please try again.", traceId, List.of());
    }

    @ExceptionHandler(MondayApiException.class)
    public ResponseEntity<ErrorResponse> handleMondayApi(MondayApiException ex) {
        String traceId = newTraceId();
        log.error("[{}] Monday.com API error", traceId, ex);
        return build(HttpStatus.BAD_GATEWAY, ex.errorCode(), "Monday.com request failed: " + ex.getMessage(), traceId, List.of());
    }

    @ExceptionHandler(LlmRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleLlmRateLimit(LlmRateLimitException ex) {
        String traceId = newTraceId();
        log.warn("[{}] LLM provider rate limited: {}", traceId, ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.errorCode(), "The AI provider is rate-limited. Please try again shortly.", traceId, List.of());
    }

    @ExceptionHandler(LlmTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleLlmTimeout(LlmTimeoutException ex) {
        String traceId = newTraceId();
        log.error("[{}] LLM provider timed out: {}", traceId, ex.getMessage());
        return build(HttpStatus.GATEWAY_TIMEOUT, ex.errorCode(), "The AI provider did not respond in time. Please try again.", traceId, List.of());
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ErrorResponse> handleLlm(LlmException ex) {
        String traceId = newTraceId();
        log.error("[{}] LLM provider error", traceId, ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.errorCode(), "The AI provider request failed. Please try again.", traceId, List.of());
    }

    @ExceptionHandler(EntityResolutionException.class)
    public ResponseEntity<ErrorResponse> handleEntityResolution(EntityResolutionException ex) {
        String traceId = newTraceId();
        log.error("[{}] entity resolution pipeline failure", traceId, ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.errorCode(), "Unable to reconcile cross-board data: " + ex.getMessage(), traceId, List.of());
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFound(ConversationNotFoundException ex) {
        String traceId = newTraceId();
        log.warn("[{}] {}", traceId, ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.errorCode(), ex.getMessage(), traceId, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String traceId = newTraceId();
        log.error("[{}] unhandled exception", traceId, ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Reference " + traceId + " when reporting this.", traceId, List.of());
    }

    // --- Spring MVC's own exceptions, routed into the same response shape ---

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String traceId = newTraceId();
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        log.warn("[{}] request body validation failed: {}", traceId, details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_FAILED,
                        "Request validation failed", traceId, details));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String traceId = newTraceId();
        log.warn("[{}] malformed request body: {}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_FAILED,
                        "Malformed request body", traceId, List.of()));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode errorCode, String message, String traceId, List<String> details) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), errorCode, message, traceId, details));
    }

    private String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
