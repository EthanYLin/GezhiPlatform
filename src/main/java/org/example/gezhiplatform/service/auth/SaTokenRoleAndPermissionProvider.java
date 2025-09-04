package org.example.gezhiplatform.service.auth;

import cn.dev33.satoken.stp.StpInterface;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.entity.role.Role;
import org.example.gezhiplatform.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaTokenRoleAndPermissionProvider implements StpInterface {

    private final UserRepository userRepository;

    public SaTokenRoleAndPermissionProvider(UserRepository userRepository) {this.userRepository = userRepository;}

    /**
     * 获取某个登录主体的权限列表
     * 【暂不实现，目前只实现角色列表的获取 {@link SaTokenRoleAndPermissionProvider#getRoleList(Object, String)}】
     * @param loginId 登录主体ID
     * @param loginType 登录类型
     * @return 权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    /**
     * 获取某个登录主体的角色列表
     * @param loginId 登录主体ID
     * @param loginType 登录类型
     * @return 角色列表(只包括角色类型的枚举标识符, 不包括角色范围, 如 "SUPER_ADMIN", "STUDENT_USER" 等) {@see RoleType}
     */
    @Transactional
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) return List.of();
        try {
            Long userId = Long.parseLong(loginId.toString());
            return userRepository.findById(userId).map(
                user -> user.getRoles().stream().map(Role::getRoleType).map(Enum::name).toList()
            ).orElse(List.of());
        } catch (NumberFormatException e) {
            return List.of();
        }
    }
}
