package org.example.gezhiplatform.service.archive;

import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.archive.AllowedJsonPathsResponse;
import org.example.gezhiplatform.DTO.archive.ArchivePermissionDetails;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.archive.PermissionGroup;
import org.example.gezhiplatform.entity.role.Role;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;
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

    private final ArchivePermissionGroupService archivePermissionGroupService;
    private final ArchiveMetadataService archiveMetadataService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public ArchiveAccessControlService(
        ArchivePermissionGroupService archivePermissionGroupService,
        ArchiveMetadataService archiveMetadataService,
        UserRepository userRepository,
        StudentRepository studentRepository
    ) {
        this.archivePermissionGroupService = archivePermissionGroupService;
        this.archiveMetadataService = archiveMetadataService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * 用户-学生-档案 上下文
     * <p>
     * 封装了用户、学生和档案三个实体的组合，用于在档案操作中传递相关联的实体信息。
     * 通过该上下文可以获取用户对于该档案的权限，包括可写路径、可读路径等。
     * </p>
     *
     */
    @Transactional
    public class UserStudentArchive {

        private final User user;
        private final Student student;
        private final Archive archive;
        private final ArchivePermissionDetails permissionDetails;

        /**
         * 构造用户-学生-档案上下文
         * <p>
         * 构造时：
         * <ol>
         *   <li>验证用户ID对应的用户是否存在</li>
         *   <li>验证学号对应的学生是否存在</li>
         *   <li>检查用户的角色是否允许访问该学生</li>
         *   <li>验证学生是否拥有档案数据</li>
         *   <li>解除档案的Hibernate代理以确保数据可访问</li>
         * </ol>
         * </p>
         *
         * @param userId 用户ID
         * @param stuNo  学生学号
         * @throws NotFoundException   当用户不存在、学生不存在或学生无档案时抛出
         * @throws BadRequestException 当用户无权访问指定学生时抛出
         */
        public UserStudentArchive(
            @NotNull Long userId, @NotNull String stuNo
        ) throws BadRequestException {

            // 获取当前操作的用户及学生
            this.user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("当前操作用户不存在 (ID:" + userId + ")")
            );
            this.student = studentRepository.findByStuNo(stuNo).orElseThrow(
                () -> new BadRequestException("当前用户无权访问该学生(学号:" + stuNo + ")档案")
            );

            // 检查该用户是否能够访问该学生
            if (user.getRoles().stream().noneMatch(role -> role.canAccessStudent(student))) {
                throw new BadRequestException("当前用户无权访问该学生(学号:" + stuNo + ")档案");
            }

            // 查询该学生的档案并对档案解除代理
            Archive archive = Optional.ofNullable(student.getArchive()).orElseThrow(
                () -> new NotFoundException("要查询的学生(学号:" + stuNo + ")尚无档案")
            );
            archive = Hibernate.unproxy(archive, Archive.class);
            this.archive = archive;

            // 计算用户对该学生档案的权限详情
            this.permissionDetails = this.calculatePermissions();
        }

        public User user() {return user;}

        public Student student() {return student;}

        public Archive archive() {return archive;}

        public ArchivePermissionDetails permissionDetails() {return permissionDetails;}

        public Set<String> deniedReadableJsonPaths() {
            return archiveMetadataService.getComplementSet(permissionDetails.allowedJsonPaths().readableJsonPaths());
        }

        public Set<String> deniedWritableJsonPaths() {
            return archiveMetadataService.getComplementSet(permissionDetails.allowedJsonPaths().writableJsonPaths());
        }

        /**
         * 计算上下文中的用户对学生的权限详情
         * 包括：拥有的且可访问该学生的角色范围、拥有的且可访问该学生的权限组 以及 允许访问可读/可写的JSON Path。
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
         * @return 档案权限详情，包含角色范围、权限组和允许访问的JSON Path
         */
        public ArchivePermissionDetails calculatePermissions() throws NotFoundException {
            // 获取该用户中, 能访问该学生的所有角色&角色类型
            // 例如用户(2027届年级组长、2027届1班班主任)访问270201, 只拥有年级组长的权限
            var grantedRoles = user.getRoles().stream()
                                   .filter(role -> role.canAccessStudent(student))
                                   .collect(Collectors.toSet());
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

}
