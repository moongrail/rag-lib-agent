package com.ragassistant.common.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRag_returnsBadRequest() {
        ProblemDetail detail = handler.handleRag(new RagException("bad input"));
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getDetail()).isEqualTo("bad input");
        assertThat(detail.getTitle()).isEqualTo("Business error");
    }

    @Test
    void handleDocumentNotFound_returnsNotFound() {
        ProblemDetail detail = handler.handleNotFound(new DocumentNotFoundException("123"));
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getDetail()).isEqualTo("Document not found: 123");
    }

    @Test
    void handleUnexpected_returnsInternalError() {
        ProblemDetail detail = handler.handleUnexpected(new RuntimeException("boom"));
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getTitle()).isEqualTo("Internal error");
    }
}
