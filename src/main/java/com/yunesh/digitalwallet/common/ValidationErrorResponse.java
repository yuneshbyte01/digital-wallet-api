package com.yunesh.digitalwallet.common;

import java.time.Instant;
import java.util.List;

public record ValidationErrorResponse(
        int status,
        String message,
        List<FieldError> errors,
        Instant timestamp
) {
    public record FieldError(String field, String message) {}
}