package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.user.*;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.user.UserManagementService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * 用户管理控制器
 * <p>
 * 该控制器仅面向【系统管理员】，提供用户信息的维护功能，包括：
 * <ul>
 *   <li>1. 根据条件搜索用户列表</li>
 *   <li>2. 获取指定用户的详细信息和角色信息</li>
 *   <li>3. 新增用户账户</li>
 *   <li>4. 更新用户基本信息和角色配置</li>
 *   <li>5. 重置用户密码、锁定/解锁用户、强制用户下线</li>
 *   <li>6. 删除用户</li>
 * </ul>
 * </p>
 * <p>
 * <b>该控制器仅面向【超级管理员】，所有操作都需要SUPER_ADMIN权限。</b>
 * </p>
 * <p>
 * 普通用户的登录、注销等认证逻辑请使用 {@link AuthController}
 * </p>
 *
 */
@SaCheckRole("SUPER_ADMIN")
@RestController
@RequestMapping("/admin/users")
@Tag(name = "用户管理(面向管理员)", description = "用户信息的维护接口")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    // ========================= GET 读取 =========================

    /**
     * 根据条件搜索用户列表
     * <p>
     * 支持<b>关键词、锁定状态、启用状态、角色类型</b>的组合查询。所有查询条件使用<b>AND逻辑</b>连接，
     * 关键词内部使用<b>OR逻辑</b>在姓名、用户名之间进行匹配。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出异常</b></p>
     * <p>
     * 查询条件组合方式：
     * <ul>
     *   <li><b>关键词搜索</b>：当keyword不为null且非空时，在姓名、用户名进行模糊匹配</li>
     *   <li><b>锁定状态过滤</b>：当isLocked不为null时，只返回指定锁定状态的用户</li>
     *   <li><b>启用状态过滤</b>：当isEnabled不为null时，只返回指定启用状态的用户</li>
     *   <li><b>角色类型过滤</b>：当roleType不为null时，只返回拥有指定角色类型的用户</li>
     *   <li><b>组合查询</b>：多个条件同时存在时，使用AND逻辑连接</li>
     *   <li><b>无条件查询</b>：所有参数为null时，返回系统中的所有用户</li>
     * </ul>
     * </p>
     * <p>
     * 使用场景举例：
     * <ul>
     *   <li>查找所有被锁定的用户：keyword=null, isLocked=true, isEnabled=null, roleType=null</li>
     *   <li>查找所有班主任：keyword=null, isLocked=null, isEnabled=null, roleType=班主任</li>
     *   <li>搜索姓名包含"张"的启用用户：keyword="张", isLocked=null, isEnabled=true, roleType=null</li>
     *   <li>查找被锁定的超级管理员：keyword=null, isLocked=true, isEnabled=null, roleType=超级管理员</li>
     * </ul>
     * </p>
     *
     * @param keyword   关键词搜索条件，为null或空白时不进行关键词搜索，支持姓名、用户名模糊匹配
     * @param isLocked  锁定状态过滤条件，为null时不进行锁定状态过滤
     * @param isEnabled 启用状态过滤条件，为null时不进行启用状态过滤
     * @param roleType  角色类型过滤条件，为null时不进行角色类型过滤
     * @param pageable  分页参数，默认每页20条记录，最大页大小为1000
     * @return 符合所有条件的用户详细信息分页结果
     * @throws BadRequestException 当分页大小超过1000或排序字段无效时抛出
     * @apiNote GET /admin/users?keyword=张三&isLocked=false&isEnabled=true&roleType=班主任&page=0&size=20&sort
     * =id,asc
     */
    @GetMapping
    @Transactional
    @Operation(summary = "条件搜索用户列表")
    public PageResult<UserDetailResponse> searchUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean isLocked,
        @RequestParam(required = false) Boolean isEnabled,
        @RequestParam(required = false) String roleType,
        @PageableDefault(size = 20) Pageable pageable
    ) throws BadRequestException {
        @Nullable RoleType roleTypeEnum = null;
        if (roleType != null && !roleType.isBlank()) roleTypeEnum = RoleType.fromDesc(roleType);
        return userManagementService.searchUsers(keyword, isLocked, isEnabled, roleTypeEnum, pageable);
    }

    /**
     * 根据用户ID获取用户详细信息
     * <p>
     * 获取指定用户的完整详细信息，包括ID、姓名、用户名、拥有的角色列表（可读版）、是否被锁定、是否启用、上次登录时间。
     * </p>
     *
     * @param userId 用户ID
     * @return 用户详细信息DTO
     * @throws NotFoundException 当指定ID的用户不存在时
     * @apiNote GET /admin/users/{userId}
     */
    @GetMapping("/{userId}")
    @Transactional
    @Operation(summary = "根据ID获取用户信息")
    public UserDetailResponse getUserDetail(
        @PathVariable @NotNull Long userId
    ) throws NotFoundException {
        return userManagementService.getUserDetail(userId);
    }

    /**
     * 根据用户ID获取该用户拥有的所有角色
     * <p>
     * 获取指定用户拥有的所有角色的详细信息，包括角色类型、角色相关的权限配置等。
     * 返回的角色详情以配置代码形式出现，可用于更新用户角色列表。
     * </p>
     *
     * @param userId 用户ID
     * @return 用户角色详情DTO列表
     * @throws NotFoundException 当指定ID的用户不存在时
     * @apiNote GET /admin/users/{userId}/roles
     */
    @GetMapping("/{userId}/roles")
    @Transactional
    @Operation(summary = "根据ID获取用户角色")
    public List<UserRoleDetailsDTO> getUserRoles(
        @PathVariable @NotNull Long userId
    ) throws NotFoundException {
        return userManagementService.getUserRoles(userId);
    }

    // ========================= POST 新增 =========================

    /**
     * 手动新增一个用户
     * <p>
     * 根据用户请求创建一个新用户，会自动检查用户名的唯一性。
     * 新创建的用户将使用请求中提供的基本信息，包括姓名、用户名、密码（均可为空）。
     * 该用户的角色列表不能为null。
     * </p>
     *
     * @param request 新用户请求DTO，包含用户的基本信息
     * @return 新创建用户的详细信息DTO，HTTP状态码201
     * @throws BadRequestException 当用户名已存在时
     * @apiNote POST /admin/users
     */
    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增用户")
    public UserDetailResponse addUser(
        @RequestBody @Valid NewUserRequest request
    ) throws BadRequestException {
        return userManagementService.addUser(request);
    }

    // ========================= PUT 更新 =========================

    /**
     * 更新指定用户的基本信息
     * <p>
     * 更新用户的基本信息，包括姓名和用户名。
     * </p>
     *
     * @param userId  要更新的用户ID
     * @param request 用户更新请求DTO，包含新的用户信息
     * @return 更新后的用户详细信息DTO
     * @throws NotFoundException   当指定ID的用户不存在时
     * @throws BadRequestException 当新用户名已被其他用户使用时
     * @apiNote PUT /admin/users/{userId}/info
     */
    @PutMapping("/{userId}/info")
    @Transactional
    @Operation(summary = "更新用户基本信息")
    public UserDetailResponse updateUserInfo(
        @PathVariable @NotNull Long userId,
        @RequestBody @Valid UserUpdateRequest request
    ) throws NotFoundException, BadRequestException {
        return userManagementService.updateUserInfo(userId, request);
    }

    /**
     * 更新指定用户的角色列表
     * <p>
     * 完全替换用户的角色列表，首先清空用户当前的所有角色，
     * 然后添加请求中指定的新角色列表。
     * </p>
     * <p>
     * 注意事项：
     * <ul>
     *   <li>该操作会<b>完全替换</b>用户的角色，而不是增量更新</li>
     *   <li>角色请求中的配置信息必须符合相应角色类型的要求</li>
     *   <li>角色列表不能为null</li>
     * </ul>
     * </p>
     *
     * @param userId       要更新角色的用户ID
     * @param roleRequests 新的角色详情DTO列表
     * @return 更新后的用户角色详情DTO列表
     * @throws NotFoundException         当指定ID的用户不存在时
     * @throws CustomInvalidArgException 当角色配置信息无效时
     * @apiNote PUT /admin/users/{userId}/roles
     */
    @PutMapping("/{userId}/roles")
    @Transactional
    @Operation(summary = "更新用户角色列表")
    public List<UserRoleDetailsDTO> updateUserRoles(
        @PathVariable @NotNull Long userId,
        @RequestBody @Valid List<UserRoleDetailsDTO> roleRequests
    ) throws NotFoundException, CustomInvalidArgException {
        return userManagementService.updateUserRoles(userId, roleRequests);
    }

    /**
     * 重置指定用户的密码
     * <p>
     * 为指定用户生成一个新的临时密码，密码格式为：<b>gz-MMdd-随机六位数字</b>。
     * 重置密码后，用户将被强制下线，用户必须使用新的初始密码重新登录并设置新密码后，才能重新启用账户。
     * </p>
     *
     * @param userId 要重置密码的用户ID
     * @return 密码重置响应DTO，包含用户信息和新生成的临时密码
     * @throws NotFoundException 当指定ID的用户不存在时
     * @apiNote POST /admin/users/{userId}/password
     */
    @PostMapping("/{userId}/password")
    @Transactional
    @Operation(summary = "重置用户密码")
    public PasswordResetResponse resetPassword(
        @PathVariable @NotNull Long userId
    ) throws NotFoundException {
        return userManagementService.resetPassword(userId);
    }

    /**
     * 锁定指定用户
     * <p>
     * 将指定用户设置为锁定状态，锁定后用户无法正常使用系统。
     * 用户将被强制下线，直到管理员解锁该用户。
     * </p>
     *
     * @param userId 要锁定的用户ID
     * @throws NotFoundException 当指定ID的用户不存在时
     * @apiNote POST /admin/users/{userId}/lock
     */
    @PostMapping("/{userId}/lock")
    @Transactional
    @Operation(summary = "锁定用户")
    public void lockUser(
        @PathVariable @NotNull Long userId
    ) throws NotFoundException {
        userManagementService.lockUser(userId);
    }

    /**
     * 解锁指定用户
     * <p>
     * 将指定用户解除锁定状态。用户将被强制下线，并要求重新登录。
     * 用户是否能正常使用账户，取决于账户的enabled状态。
     * </p>
     *
     * @param userId 要解锁的用户ID
     * @throws NotFoundException 当指定ID的用户不存在时
     * @apiNote POST /admin/users/{userId}/unlock
     */
    @PostMapping("/{userId}/unlock")
    @Transactional
    @Operation(summary = "解锁用户")
    public void unlockUser(
        @PathVariable @NotNull Long userId
    ) throws NotFoundException {
        userManagementService.unlockUser(userId);
    }

    /**
     * 强制指定用户下线
     * <p>
     * 强制指定用户从系统中下线，使其当前所有会话立即失效。
     * </p>
     *
     * @param userId 要强制下线的用户ID
     * @apiNote POST /admin/users/{userId}/kickout
     */
    @PostMapping("/{userId}/kickout")
    @Operation(summary = "强制用户下线")
    public void kickoutUser(
        @PathVariable @NotNull Long userId
    ) {
        userManagementService.kickoutUser(userId);
    }

    // ========================= DELETE 删除 =========================

    /**
     * 批量删除用户
     * <p>
     * 根据用户ID列表批量删除用户记录。如果某个用户ID不存在，该ID会被忽略，
     * 不会影响其他用户ID的删除操作。
     * </p>
     *
     * @param userIds 要删除的用户ID列表
     * @apiNote DELETE /admin/users?userIds=1,2,3
     */
    @DeleteMapping
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "批量删除用户")
    public void deleteUsers(
        @RequestParam("userIds") @NotNull Collection<Long> userIds
    ) {
        userManagementService.deleteUsers(userIds);
    }
}
