package org.example.gezhiplatform.exception;

/**
 * 一般由于需要的字段没有在指定类中找到而引发
 * 非客户端导致的异常, 而是代码编写问题
 */
public class FieldNotFoundException extends RuntimeException {
    public FieldNotFoundException(String message) {
        super(message);
    }
}
