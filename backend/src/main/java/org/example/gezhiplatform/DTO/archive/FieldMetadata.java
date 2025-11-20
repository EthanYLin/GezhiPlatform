package org.example.gezhiplatform.DTO.archive;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 字段元数据记录类
 * <p>
 * 用于存储字段的附加信息，包含字段的权限控制、标题和路径等信息。
 * </p>
 *
 * @param allowEdit          是否允许编辑此字段，基于Schema中的readOnly属性判断
 * @param titles             字段所有层级的标题列表，从根到叶子节点。例如: ["家庭成员","父亲","手机号码"]
 * @param paths              字段所有层级的路径列表，从根到叶子节点。例如: ["familyPart","father","mobile"]
 * @param isArray            是否为数组类型字段
 * @param insideArray        是否在数组中
 * @param arrayEntryJsonPath 如果该字段在数组中，标注入口数组元素的JSONPath路径。例如: $.familyPart.otherRelatives
 */
public record FieldMetadata(
    boolean allowEdit,
    @NotNull List<String> titles,
    @NotNull List<String> paths,
    boolean isArray,
    boolean insideArray,
    @Nullable String arrayEntryJsonPath
) {}
