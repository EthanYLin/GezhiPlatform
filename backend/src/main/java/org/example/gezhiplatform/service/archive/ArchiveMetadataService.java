package org.example.gezhiplatform.service.archive;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.utils.JsonSchemaGenerateUtils;
import org.example.gezhiplatform.utils.XJsonPathAugmentUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 学生档案类元数据服务
 *
 * <p><b>职责：</b></p>
 * <ul>
 *   <li>该服务负责管理学生档案(Archive)类的JSON Schema元数据信息，用于支持前端表单的自动生成和字段权限控制。</li>
 *   <li>在应用启动时自动生成Archive类的JSON Schema</li>
 *   <li>为Schema中的每个字段添加JSONPath路径标识</li>
 * </ul>
 *
 * 生成的Schema包含完整的字段结构描述、验证规则、以及扩展的JSONPath信息，
 * 可直接用于React-JSON-Schema-Form等前端表单生成工具。
 *
 * <p><b>架构说明：</b></p>
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
@Service
public class ArchiveMetadataService {

    @Getter private final Map<String, XJsonPathAugmentUtils.FieldMeta> fieldMetadata = new HashMap<>();
    private final Set<String> allFieldPaths = new HashSet<>();
    @Getter private ObjectNode schema;

    /**
     * 初始化档案元数据服务
     * <p>
     * 在Spring容器启动完成后自动执行，负责生成Archive类的JSON Schema
     * 并进行JSONPath增强处理，初始化所有必要的元数据信息。
     * </p>
     * <p>
     * 初始化流程：
     * <ol>
     *   <li>使用JsonSchemaGenerateUtils生成Archive类的基础JSON Schema</li>
     *   <li>使用XJsonPathAugmentUtils为Schema添加JSONPath路径标识</li>
     *   <li>收集并存储所有字段的元数据信息</li>
     *   <li>建立完整的字段路径集合用于后续的集合运算</li>
     * </ol>
     * </p>
     */
    @PostConstruct
    public void init() {
        schema = JsonSchemaGenerateUtils.generateSchema(Archive.class);
        var allFields = XJsonPathAugmentUtils.annotate(schema);
        fieldMetadata.clear();
        fieldMetadata.putAll(allFields);
        allFieldPaths.clear();
        allFieldPaths.addAll(fieldMetadata.keySet());
    }

    /**
     * 获取字段路径的补集
     * 返回所有已知字段路径中不包含在输入路径集合中的路径。
     *
     * @param paths 输入的字段路径集合
     * @return 补集，即所有字段路径中不包含在输入集合中的路径
     */
    public Set<String> getComplementSet(Set<String> paths) {
        Set<String> complement = new HashSet<>(allFieldPaths);
        complement.removeAll(paths);
        return complement;
    }

    /**
     * 获取字段路径的交集
     * 返回所有已知字段路径中与输入路径集合的交集部分。
     *
     * @param paths 输入的字段路径集合
     * @return 交集，即所有已知字段路径中与输入集合共有的路径
     */
    public Set<String> getIntersectSet(Set<String> paths) {
        Set<String> intersect = new HashSet<>(allFieldPaths);
        intersect.retainAll(paths);
        return intersect;
    }


}
