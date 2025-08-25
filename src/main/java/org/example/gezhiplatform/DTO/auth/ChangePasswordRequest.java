package org.example.gezhiplatform.DTO.auth;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求
 * @param oldPassword 原密码
 * @param newPassword 新密码(必须包含字母、数字、特殊符号中的至少两类, 且长度在6~30个字符之间)
 */
public record ChangePasswordRequest(
    @NotBlank
    String oldPassword, // 原密码

    @NotBlank
    @Size(min = 6, max = 30, message = "密码应该在6~30个字符之间")
    @Pattern(
        regexp = "^(?![A-Za-z]+$)(?!\\d+$)(?![^A-Za-z0-9]+$)[A-Za-z\\d[^A-Za-z0-9]]{6,30}$",
        message = "密码必须包含字母、数字、特殊符号中的至少两类"
    )
    String newPassword // 新密码
) {
}
