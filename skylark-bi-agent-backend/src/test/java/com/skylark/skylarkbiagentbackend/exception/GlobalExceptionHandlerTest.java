package com.skylark.skylarkbiagentbackend.exception;

import com.skylark.skylarkbiagentbackend.dto.error.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationException_mapsTo400WithDetails() {
        ResponseEntity<ErrorResponse> response =
                handler.handleValidation(new ValidationException("bad request", java.util.List.of("field: must not be blank")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(response.getBody().details()).containsExactly("field: must not be blank");
        assertThat(response.getBody().traceId()).isNotBlank();
    }

    @Test
    void mondayAuthenticationException_mapsTo502AndDoesNotLeakRawMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMondayAuth(new MondayAuthenticationException("token=super-secret rejected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.MONDAY_AUTHENTICATION_FAILED);
        assertThat(response.getBody().message()).doesNotContain("super-secret");
    }

    @Test
    void conversationNotFoundException_mapsTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleConversationNotFound(new ConversationNotFoundException("sess_123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    void unexpectedException_mapsTo500WithoutLeakingInternalMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new RuntimeException("npe at InternalRepository.java:42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(response.getBody().message()).doesNotContain("InternalRepository.java");
    }
}
