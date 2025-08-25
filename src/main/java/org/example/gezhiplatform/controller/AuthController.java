package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.gezhiplatform.DTO.auth.ChangePasswordRequest;
import org.example.gezhiplatform.DTO.auth.LoginRequest;
import org.example.gezhiplatform.DTO.auth.MeResponse;
import org.example.gezhiplatform.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>
 * 提供用户身份认证相关功能，包括用户登录、退出登录、获取当前用户信息以及修改密码操作。
 * 除了登录接口外，其他接口都需要用户已登录状态；该控制器不校验用户是否被封禁。
 * </p>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证", description = "用于用户登录、注销、修改密码等操作")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public MeResponse login(@RequestBody @Valid LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @SaCheckLogin
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout();
    }

    @SaCheckLogin
    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public MeResponse me() {
        Long userId = StpUtil.getLoginIdAsLong();
        return authService.getCurrentUserInfo(userId);
    }

    @SaCheckLogin
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public MeResponse changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        return authService.changePassword(userId, changePasswordRequest);
    }

}
