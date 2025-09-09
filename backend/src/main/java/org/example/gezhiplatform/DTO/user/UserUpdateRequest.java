package org.example.gezhiplatform.DTO.user;

import org.jetbrains.annotations.Nullable;

/**
 * 用户信息更新请求
 * @param name 姓名
 * @param username 用户名（登录用）
 */
public record UserUpdateRequest(
    @Nullable String name, // 姓名
    @Nullable String username // 用户名（登录用）
) {}
