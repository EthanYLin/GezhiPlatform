package org.example.gezhiplatform.exception;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class HttpExceptionHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
        @NotNull MethodParameter returnType,
        @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
        @NotNull MethodParameter returnType,
        @NotNull MediaType contentType,
        @NotNull Class<? extends HttpMessageConverter<?>> converterType,
        @NotNull ServerHttpRequest request,
        @NotNull ServerHttpResponse response
    ) {
        if (body instanceof ProblemDetail pd) {
            int status = pd.getStatus();
            String message = pd.getDetail() != null ? pd.getDetail() : pd.getTitle();
            return new ErrorResponse(status, message);
        }
        return body;
    }

}
