package com.interview.prep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private final String timestamp = Instant.now().toString();
    private final int status;
    private final ErrorType errorType;
    private final String message;
    private final List<FieldError> fieldErrors;

    public enum ErrorType {
        INVALID_INPUT,
        NOT_FOUND,
        EXPIRED,
        UNEXPECTED
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final Object rejectedValue;
        private final String message;
    }
}
