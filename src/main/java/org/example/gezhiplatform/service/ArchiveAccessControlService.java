package org.example.gezhiplatform.service;

import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.permission.AllowedJsonPathsResponse;
import org.example.gezhiplatform.DTO.permission.ArchivePermissionDetails;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.permission.PermissionGroup;
import org.example.gezhiplatform.entity.user_role.Role;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 档案访问控制服务
 *
 * <p><b>职责：</b></p>
 * <ul>
 *   <li>基于用户所属的角色组和档案元字段定义，计算用户在某个学生档案上的可读/可写权限</li>
 *   <li>为档案查询服务和档案更新服务提供权限决策支持</li>
 * </ul>
 *
 * <p><b>架构说明：</b></p>
 * <p>
 * 档案查询服务（{@link ArchiveQueryService}）与档案更新服务（{@link ArchiveUpdateService}）
 * 在处理请求时，会调用访问控制服务（{@link ArchiveAccessControlService}）获取用户对于该学生的读写权限。<br/>
 * 访问控制服务在进行权限判断时，需要依赖：<br/>
 * - 档案元字段服务（{@link ArchiveMetadataService}），提供档案字段及类型信息；<br/>
 * - 权限组配置服务（{@link ArchivePermissionGroupService}），提供用户角色所在的权限组及其读写权限。<br/>
 * 最终，{@link ArchiveQueryService} 与 {@link ArchiveUpdateService}
 * 会根据 {@link ArchiveAccessControlService} 返回的权限信息，过滤或限制数据访问。
 * </p>
 */
@Service
public class ArchiveAccessControlService {


    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ArchivePermissionGroupService archivePermissionGroupService;

    public ArchiveAccessControlService(
        StudentRepository studentRepository,
        UserRepository userRepository,
        ArchivePermissionGroupService archivePermissionGroupService
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.archivePermissionGroupService = archivePermissionGroupService;
    }

    /**
     * 获取用户对指定学生档案的权限详情
     * <p>
     * 该方法用于计算并返回当前用户对指定学生档案的完整访问权限，
     * 包括：拥有的且可访问该学生的角色范围、拥有的且可访问该学生的权限组 以及 允许访问可读/可写的JSON Path。
     * </p>
     * <p>
     * 该方法在返回权限时会合并用户具有的所有角色以及权限组。
     * </p>
     * <p>
     * 权限计算流程：
     * <ol>
     *   <li>筛选用户所有角色中能够访问该学生的角色</li>
     *   <li>获取这些角色对应的权限组</li>
     *   <li>合并所有权限组的可读/可写JSON Path</li>
     *   <li>返回完整的权限详情信息</li>
     * </ol>
     * </p>
     * 
     * @param currentUserId 当前用户ID
     * @param stuNoForQuery 要查询的学生学号
     * @return 档案权限详情，包含角色范围、权限组和允许访问的JSON Path
     * @throws NotFoundException 当用户或学生不存在时抛出
     */
    @Transactional
    public ArchivePermissionDetails getMergedPermissions(
        @NotNull Long currentUserId,
        @NotNull String stuNoForQuery
    ) throws NotFoundException {
        Student student = studentRepository.findByStuNo(stuNoForQuery).orElseThrow(
            () -> new NotFoundException("要查询的学生(学号:" + stuNoForQuery + ")不存在")
        );
        User user = userRepository.findById(currentUserId).orElseThrow(
            () -> new NotFoundException("当前用户(ID:" + currentUserId + ")不存在")
        );

        // 获取该用户中, 能访问该学生的所有角色&角色类型
        // 例如用户(2027届年级组长、2027届1班班主任)访问270201, 只拥有年级组长的权限
        var grantedRoles = user.getRoles().stream().filter(role -> role.canAccessStudent(student)).collect(
            Collectors.toSet());
        var grantedRoleTypes = grantedRoles.stream().map(Role::getRoleType).collect(Collectors.toSet());
        // 获取包含该角色类型的所有权限组
        var ownedPermissionGroups = grantedRoleTypes.stream().map(
            archivePermissionGroupService::getPermissionGroupsByRoleType
        ).flatMap(Set::stream).collect(Collectors.toSet());
        // 合并所有权限组的可读可写路径
        var allReadablePaths = ownedPermissionGroups.stream().map(
            PermissionGroup::getAllowedReadableJsonPaths
        ).flatMap(Set::stream).collect(Collectors.toSet());
        var allWritablePaths = ownedPermissionGroups.stream().map(
            PermissionGroup::getAllowedWritableJsonPaths
        ).flatMap(Set::stream).collect(Collectors.toSet());

        return new ArchivePermissionDetails(
            grantedRoles.stream().map(Role::getRoleAndScope).toList(),
            ownedPermissionGroups.stream().map(PermissionGroup::getName).toList(),
            new AllowedJsonPathsResponse(allReadablePaths, allWritablePaths)
        );

    }


}
