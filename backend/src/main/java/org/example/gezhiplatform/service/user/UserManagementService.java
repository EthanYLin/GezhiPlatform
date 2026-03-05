package org.example.gezhiplatform.service.user;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.user.*;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.entity.role.Role;
import org.example.gezhiplatform.entity.role.SuperAdmin;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.AuditRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.example.gezhiplatform.service.auth.AuthService;
import org.example.gezhiplatform.utils.PasswordEncryptUtils;
import org.example.gezhiplatform.utils.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;

/**
 * 用户维护服务
 * 负责管理员查看或维护用户信息，该服务类【只面向系统管理员】
 * 用户的登录、注销、封禁、重置等认证逻辑，请见 {@link AuthService}
 */
@Service
public class UserManagementService {


    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);
    private final UserRepository userRepository;
    private final Environment environment;
    private final AuditRepository auditRepository;

    public UserManagementService(UserRepository userRepository, Environment environment,
        AuditRepository auditRepository
    ) {
        this.userRepository = userRepository;
        this.environment = environment;
        this.auditRepository = auditRepository;
    }

    @PostConstruct
    private void init() {
        checkFields();
        initAdminUser();
    }

    private void checkFields() {
        getField(User.class, "name", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "用户管理服务(UserManagementService)需要依照姓名(name)进行搜索, 但未在User类中找到String类型的name字段。"));

        getField(User.class, "username", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "用户管理服务(UserManagementService)需要依照用户名(username)进行搜索, 但未在User类中找到String类型的username字段。"));

        getField(User.class, "isLocked", boolean.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "用户管理服务(UserManagementService)需要依照锁定状态(isLocked)进行过滤, 但未在User类中找到boolean类型的isLocked字段。"));

        getField(User.class, "isEnabled", boolean.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "用户管理服务(UserManagementService)需要依照启用状态(isEnabled)进行过滤, 但未在User类中找到boolean类型的isEnabled字段。"));

        getField(User.class, "roles", List.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "用户管理服务(UserManagementService)需要依照角色(roles)进行过滤, 但未在User类中找到List类型的roles字段。"));
    }

    private void initAdminUser() {
        // 只在生产环境下初始化管理员用户
        boolean isProd = Arrays.stream(environment.getActiveProfiles()).anyMatch(
            profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production")
        );
        if (!isProd) return;
        
        // 如果在启动时仓库内没有任何用户，则创建一个管理员用户。用户名为admin，密码为123456，但不启用账户。
        if (userRepository.count() != 0) return;
        String password = "123456";
        User admin = new User("admin", "admin", password, List.of(new SuperAdmin()));
        log.warn("初始化管理员用户成功，用户名为admin，密码为{}", password);
        userRepository.save(admin);
    }

    // ========================= GET 读取 =========================
    // 包括 读取用户列表、用户信息详情、用户拥有的角色详情

    /**
     * 根据多种条件搜索用户列表
     * <p>
     * 该方法支持<b>关键词、锁定状态、启用状态、角色类型</b>的组合查询。
     * 所有查询条件使用<b>AND逻辑</b>连接，关键词在姓名、用户名之间进行匹配。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出 {@link BadRequestException}</b></p>
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
     *   <li>查找所有班主任：keyword=null, isLocked=null, isEnabled=null, roleType=CLASS_ADVISER</li>
     *   <li>搜索姓名包含"张"的启用用户：keyword="张", isLocked=null, isEnabled=true, roleType=null</li>
     *   <li>查找被锁定的超级管理员：keyword=null, isLocked=true, isEnabled=null, roleType=SUPER_ADMIN</li>
     * </ul>
     * </p>
     *
     * @param keyword  关键词搜索条件，为null或空白时不进行关键词搜索，支持姓名、用户名模糊匹配
     * @param isLocked 锁定状态过滤条件，为null时不进行锁定状态过滤
     * @param isEnabled 启用状态过滤条件，为null时不进行启用状态过滤
     * @param roleType 角色类型过滤条件，为null时不进行角色类型过滤
     * @param pageable 分页参数(最大页大小为1000)
     * @return 符合所有条件的用户详细信息分页结果
     * @throws BadRequestException 当分页大小超过1000或排序字段无效时抛出
     */
    public PageResult<UserDetailResponse> searchUsers(
        @Nullable String keyword,
        @Nullable Boolean isLocked,
        @Nullable Boolean isEnabled,
        @Nullable RoleType roleType,
        @NotNull Pageable pageable
    ) throws BadRequestException {

        if (pageable.getPageSize() > 1000) {
            throw new BadRequestException("分页的最大页大小为1000条记录。");
        }
        Set<String> illegalSortProperties = ReflectionUtils.getIllegalSortProperties(User.class, pageable);
        if (!illegalSortProperties.isEmpty()) {
            throw new BadRequestException("分页排序参数中包含无效的字段: " + String.join(", ", illegalSortProperties));
        }

        Specification<User> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 关键词(id, name, username)
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                    cb.like(root.get("name"), "%" + keyword + "%"),
                    cb.like(root.get("username"), "%" + keyword + "%")
                ));
            }
            // 是否被锁定
            if (isLocked != null) {
                predicates.add(cb.equal(root.get("isLocked"), isLocked));
            }
            // 是否启用
            if (isEnabled != null) {
                predicates.add(cb.equal(root.get("isEnabled"), isEnabled));
            }
            // 用户角色
            if (roleType != null) {
                Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
                predicates.add(cb.equal(roleJoin.type(), roleType.getEntityClass()));
            }

            // 如果没有任何筛选条件，返回所有学生；否则AND合并所有条件
            if (predicates.isEmpty()) {
                return cb.conjunction();
            } else {
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };

        return new PageResult<>(
            userRepository
                .findBy(spec, q -> q.sortBy(pageable.getSort()).page(pageable))
                .map(UserDetailResponse::of)
        );
    }

    /**
     * 根据用户ID获取用户详细信息
     * <p>
     * 该方法获取指定用户的完整详细信息，包括ID、姓名、用户名、拥有的角色列表（可读版）、是否被锁定、是否启用、上次登录时间。
     * </p>
     *
     * @param userId 用户ID
     * @return 用户详细信息DTO
     * @throws NotFoundException 当指定ID的用户不存在时
     */
    @Transactional
    public UserDetailResponse getUserDetail(@NotNull Long userId) throws NotFoundException {
        return userRepository.findById(userId).map(UserDetailResponse::of).orElseThrow(
            () -> new NotFoundException("未找到ID为 " + userId + " 的用户")
        );
    }

    /**
     * 根据用户ID获取该用户拥有的所有角色
     * <p>
     * 该方法获取指定用户拥有的所有角色的详细信息，包括角色类型、角色相关的权限配置等。
     * 该方法返回的 {@link UserRoleDetailsDTO} 中的角色详情以配置代码形式出现，可用于更新用户角色列表。
     * </p>
     *
     * @param userId 用户ID
     * @return 用户角色详情DTO列表
     * @throws NotFoundException 当指定ID的用户不存在时
     */
    @Transactional
    public List<UserRoleDetailsDTO> getUserRoles(@NotNull Long userId) throws NotFoundException {
        return userRepository.findById(userId).map(User::getRoles).map(UserRoleDetailsDTO::of).orElseThrow(
            () -> new NotFoundException("未找到ID为 " + userId + " 的用户")
        );
    }

    // ========================= POST 新增 =========================
    // 包括 手动新增一个用户、从Excel批量导入或更新用户

    /**
     * 手动新增一个用户
     * <p>
     * 该方法根据用户请求创建一个新用户，会自动检查用户名的唯一性。
     * 新创建的用户将使用请求中提供的基本信息，包括姓名、用户名、密码（均可为空）。
     * 该用户的角色列表不能为null。
     * </p>
     *
     * @param request 新用户请求DTO，包含用户的基本信息
     * @return 新创建用户的详细信息DTO
     * @throws BadRequestException 当用户名已存在时
     */
    public UserDetailResponse addUser(@NotNull NewUserRequest request) {
        if (request.username() != null && userRepository.findByUsername(request.username()).isPresent()) {
            throw new BadRequestException("用户名 " + request.username() + " 已存在");
        }
        User user = userRepository.save(request.toUser());
        return UserDetailResponse.of(user);
    }

    /**
     * 批量导入用户
     * <p>
     * 该方法一次性导入多个用户记录，适用于从Excel等文件批量导入用户数据的场景。方法会对每个用户记录进行验证，确保用户名的唯一性，并且所有用户记录必须满足基本的合法性要求。
     * 如果请求中存在任何一个用户记录不合法（例如用户名重复、角色配置错误等），整个批量导入操作将会失败，并抛出 {@link BadRequestException}，异常信息中会包含所有不合法记录的详细错误描述。
     * </p>
     * @param requests 新用户请求DTO列表，每个DTO包含一个用户的基本信息
     * @throws BadRequestException 当请求中存在任何一个用户记录不合法时抛出，异常信息中包含所有不合法记录的详细错误描述
     * @see UserManagementService#addUser(NewUserRequest)
     */
    @Transactional
    public void importUsers(@NotNull List<NewUserRequest> requests) throws BadRequestException {
        // 检查请求中是否有重复的用户名
        Set<String> seen = new HashSet<>();
        Set<String> duplicateUsernames = requests
            .stream()
            .map(NewUserRequest::username)
            .filter(name -> !seen.add(name))
            .collect(Collectors.toSet());
        if (!duplicateUsernames.isEmpty()) {
            throw new BadRequestException("请求中包含重复的用户名: " + duplicateUsernames);
        }

        // 检查请求中的用户名是否已存在于数据库中
        Set<String> usernames = requests.stream()
            .map(NewUserRequest::username)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<String> existingUsernames = userRepository.findByUsernameIn(usernames).stream()
            .map(User::getUsername)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (!existingUsernames.isEmpty()) {
            throw new BadRequestException("以下用户名已存在: " + existingUsernames);
        }

        List<String> errors = new ArrayList<>();
        List<User> users = new ArrayList<>();
        for (NewUserRequest request : requests) {
            try {
                User user = request.toUser();
                users.add(user);
            } catch (CustomInvalidArgException e) {
                errors.add(String.format("在创建用户 %s(%s) 时发生异常: %s", request.name(), request.username(), e.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            throw new BadRequestException("在导入用户时发生以下异常: " + String.join("; ", errors));
        }
        userRepository.saveAll(users);
    }


    // ========================= PUT 更新 =========================
    // 包括 更新用户信息、更新用户拥有的角色、重置用户密码
    // 还包括 封禁、解封、强制下线用户

    /**
     * 更新指定用户的基本信息
     * <p>
     * 该方法更新用户的基本信息，包括姓名和用户名。
     * </p>
     *
     * @param userId 要更新的用户ID
     * @param request 用户更新请求DTO，包含新的用户信息
     * @return 更新后的用户详细信息DTO
     * @throws NotFoundException 当指定ID的用户不存在时
     * @throws BadRequestException 当新用户名已被其他用户使用时
     */
    @Transactional
    public UserDetailResponse updateUserInfo(
        @NotNull Long userId,
        @NotNull UserUpdateRequest request
    ) throws BadRequestException {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("未找到ID为 " + userId + " 的用户")
        );
        if (request.username() != null &&
            userRepository.findByUsername(request.username()).map(User::getId).filter(
                id -> !id.equals(userId)).isPresent()) {
            throw new BadRequestException("用户名 " + request.username() + " 已存在");
        }
        user.setName(request.name());
        user.setUsername(request.username());
        userRepository.save(user);
        return UserDetailResponse.of(user);
    }

    /**
     * 更新指定用户的角色列表
     * <p>
     * 该方法完全替换用户的角色列表，首先清空用户当前的所有角色，
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
     * @param userId 要更新角色的用户ID
     * @param roleRequests 新的角色详情DTO列表
     * @return 更新后的用户角色详情DTO列表
     * @throws NotFoundException 当指定ID的用户不存在时
     * @throws CustomInvalidArgException 当角色配置信息无效时
     */
    @Transactional
    public List<UserRoleDetailsDTO> updateUserRoles(
        @NotNull Long userId,
        @NotNull List<UserRoleDetailsDTO> roleRequests
    ) throws NotFoundException, CustomInvalidArgException {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("未找到ID为 " + userId + " 的用户")
        );
        var roles = roleRequests.stream().map(UserRoleDetailsDTO::toRole).toList();
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        userRepository.save(user);
        return this.getUserRoles(userId);
    }

    /**
     * 重置指定用户的密码
     * <p>
     * 该方法为指定用户生成一个新的临时密码，密码格式为：<b>gz-MMdd-随机六位数字</b>。
     * 重置密码后，用户将被强制下线，用户必须使用新的初始密码重新登录并设置新密码后，才能重新启用账户。
     * </p>
     *
     * @param userId 要重置密码的用户ID
     * @return 密码重置响应DTO，包含用户信息和新生成的临时密码
     * @throws NotFoundException 当指定ID的用户不存在时
     */
    @Transactional
    public PasswordResetResponse resetPassword(@NotNull Long userId) throws NotFoundException {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("未找到ID为 " + userId + " 的用户")
        );

        // 默认密码为 gz-MMdd-随机六位数字
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMdd"));
        String randomDigits = String.format("%06d", new SecureRandom().nextInt(1000000));
        String newPassword = String.format("gz-%s-%s", date, randomDigits);
        user.setEncryptedPassword(PasswordEncryptUtils.encode(newPassword));

        // 重置密码后，将用户状态设置为未启用，需要用户重新设置密码后才能启用
        user.setEnabled(false);
        StpUtil.kickout(user.getId());
        StpUtil.disable(user.getId(), -1); // 永久封禁

        userRepository.save(user);
        return PasswordResetResponse.of(user, newPassword);
    }

    /**
     * 锁定指定用户
     * <p>
     * 该方法将指定用户设置为锁定状态，锁定后用户无法正常使用系统。
     * 用户将被强制下线，直到管理员解锁该用户。
     * </p>
     *
     * @param userId 要锁定的用户ID
     * @throws NotFoundException 当指定ID的用户不存在时
     */
    @Transactional
    public void lockUser(@NotNull Long userId) throws NotFoundException {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("未找到ID为 " + userId + " 的用户")
        );
        user.setLocked(true);
        userRepository.save(user);
        StpUtil.kickout(user.getId());
        StpUtil.disable(user.getId(), -1); // 永久封禁
    }

    /**
     * 解锁指定用户
     * <p>
     * 用户将被强制下线，并要求重新登录。
     * 用户是否能正常使用账户，取决于账户的enabled状态。
     * </p>
     *
     * @param userId 要解锁的用户ID
     * @throws NotFoundException 当指定ID的用户不存在时
     */
    @Transactional
    public void unlockUser(@NotNull Long userId) throws NotFoundException {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new NotFoundException("未找到ID为 " + userId + " 的用户")
        );
        user.setLocked(false);
        boolean isEnabled = user.isEnabled();
        userRepository.save(user);
        // unlock后, 根据用户的enabled状态决定是否解封
        StpUtil.kickout(user.getId());
        if (isEnabled) {
            StpUtil.untieDisable(user.getId());
        }
    }

    /**
     * 强制指定用户下线
     * <p>
     * 该方法强制指定用户从系统中下线，使其当前所有会话立即失效。
     * </p>
     *
     * @param userId 要强制下线的用户ID
     */
    public void kickoutUser(@NotNull Long userId) {
        StpUtil.kickout(userId);
    }


    // ========================= DELETE 删除 =========================

    /**
     * 批量删除用户
     * <p>
     * 该方法批量删除指定ID列表中的所有用户。
     * 如果用户ID不存在，不会抛出异常，会被静默忽略
     * </p>
     *
     * @param userIds 要删除的用户ID集合
     */
    @Transactional
    public void deleteUsers(@NotNull Collection<Long> userIds) {
        userIds.forEach(StpUtil::kickout);
        auditRepository.deleteByUser_IdIn(userIds);
        auditRepository.flush();
        userRepository.deleteAllByIdInBatch(userIds);
    }

}
