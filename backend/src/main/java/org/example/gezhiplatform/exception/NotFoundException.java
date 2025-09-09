package org.example.gezhiplatform.exception;

public class NotFoundException extends BadRequestException {
    public NotFoundException(String message) {
        super(message);
    }
}
