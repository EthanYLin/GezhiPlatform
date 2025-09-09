package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckDisable;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.gezhiplatform.service.archive.ArchiveMetadataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 档案类字段元数据控制器
 * 提供学生档案类的JSON Schema元数据
 */
@SaCheckDisable
@RestController
@RequestMapping("/archive/metadata")
@Tag(name = "档案类字段元数据查询")
public class ArchiveMetadataController {

    private final ArchiveMetadataService archiveMetadataService;

    public ArchiveMetadataController(ArchiveMetadataService archiveMetadataService) {
        this.archiveMetadataService = archiveMetadataService;
    }

    /**
     * 获取档案类的JSON Schema元数据
     * <p>
     * 返回学生档案(Archive)类的完整JSON Schema，包含所有字段的结构描述、
     * 验证规则以及扩展的JSONPath路径信息。
     * </p>
     * <p>
     * Schema中包含的扩展信息：
     * <ul>
     *   <li>x-jsonpath：每个字段的JSONPath路径标识</li>
     *   <li>title：字段的显示标题（支持@JsonTitle注解）</li>
     *   <li>validation：基于Jakarta Validation注解的验证规则</li>
     * </ul>
     * </p>
     *
     * @return 包含完整Schema信息的ObjectNode对象
     */
    @GetMapping
    @Operation(summary = "获取档案类的JSON Schema")
    public ObjectNode getArchiveMetadata() {
        return archiveMetadataService.getSchema();
    }
}
