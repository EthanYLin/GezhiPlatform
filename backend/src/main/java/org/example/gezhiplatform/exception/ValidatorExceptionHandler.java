package org.example.gezhiplatform.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidatorExceptionHandler {

    // =======================  @Valid 校验未通过  =============================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors().stream()
                                           .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                                           .collect(Collectors.joining(", "));
        if (errorMessage.isEmpty()) {
            errorMessage = bindingResult.getGlobalErrors().stream()
                                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                        .collect(Collectors.joining(", "));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        String errorMessages = ex.getParameterValidationResults().stream()
                                 .flatMap(result -> result.getResolvableErrors().stream()
                                     .map(err -> {
                                         String paramName = result.getMethodParameter().getParameterName();
                                         String msg = err.getDefaultMessage();
                                         return (paramName != null ? paramName + ": " : "") + msg;
                                     }))
                                 .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessages)
        );
    }

}
