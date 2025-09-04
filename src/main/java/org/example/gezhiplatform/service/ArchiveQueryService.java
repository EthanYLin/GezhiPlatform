package org.example.gezhiplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;

/**
 * 档案查询服务
 *
 * <p><b>职责：</b></p>
 * 提供档案查询接口，根据用户权限返回过滤后的档案数据，屏蔽掉用户无权访问的字段。
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
public class ArchiveQueryService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ArchiveMetadataService archiveMetadataService;
    private final ArchiveAccessControlService archiveAccessControlService;
    private final AuditService auditService;

    private final ParseContext jsonParser = JsonPath.using(defaultConfiguration().setOptions(SUPPRESS_EXCEPTIONS));
    private final ObjectMapper objectMapper;

    public ArchiveQueryService(
        StudentRepository studentRepository,
        ArchiveMetadataService archiveMetadataService,
        ObjectMapper objectMapper,
        ArchiveAccessControlService archiveAccessControlService,
        AuditService auditService,
        UserRepository userRepository
    ) {
        this.studentRepository = studentRepository;
        this.archiveMetadataService = archiveMetadataService;
        this.objectMapper = objectMapper;
        this.archiveAccessControlService = archiveAccessControlService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * 查询指定学号学生的档案（基于权限过滤）
     * <p>
     * 根据当前用户的权限返回过滤后的学生档案数据。
     * 只有用户具备读取权限的字段才会在返回结果中包含，无权访问的字段将被移除。
     * </p>
     * <p>
     * 查询和权限过滤流程：
     * <ol>
     *   <li>根据学号查找学生档案数据</li>
     *   <li>获取当前用户对该学生的可读权限路径，并取补集得到无权访问的路径</li>
     *   <li>从档案JSON中删除无权访问的字段</li>
     *   <li>返回过滤后的档案JSON数据</li>
     * </ol>
     * </p>
     *
     * @param currentUserId 当前用户ID
     * @param stuNoForQuery 要查询的学生学号
     * @return 经过权限过滤的学生档案JSON字符串
     * @throws BadRequestException 当档案序列化失败时抛出
     * @throws NotFoundException   当学生不存在时抛出
     */
    @Transactional
    public String queryByStuNo(
        @NotNull Long currentUserId,
        @NotNull String stuNoForQuery
    ) throws BadRequestException {
        // 查询学生档案
        User user = userRepository.findById(currentUserId).orElseThrow(
            () -> new NotFoundException("当前操作用户不存在 (ID:" + currentUserId + ")")
        );
        Archive archive = studentRepository.findArchiveByStuNo(stuNoForQuery).orElseThrow(
            () -> new NotFoundException("要查询的学生(学号:" + stuNoForQuery + ")不存在")
        ).getArchive();

        // 将档案数据经由String转换为JaywayDocument
        String archiveJson;
        try {
            archiveJson = objectMapper.writeValueAsString(archive);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("无法序列化档案数据(追踪点:AQS01) :" + e.getMessage());
        }
        var archiveJayway = jsonParser.parse(archiveJson);

        // 获取用户具有的可读权限
        var allowedReadablePaths = archiveAccessControlService
            .getMergedPermissions(currentUserId, stuNoForQuery)
            .allowedJsonPaths().readableJsonPaths();
        var deniedReadablePaths = archiveMetadataService.getComplementSet(allowedReadablePaths);

        // 根据可读权限裁剪档案数据并返回
        deniedReadablePaths.forEach(archiveJayway::delete);

        // 在审计日志中记录
        var stuNo = archive.getStudent() == null ? "未知" : archive.getStudent().getStuNo();
        var stuName = archive.getStudent() == null ? "未知" : archive.getStudent().getStuName();
        auditService.log(
            user, AuditOperationType.ARCHIVE_QUERY,
            String.format("查询学生档案(学号:%s, 姓名: %s)", stuNo, stuName)
        );

        return archiveJayway.jsonString();
    }

}
