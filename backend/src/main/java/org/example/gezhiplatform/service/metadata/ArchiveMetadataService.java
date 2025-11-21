package org.example.gezhiplatform.service.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.example.gezhiplatform.DTO.archive.FieldMetadata;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.archive.family_part.FamilyPart;
import org.example.gezhiplatform.entity.archive.health_part.HealthPart;
import org.example.gezhiplatform.service.permission.ArchiveAccessControlService;
import org.example.gezhiplatform.service.permission.ArchivePermissionGroupService;
import org.example.gezhiplatform.utils.FieldMetadataGenerateUtils;
import org.example.gezhiplatform.utils.JsonSchemaGenerateUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 档案元数据服务
 *
 * <p><b>职责：</b></p>
 * <ul>
 *   <li>管理学生档案(Archive)类的JSON Schema元数据信息</li>
 *   <li>提供字段元数据查询功能，包括字段路径、类型、权限等信息</li>
 *   <li>提供类型安全的数组字段访问器（ArrayGetter），用于档案数组字段的更新操作</li>
 * </ul>
 *
 * <p><b>元数据内容：</b></p>
 * <ul>
 *   <li><b>schema</b>：Archive类的完整JSON Schema，包含字段结构、验证规则和JSONPath扩展信息</li>
 *   <li><b>fields</b>：字段路径到元数据的映射表，包含每个字段的详细信息（编辑权限、标题链、路径链等）</li>
 *   <li><b>arrayGetters</b>：档案中数组字段的路径以及对应的访问器</li>
 * </ul>
 *
 * <p><b>架构说明：</b></p>
 * <p>
 * 访问控制服务（{@link ArchiveAccessControlService}）在进行权限判断时依赖本服务：
 * <ul>
 *   <li>在<b>读取操作</b>中，通过字段元数据判断用户有权访问的字段</li>
 *   <li>在<b>更新操作</b>中，通过字段元数据过滤用户不可写的字段数据</li>
 *   <li>在<b>数组更新</b>中，通过 ArrayGetter 提供类型安全的列表访问</li>
 *   <li>权限组配置服务（{@link ArchivePermissionGroupService}）使用本服务验证和规范化JSONPath权限配置</li>
 * </ul>
 * </p>
 *
 * @see ArchiveAccessControlService
 * @see ArchivePermissionGroupService
 * @see FieldMetadata
 * @see ArrayGetter
 */
@Service
public class ArchiveMetadataService {

    /**
     * 数组字段及其访问器
     */
    public final Map<String, ArrayGetter<?>> arrayGetters = Map.of(
        "$.familyPart.otherRelatives", new ArrayGetter<>(
            a -> Optional.of(a).map(Archive::getFamilyPart).map(FamilyPart::getOtherRelatives).orElse(null)
        ),
        "$.healthPart.mentalCondition", new ArrayGetter<>(
            a -> Optional.of(a).map(Archive::getHealthPart).map(HealthPart::getMentalCondition).orElse(null)
        ),
        "$.healthPart.physicalCondition", new ArrayGetter<>(
            a -> Optional.of(a).map(Archive::getHealthPart).map(HealthPart::getPhysicalCondition).orElse(null)
        )
    );

    /**
     * 字段路径到元数据的映射表
     * <p>
     * 键为JSONPath路径（如 $.personalPart.gender），值为{@link FieldMetadata}对象，
     * 包含字段的编辑权限、标题链、路径链、是否为数组等完整元数据信息。
     * </p>
     */
    @Getter private final Map<String, FieldMetadata> fields = new HashMap<>();
    /**
     * Archive类的完整JSON Schema
     * <p>
     * 包含字段结构描述、验证规则、以及扩展的'x-jsonpath'信息，
     * 可直接用于前端表单生成工具（如React-JSON-Schema-Form）。
     * </p>
     */
    @Getter private ObjectNode schema;

    /**
     * 所有数组类型字段的路径集合
     */
    public Set<String> getArrayFields() {
        return arrayGetters.keySet();
    }

    /**
     * 初始化档案元数据
     * <p>
     * 在Spring容器启动完成后自动执行，负责生成Archive类的JSON Schema
     * 并提取所有字段的元数据信息。
     * </p>
     * <p>
     * 初始化流程：
     * <ol>
     *   <li>使用{@link JsonSchemaGenerateUtils#generateSchema}生成Archive类的完整JSON Schema</li>
     *   <li>使用{@link FieldMetadataGenerateUtils#generate}提取所有字段的元数据信息并向schema中添加x-json-path</li>
     *   <li>验证数组字段配置的一致性，确保arrayGetters中配置的字段与Schema中扫描到的数组字段完全匹配</li>
     * </ol>
     * </p>
     *
     * @throws RuntimeException 当数组字段配置不一致时抛出异常(AMS01)
     */
    @PostConstruct
    public void init() {
        this.schema = JsonSchemaGenerateUtils.generateSchema(Archive.class);
        this.fields.clear();
        this.fields.putAll(FieldMetadataGenerateUtils.generate(schema));

        // 验证数组字段配置一致性
        Set<String> arrayFields = fields.entrySet().stream()
                                        .filter(e -> e.getValue().isArray())
                                        .map(Map.Entry::getKey).collect(Collectors.toSet());
        if (!arrayFields.equals(this.arrayGetters.keySet())) {
            throw new RuntimeException(
                "档案元数据服务初始化失败(AMS01): 数组字段配置不匹配。\n" +
                "扫描到的数组字段: " + arrayFields + "\n" +
                "提供了访问器的数组字段: " + this.arrayGetters.keySet()
            );
        }
    }

    public record ArrayGetter<T extends Identifiable>(
        Function<@NotNull Archive, @Nullable List<T>> getter
    ) {
        @Nullable public List<T> apply(@NotNull Archive archive) {
            return getter.apply(archive);
        }
    }

    /**
     * 获取字段路径集合的补集
     *
     * @param paths 输入的字段路径集合
     * @return 补集，即所有字段路径中不在输入集合中的路径
     */
    public HashSet<String> getComplementSet(Set<String> paths) {
        HashSet<String> complement = new HashSet<>(fields.keySet());
        complement.removeAll(paths);
        return complement;
    }

    /**
     * 获取字段路径集合的交集
     *
     * @param paths 输入的字段路径集合
     * @return 交集，即所有已知字段路径中与输入集合共有的路径
     */
    public HashSet<String> getIntersectSet(Set<String> paths) {
        HashSet<String> intersect = new HashSet<>(fields.keySet());
        intersect.retainAll(paths);
        return intersect;
    }


}
