package org.example.gezhiplatform.service.archive;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.audit.AuditService;
import org.example.gezhiplatform.service.metadata.ArchiveMetadataService;
import org.example.gezhiplatform.service.permission.ArchiveAccessControlService;
import org.example.gezhiplatform.service.permission.ArchivePermissionGroupService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * 档案查询服务
 *
 * <p><b>职责：</b></p>
 * 提供档案查询接口，根据用户权限返回过滤后的档案数据，屏蔽掉用户无权访问的字段。
 *
 * <p><b>架构说明：</b></p>
 * <p>
 * 档案查询（{@link ArchiveQueryService}）、档案导出（{@link ArchiveExportService}）
 * 与档案更新服务（{@link ArchiveUpdateService}）在处理请求时，会调用访问控制服务
 * （{@link ArchiveAccessControlService}）获取用户对于该学生的读写权限。
 * </p>
 * <p>
 * 访问控制服务（{@link ArchiveAccessControlService}）将根据用户的权限过滤档案数据：
 * <ul>
 *   <li>在<b>读取操作</b>中，只返回档案中用户有权限访问的字段。</li>
 *   <li>在<b>更新操作</b>中，去除请求体中用户不可写的字段数据。</li>
 * </ul>
 * </p>
 * <p>
 * 在进行权限判断时，依赖以下服务：
 * <ul>
 *   <li>档案元字段服务（{@link ArchiveMetadataService}）- 提供档案字段及类型信息。</li>
 *   <li>权限组配置服务（{@link ArchivePermissionGroupService}）- 提供用户角色所在的权限组及其读写权限。</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
@Service
public class ArchiveQueryService {

    private final ArchiveAccessControlService archiveAccessControlService;
    private final AuditService auditService;

    /**
     * 查询指定学号学生的档案（基于权限过滤）
     * <p>
     * 根据当前用户的权限返回过滤后的学生档案数据。
     * 只有用户具备读取权限的字段才会在返回结果中包含，无权访问的字段将被移除。
     * 查询操作会被记录到审计日志中。
     * </p>
     * <p>
     * 查询和权限过滤流程：
     * <ol>
     *   <li>创建用户-学生-档案上下文（{@link ArchiveAccessControlService.UserStudentArchive}），
     *       该上下文会自动加载用户信息、学生信息和完整档案数据</li>
     *   <li>调用 {@code context.getReadableArchive()} 获取经过权限过滤的档案数据（Jayway JSON格式），
     *       该方法会根据用户的角色权限，自动移除用户无权访问的字段</li>
     *   <li>记录查询操作到审计日志，包含操作用户、目标学生学号和姓名</li>
     *   <li>返回过滤后的档案JSON字符串</li>
     * </ol>
     * </p>
     *
     * @param currentUserId 当前用户ID
     * @param stuNoForQuery 要查询的学生学号
     * @return 经过权限过滤的学生档案JSON字符串
     * @throws BadRequestException 当档案序列化失败时抛出
     * @throws NotFoundException   当用户不存在或学生不存在时抛出
     */
    @Transactional
    public String queryByStuNo(
        @NotNull Long currentUserId,
        @NotNull String stuNoForQuery
    ) throws BadRequestException {

        // 查询学生档案
        var context = archiveAccessControlService.new UserStudentArchive(currentUserId, stuNoForQuery);
        var archiveJayway = context.getReadableArchive();

        // 在审计日志中记录
        auditService.log(
            context.getUser(), AuditOperationType.ARCHIVE_QUERY,
            String.format("查询学生档案(学号:%s, 姓名: %s)", context.getStudent().getStuNo(), context.getStudent().getStuName())
        );

        return archiveJayway.jsonString();
    }

}
