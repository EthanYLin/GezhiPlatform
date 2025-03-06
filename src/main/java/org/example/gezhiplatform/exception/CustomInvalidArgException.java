package org.example.gezhiplatform.exception;

public class CustomInvalidArgException extends BadRequestException {
    public CustomInvalidArgException(String message) {
        super(message);
    }
}
