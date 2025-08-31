package org.example.gezhiplatform.DTO.user;

import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.User;
import org.jetbrains.annotations.Nullable;

/**
 * 密码重置结果响应体
 * @param id 用户ID
 * @param name 姓名
 * @param username 用户名(登录用)
 * @param defaultPassword 默认密码
 */
public record PasswordResetResponse(
    @NotNull Long id, // 用户ID
    @Nullable String name, // 姓名
    @Nullable String username, // 用户名(登录用)
    @NotNull String defaultPassword // 默认密码
) {
    public static PasswordResetResponse of(User user, String defaultPassword) {
        return new PasswordResetResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            defaultPassword
        );
    }
}
