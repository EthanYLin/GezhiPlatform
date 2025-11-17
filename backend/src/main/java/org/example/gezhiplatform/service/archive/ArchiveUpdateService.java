package org.example.gezhiplatform.service.archive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * 档案更新服务
 *
 * <p><b>职责：</b></p>
 * 提供档案更新接口，根据用户权限判断是否允许字段级别的更新，仅对用户有权修改的字段执行持久化。
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
public class ArchiveUpdateService {

    private final ArchiveAccessControlService archiveAccessControlService;
    private final StudentRepository studentRepository;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * 更新学生档案
     * <p>
     * 根据提供的JSON数据更新指定学生的档案信息。该方法会执行权限检查，去除用户无权修改的字段。
     * </p>
     * <p>
     * 更新流程：
     * <ol>
     *   <li>获取并验证用户、学生和档案的权限关系</li>
     *   <li>根据用户权限过滤更新数据，移除无权修改的字段</li>
     *   <li>将过滤后的JSON数据应用到档案实体</li>
     *   <li>对更新后的档案进行数据校验</li>
     *   <li>持久化更新后的学生实体（包含档案）</li>
     * </ol>
     * </p>
     *
     * @param jsonForUpdate 包含更新数据的JSON字符串
     * @param userId        当前操作用户的ID
     * @param stuNo         要更新档案的学生学号
     * @throws BadRequestException 当JSON解析失败、权限不足、数据校验失败或更新应用失败时抛出
     */
    @Transactional
    public void updateArchive(
        @NotNull String jsonForUpdate, @NotNull Long userId, @NotNull String stuNo
    ) throws BadRequestException {
        // 获取用户、学生、档案
        var context = archiveAccessControlService.new UserStudentArchive(userId, stuNo);
        // 对请求体进行权限过滤
        String filteredJson = context.getWritableUpdateData(jsonForUpdate).jsonString();
        // 应用更新
        try {
            objectMapper.readerForUpdating(context.archive()).readValue(filteredJson);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("无法应用档案更新数据(AUS02): " + e.getMessage());
        }
        // 检查不合法的数据
        var violations = validator
            .validate(context.archive())
            .stream()
            .map(ConstraintViolation::getMessage)
            .toList();
        if (!violations.isEmpty())
            throw new BadRequestException("档案更新数据校验失败: " + String.join(", ", violations));
        // 保存
        studentRepository.save(context.student());
    }

}