package org.example.gezhiplatform.service.archive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.example.gezhiplatform.DTO.archive.ArrayPermission;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.archive.ValidationExpr;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.service.metadata.ArchiveMetadataService;
import org.example.gezhiplatform.service.metadata.Identifiable;
import org.example.gezhiplatform.service.permission.ArchiveAccessControlService;
import org.example.gezhiplatform.service.permission.ArchivePermissionGroupService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

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
    private final ArchiveMetadataService archiveMetadataService;
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
     * @throws RuntimeException 其他运行时异常，如配置错误等
     */
    @Transactional
    public void updateArchive(
        @NotNull String jsonForUpdate, @NotNull Long userId, @NotNull String stuNo
    ) throws RuntimeException {
        // 获取用户、学生、档案
        var context = archiveAccessControlService.new UserStudentArchive(userId, stuNo);
        Map<String, Object> prevArchiveSnapshot = snapshotArchive(context.getArchive());

        // 校验1: 对请求体进行权限过滤 (WriteableJsonPaths)并应用更新
        String filteredJson = context.getWritableUpdateData(jsonForUpdate).jsonString();
        try {
            objectMapper.readerForUpdating(context.getArchive()).readValue(filteredJson);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("无法应用档案更新数据(AUS02): " + e.getMessage());
        }

        // 校验2: 处理数组的增加、删除、修改权限 (Add/Edit/DeleteArrayJsonPaths)
        try {
            Archive updateArchive = objectMapper.readValue(jsonForUpdate, Archive.class);
            archiveMetadataService.arrayGetters.forEach(
                (arrayPath, arrayGetter) -> {
                    ArrayPermission perm = context.testArrayPermission(arrayPath);
                    updateArchiveArray(context.getArchive(), updateArchive, arrayGetter, perm);
                }
            );
        } catch (JsonProcessingException e) {
            throw new BadRequestException("无法解析档案更新数据(AUS01): " + e.getMessage());
        }

        // 校验3: SpEL校验
        Map<String, Object> curArchiveSnapshot = snapshotArchive(context.getArchive());
        validateWithSpel(prevArchiveSnapshot, curArchiveSnapshot, context.permissionDetails().validationSpELs());

        // 校验4: 通过 Bean Validation 进行数据校验
        var violations = validator
            .validate(context.getArchive())
            .stream()
            .map(ConstraintViolation::getMessage)
            .toList();
        if (!violations.isEmpty())
            throw new BadRequestException("档案更新数据校验失败: " + String.join(", ", violations));

        // 保存
        context.getStudent().setArchive(context.getArchive());
        studentRepository.save(context.getStudent());
    }

    /**
     * 更新档案中的数组字段
     *
     * @param originalArchive 原始档案实例（包含待更新的列表）
     * @param updateArchive 更新数据档案（包含新的列表数据）
     * @param arrayGetter 数组获取器（用于在档案中获取指定数组字段）
     * @param permission 数组权限配置（控制 canAdd/canEdit/canDelete）
     * @param <T> 数组元素类型，必须实现 {@link Identifiable} 接口
     */
    private <T extends Identifiable> void updateArchiveArray(
        Archive originalArchive,
        Archive updateArchive,
        ArchiveMetadataService.ArrayGetter<T> arrayGetter,
        ArrayPermission permission
    ) {
        List<T> originalList = arrayGetter.apply(originalArchive);
        List<T> updateList = arrayGetter.apply(updateArchive);
        mergeList(originalList, updateList, permission.canAdd(), permission.canEdit(), permission.canDelete());
    }

    /**
     * 合并列表内容（原地修改，基于权限控制）
     *
     * <p><b>实现逻辑：</b></p>
     * <ol>
     *   <li><b>编辑</b>（canEdit=true）：遍历原列表，使用 Jackson readerForUpdating 更新匹配 ID 的元素</li>
     *   <li><b>删除</b>（canDelete=true）：移除原列表中 ID 不在更新数据中的元素</li>
     *   <li><b>新增</b>（canAdd=true）：将更新数据中 ID 为 null 的新元素添加到列表末尾</li>
     * </ol>
     *
     * @param originalList 原始列表（会被原地修改）
     * @param updateData 更新数据集合
     * @param canAdd 是否允许添加新元素（ID 为 null 的元素）
     * @param canEdit 是否允许编辑现有元素（通过 ID 匹配）
     * @param canDelete 是否允许删除元素（原列表中存在但更新数据中不存在的元素）
     * @param <T> 列表元素类型，必须实现 {@link Identifiable} 接口
     * @throws RuntimeException 当 JSON 序列化/反序列化失败时抛出
     */
    private  <T extends Identifiable> void mergeList(
        @Nullable List<T> originalList,
        @Nullable Collection<T> updateData,
        boolean canAdd, boolean canEdit, boolean canDelete
    ) throws RuntimeException {

        // 若原数据为空/新数据为空，说明没有更新，直接返回
        if (originalList == null || updateData == null) {
            return;
        }

        // 1. 构建更新数据索引
        Map<Object, T> updateItemsById = updateData
            .stream()
            .filter(item -> item.getId() != null)
            .collect(toMap(Identifiable::getId, item -> item));

        // 2. 如果允许编辑，原地更新现有元素
        if (canEdit) {
            for (T originalItem : originalList) {
                Object id = originalItem.getId();
                // 该元素没有被更新，跳过
                if (id == null || !updateItemsById.containsKey(id)) {
                    continue;
                }
                // 将更新数据应用到原对象上（部分字段更新）
                try {
                    T updateItem = updateItemsById.get(id);
                    String updateItemJson = objectMapper.writeValueAsString(updateItem);
                    objectMapper.readerForUpdating(originalItem).readValue(updateItemJson);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("无法应用更新到 id=" + id + " 的对象: " + e.getMessage(), e);
                }
            }
        }

        // 3. 如果允许删除，删除不在更新数据中的元素
        if (canDelete) {
            Set<Object> updateIds = updateItemsById.keySet();
            originalList.removeIf(item -> item.getId() != null && !updateIds.contains(item.getId()));
        }

        // 4. 如果允许新增，将新元素(id 为 null)添加到最后
        if (canAdd) {
            updateData.stream().filter(item -> item.getId() == null).forEach(originalList::add);
        }

    }

    /**
     * 转换档案对象为 Map 结构，便于 SpEL 使用键访问数据快照。
     */
    private Map<String, Object> snapshotArchive(@NotNull Archive archive) throws RuntimeException {
        try {
            return objectMapper.convertValue(archive, new TypeReference<>() {});
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无法生成档案快照(AUS03): " + e.getMessage(), e);
        }
    }

    /**
     * 使用 SpEL 对提交前后的档案快照执行权限组自定义校验。
     *
     * @throws BadRequestException 如果有任意校验不通过则抛出异常, 包含所有失败的校验信息。
     */
    private void validateWithSpel(
        @NotNull Map<String, Object> prevArchive,
        @NotNull Map<String, Object> curArchive,
        @NotNull Set<ValidationExpr> validations
    ) throws BadRequestException{
        if (validations.isEmpty()) return;

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        evaluationContext.addPropertyAccessor(new MapAccessor());
        evaluationContext.setVariable("prev", prevArchive);
        evaluationContext.setVariable("cur", curArchive);

        List<String> failedMessages = validations.stream()
            .filter(validation -> !evaluateSpel(validation.getSpelExpr(), parser, evaluationContext))
            .map(ValidationExpr::getMessage)
            .toList();

        if (!failedMessages.isEmpty()) {
            throw new BadRequestException("档案更新数据未通过规则校验: " + String.join("; ", failedMessages));
        }
    }

    /**
     * 评估单条 SpEL 表达式，异常时按返回 false 处理。
     */
    private boolean evaluateSpel(
        @NotNull String expression,
        ExpressionParser parser,
        StandardEvaluationContext evaluationContext
    ) {
        try {
            Boolean result = parser.parseExpression(expression).getValue(evaluationContext, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (RuntimeException e) {
            return false;
        }
    }


}