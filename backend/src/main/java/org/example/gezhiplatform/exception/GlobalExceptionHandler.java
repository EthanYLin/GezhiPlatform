package org.example.gezhiplatform.exception;

import cn.dev33.satoken.exception.*;
import org.jetbrains.annotations.NotNull;
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

    // =======================  认证或鉴权异常处理  =============================

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ErrorResponse> handleNotLoginException(NotLoginException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期，请重新登录。(" + ex.getCode() + ")")
        );
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<ErrorResponse> handleNotPermissionException(SaTokenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ErrorResponse(HttpStatus.FORBIDDEN.value(), "无权限进行该操作，如有需要请联系管理员。（" + ex.getCode() + "）")
        );
    }

    @ExceptionHandler(DisableServiceException.class)
    public ResponseEntity<ErrorResponse> handleDisableServiceException(DisableServiceException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ErrorResponse(HttpStatus.FORBIDDEN.value(), "该账号被锁定或未启用，请退出登录后重新登录查看详细信息。（" + ex.getCode() + "）")
        );
    }

    @ExceptionHandler(SaTokenException.class)
    public ResponseEntity<ErrorResponse> handleSaTokenException(SaTokenException ex) {
        String traceId = logError("通用SaToken异常捕获(SaToken异常业务码: " + ex.getCode() + ")", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                              "身份认证异常，请重新登录。（错误编码: " + ex.getMessage() + ", 追踪编号: " + traceId + "）")
        );
    }


    // =======================  全局异常处理  =============================

    public static @NotNull String logError(@NotNull String logPrefix, Exception ex) {
        // 生成异常编号
        String rand3digits = String.format("%03d", new Random().nextInt(1000));
        String traceId = "ERR-"
                         + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd-HHmmss"))
                         + "-" + rand3digits;
        // 记录异常信息
        log.error("{}, 异常追踪编号: {}, 异常信息: {}", logPrefix, traceId, ex.getMessage(), ex);
        return traceId;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        String traceId = logError("全局500异常捕获", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "发生异常！请检查您的请求是否正确，或者持有该追踪编号向管理员查询:" + traceId)
        );
    }

}
