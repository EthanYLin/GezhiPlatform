package org.example.gezhiplatform.service.auth;

import cn.dev33.satoken.stp.StpUtil;
import org.example.gezhiplatform.DTO.auth.ChangePasswordRequest;
import org.example.gezhiplatform.DTO.auth.LoginRequest;
import org.example.gezhiplatform.DTO.auth.MeResponse;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.UserRepository;
import org.example.gezhiplatform.service.user.UserManagementService;
import org.example.gezhiplatform.utils.PasswordEncryptUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证与授权服务
 * <p>
 * 负责用户身份认证相关的核心业务逻辑，包括用户登录、注销、密码修改功能。
 * <b>同时还提供获取当前用户信息的方法，供其他模块使用。</b>
 * </p>
 * <p>
 * 管理员查看或维护用户信息，请使用 {@link UserManagementService}。
 * </p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {this.userRepository = userRepository;}

    /**
     * 用户登录
     * <p>
     * 执行用户身份认证流程，包括用户名密码验证、会话创建和账户状态同步。
     * 该方法会更新用户的最后登录时间，并将用户的封禁状态同步到SaToken会话管理中。
     * </p>
     * <p>
     * 登录流程：
     * <ol>
     *   <li>验证用户名和密码</li>
     *   <li>创建用户登录会话</li>
     *   <li>更新用户最后登录时间</li>
     *   <li>同步用户封禁和启用状态到SaToken</li>
     *   <li>返回用户信息和访问令牌</li>
     * </ol>
     * </p>
     *
     * @param loginRequest 登录请求体，包含用户名和密码
     * @return 当前登录用户信息响应体，包含用户基本信息和访问令牌
     * @throws BadRequestException 当用户名或密码为空、用户不存在或密码错误时抛出
     */
    public MeResponse login(@NotNull LoginRequest loginRequest) throws BadRequestException {

        // 验证用户名和密码并登录
        if (loginRequest.username() == null || loginRequest.password() == null) {
            throw new BadRequestException("用户名或密码不能为空");
        }
        User user = userRepository.findByUsername(loginRequest.username()).orElseThrow(
            () -> new BadRequestException("用户名或密码错误")
        );
        if (user.getEncryptedPassword() == null || !PasswordEncryptUtils.matches(loginRequest.password(), user.getEncryptedPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }
        StpUtil.login(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        user = userRepository.save(user);

        // 查看封禁与启用情况, 并同步到SaToken
        if (user.isLocked() || !user.isEnabled()) StpUtil.disable(user.getId(), -1); // 永久封禁
        else StpUtil.untieDisable(user.getId());

        // 返回结果
        return MeResponse.of(user, StpUtil.getTokenValue());
    }

    /**
     * 用户退出登录
     * <p>
     * 注销当前用户的登录会话，清除SaToken中的会话信息。
     * </p>
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 修改用户密码
     * <p>
     * 允许用户修改自己的登录密码，密码修改成功后会自动重新登录用户并返回新的访问令牌。
     * </p>
     * <p>
     * 密码修改验证规则：
     * <ul>
     *   <li>用户不能处于封禁状态</li>
     *   <li>用户必须已设置用户名和密码</li>
     *   <li>对于未启用账户，新密码不能与初始默认密码相同</li>
     *   <li>必须正确提供当前密码</li>
     * </ul>
     * </p>
     * <p>
     * 特殊处理：
     * <ul>
     *   <li>如果用户账户未启用，修改密码后会自动启用账户</li>
     *   <li>密码修改成功后会使用新密码自动重新登录</li>
     * </ul>
     * </p>
     *
     * @param userId                用户ID，标识要修改密码的用户
     * @param changePasswordRequest 密码修改请求体，包含旧密码和新密码
     * @return 用户信息响应体，包含用户基本信息和新的访问令牌
     * @throws BadRequestException  当用户被封禁、用户名密码为空、新密码与默认密码相同或旧密码错误时抛出
     * @throws NotFoundException    当指定用户ID不存在时抛出
     */
    public MeResponse changePassword(@NotNull Long userId, @NotNull ChangePasswordRequest changePasswordRequest) throws BadRequestException {
        // 若用户被封禁(Locked), 则不允许修改密码
        User user = userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("用户(" + userId + ")不存在")
        );
        if (user.isLocked()) {
            throw new BadRequestException("用户(" + userId + ")已被封禁, 无法修改密码");
        }
        // 若用户没有设置用户名或密码, 则不允许修改密码
        if (user.getUsername() == null || user.getEncryptedPassword() == null) {
            throw new BadRequestException("用户(" + userId + ")当前的用户名或密码为空, 请联系管理员");
        }
        // 新密码不能与默认密码相同
        if (!user.isEnabled() && PasswordEncryptUtils.matches(changePasswordRequest.newPassword(), user.getEncryptedPassword())) {
            throw new BadRequestException("新密码不能与初始默认密码相同");
        }
        // 验证旧密码并修改密码
        if (!PasswordEncryptUtils.matches(changePasswordRequest.oldPassword(), user.getEncryptedPassword())) {
            throw new BadRequestException("原密码错误");
        }
        user.setEncryptedPassword(PasswordEncryptUtils.encode(changePasswordRequest.newPassword()));
        // 若未启用账户(Enabled), 则启用账户并写入数据库和SaToken
        user.setEnabled(true);
        user = userRepository.save(user);
        // 直接登录并返回Token
        return this.login(new LoginRequest(user.getUsername(), changePasswordRequest.newPassword()));
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 根据用户ID获取用户的基本信息，包括用户详情和当前的访问令牌。
     * </p>
     *
     * @param userId 当前登录用户的ID
     * @return 用户信息响应体，包含用户基本信息和当前访问令牌
     * @throws NotFoundException 当指定用户ID不存在时抛出
     */
    public MeResponse getCurrentUserInfo(@NotNull Long userId) throws NotFoundException {
        User user = userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("用户(" + userId + ")不存在")
        );
        return MeResponse.of(user, StpUtil.getTokenValue());
    }

    /**
     * 根据用户ID获取用户实体, 便于其他模块调用用户信息。
     *
     * @param userId 用户ID
     * @return 用户实体
     * @throws NotFoundException 当指定用户ID不存在时抛出
     */
    public User getUserById(@NotNull Long userId) throws NotFoundException {
        return userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("用户(" + userId + ")不存在")
        );
    }

    /**
     * 获取当前登录用户实体, 便于其他模块调用用户信息。
     *
     * @return 当前登录用户实体
     * @throws NotFoundException 当当前登录用户ID不存在时抛出
     */
    public User getCurrentUser() throws NotFoundException {
        return getUserById(StpUtil.getLoginIdAsLong());
    }
}
