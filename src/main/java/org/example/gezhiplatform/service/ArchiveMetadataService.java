package org.example.gezhiplatform.service;

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
 * <p>
 * 该服务负责管理学生档案(Archive)类的JSON Schema元数据信息，
 * 主要用于支持前端表单的自动生成和字段权限控制。
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *   <li>在应用启动时自动生成Archive类的JSON Schema</li>
 *   <li>为Schema中的每个字段添加JSONPath路径标识</li>
 * </ul>
 * </p>
 * <p>
 * 生成的Schema包含完整的字段结构描述、验证规则、以及扩展的JSONPath信息，
 * 可直接用于React-JSON-Schema-Form等前端表单生成工具。
 * </p>
 */
@Service
public class ArchiveMetadataService {

    private final Map<String, XJsonPathAugmentUtils.FieldMeta> fieldMetadata = new HashMap<>();
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


}
