package org.example.gezhiplatform.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error
) {
    public ErrorResponse(int status, String error) {
        this(LocalDateTime.now(), status, error);
    }
}
