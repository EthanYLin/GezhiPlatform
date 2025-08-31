package org.example.gezhiplatform.DTO.user;

import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.User;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 用户详情响应体
 * <p>
 * 供管理员查看或维护用户信息时使用的DTO。
 * </p>
 * @param id 用户ID
 * @param name 姓名
 * @param username 用户名(登录用)
 * @param roles 拥有的角色列表(包含角色类型名称与范围)
 * @param isLocked 是否被锁定
 * @param isEnabled 是否启用
 * @param lastLoginTime 上次登录时间
 */
public record UserDetailResponse(
    @NotNull Long id, // 用户ID
    @Nullable String name, // 姓名
    @Nullable String username, // 用户名
    @NotNull List<RoleInfo> roles, // 具有的所有角色
    boolean isLocked, // 是否被锁定
    boolean isEnabled, // 是否启用
    @Nullable LocalDateTime lastLoginTime // 上次登录时间
) {

    /**
     * 角色信息
     * <p>
     * 包含该用户的单个角色的类型与作用范围描述。
     * </p>
     * @param roleType 角色类型名称(如: 班主任)
     * @param roleAndScope 角色及其作用范围(如: 班主任: 高一(03)班)
     */
    record RoleInfo(
        @NotNull String roleType, // 角色类型名称(如: 班主任)
        @NotNull String roleAndScope // 角色及其作用范围(如: 班主任: 高一(03)班)
    ) {}

    public static UserDetailResponse of(User user) {
        List<RoleInfo> roleInfos = user.getRoles().stream()
            .map(role -> new RoleInfo(
                role.getRoleType().getDesc(),
                role.getRoleAndScope()
            ))
            .toList();

        return new UserDetailResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            roleInfos,
            user.isLocked(),
            user.isEnabled(),
            user.getLastLoginTime()
        );
    }
}
