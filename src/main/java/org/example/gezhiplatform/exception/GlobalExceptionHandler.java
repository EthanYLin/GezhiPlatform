package org.example.gezhiplatform.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =======================  业务异常处理  =============================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage())
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage())
        );
    }

    // =======================  全局异常处理  =============================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // 生成异常编号
        String rand3digits = String.format("%03d", new Random().nextInt(1000));
        String errorId = "ERR-"
                         + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd-HHmmss"))
                         + "-" + rand3digits;
        // 记录异常信息
        log.error("全局500异常处理，错误编号：{}，异常信息：{}", errorId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误，追踪编号:" + errorId)
        );
    }

}
