package org.example.gezhiplatform.service.archive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;

/**
 * 档案更新服务
 *
 * <p><b>职责：</b></p>
 * 提供档案更新接口，根据用户权限判断是否允许字段级别的更新，仅对用户有权修改的字段执行持久化。
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
public class ArchiveUpdateService {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ParseContext jaywayParser = JsonPath.using(defaultConfiguration().setOptions(SUPPRESS_EXCEPTIONS));
    private final ArchiveAccessControlService archiveAccessControlService;
    private final StudentRepository studentRepository;

    public ArchiveUpdateService(
        ObjectMapper objectMapper,
        Validator validator,
        ArchiveAccessControlService archiveAccessControlService,
        StudentRepository studentRepository
    ) {
        this.objectMapper = objectMapper;
        this.archiveAccessControlService = archiveAccessControlService;
        this.validator = validator;
        this.studentRepository = studentRepository;
    }


    /**
     * 更新学生档案
     * <p>
     * 根据提供的JSON数据更新指定学生的档案信息。该方法会执行完整的权限验证、数据过滤、
     * 更新应用、数据校验和持久化流程，确保档案更新操作的安全性和数据完整性。
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
     * <p>
     * <b>权限控制：</b>只有用户具备可写权限的字段才会被实际更新，
     * 无权修改的字段将在过滤阶段被移除，确保数据安全。
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
        // 对请求体进行过滤
        String filteredJson = filterUpdateData(jsonForUpdate, context);
        // 应用更新
        try {
            objectMapper.readerForUpdating(context.archive()).readValue(filteredJson);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("无法应用档案更新数据(AUS02): " + e.getMessage());
        }
        // 校验
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

    /**
     * 根据用户权限过滤更新数据
     * <p>
     * 根据当前用户对指定学生的访问权限来过滤档案更新数据。
     * 它会获取用户的可写权限路径，计算出不允许修改的路径，然后从更新JSON中删除这些路径对应的数据。
     * </p>
     *
     * @param json    原始的更新数据JSON字符串
     * @param context 用户-学生-档案上下文，包含当前操作的用户和学生信息
     * @return 经过权限过滤的JSON字符串，只包含用户有权修改的字段
     * @throws BadRequestException 当JSON解析失败时抛出
     */
    private @NotNull String filterUpdateData(
        @NotNull String json, @NotNull ArchiveAccessControlService.UserStudentArchive context
    ) throws BadRequestException {

        // 根据用户的不可写权限，裁剪请求体并返回
        try {
            var jaywayUpdateData = jaywayParser.parse(json);
            context.deniedWritableJsonPaths().forEach(jaywayUpdateData::delete);
            return jaywayUpdateData.jsonString();
        } catch (Exception e) {
            throw new BadRequestException("无法解析档案更新数据(AUS01): " + e.getMessage());
        }
    }

}