package org.example.gezhiplatform.entity.user_role;

/**
 * Role角色类的applyFilter()过滤器设置异常
 * 一般由于需要的字段没有在Student类中出现而引发
 * 非客户端导致的异常, 而是代码编写问题
 */
public class FilterSettingException extends RuntimeException {
    public FilterSettingException(String message) {
        super(message);
    }
}
