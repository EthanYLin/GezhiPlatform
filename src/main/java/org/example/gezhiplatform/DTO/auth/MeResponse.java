package org.example.gezhiplatform.DTO.auth;

import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.role.Role;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 当前登录用户信息响应体
 * @param token 令牌
 * @param id 用户ID(数据库内自增)
 * @param name 姓名
 * @param username 用户名(登录用)
 * @param isLocked 是否被锁定
 * @param isEnabled 是否启用(如果当前密码是默认的初始密码, 则不启用, 只能进行修改密码功能)
 * @param roles 拥有的角色列表(包括角色名称与范围, 例如"班主任: 高一(01)班")
 */
public record MeResponse (
    @NotNull String token, // 令牌
    @NotNull Long id, // 用户ID(数据库内自增)
    @Nullable String name, // 姓名
    @NotNull String username, // 用户名(登录用)
    @NotNull Boolean isLocked, // 是否被锁定
    @NotNull Boolean isEnabled, // 是否启用(如果当前密码是默认的初始密码, 则不启用, 只能进行修改密码功能)
    @NotNull List<String> roles // 拥有的角色列表(包括角色名称与范围, 例如"班主任: 高一(01)班")
){

    /**
     * 根据用户实体创建当前用户信息DTO响应体
     * @param user 用户实体
     * @param token 当前用于登录的令牌
     * @return 当前用户信息响应体
     */
    public static MeResponse of(@NotNull User user, String token) {
        List<String> roleDescriptions = user.getRoles().stream()
            .map(Role::getRoleAndScope).toList();
        return new MeResponse(
            token,
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.isLocked(),
            user.isEnabled(),
            roleDescriptions
        );
    }
}
