package org.example.gezhiplatform.DTO.user;

import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 新建用户请求体
 * @param name 用户姓名（可为空）
 * @param username 用户名（登录用）
 * @param defaultPassword 初始密码（明文）
 * @param roles 具有的角色列表（不可为空）
 */
public record NewUserRequest(
    @Nullable String name, // 用户姓名
    @Nullable String username, // 用户名(登录用)
    @Nullable String defaultPassword, // 初始密码(明文)
    @NotNull List<UserRoleDetailsDTO> roles // 具有的角色列表
) {
    public User toUser() throws CustomInvalidArgException {
        return new User(
            name,
            username,
            defaultPassword,
            roles.stream().map(UserRoleDetailsDTO::toRole).toList()
        );
    }
}
