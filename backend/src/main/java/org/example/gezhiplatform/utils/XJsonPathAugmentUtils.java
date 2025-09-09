package org.example.gezhiplatform.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON Schema JSONPath 增强工具类
 * <p>
 * 该工具类用于为生成的JSON Schema添加自定义的JSONPath路径标识，
 * 在JSON Schema的基础上，在其中添加'x-jsonpath'扩展属性，用于标识字段的JSONPath路径。
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *   <li>为Schema中的每个字段添加'x-jsonpath'扩展属性，标识字段的JSONPath路径</li>
 *   <li>收集字段的元数据信息，如是否允许编辑等权限控制信息</li>
 *   <li>支持嵌套对象和数组的路径标识，数组元素使用'[*]'通配符</li>
 * </ul>
 * </p>
 */
public class XJsonPathAugmentUtils {

    /**
     * 字段元数据记录类
     * <p>
     * 用于存储字段的附加信息，包含字段的权限控制、标题和路径等信息。
     * </p>
     *
     * @param allowEdit 是否允许编辑此字段，基于Schema中的readOnly属性判断
     * @param titles    字段所有层级的标题列表，从根到叶子节点
     * @param paths     字段所有层级的路径列表，从根到叶子节点
     * @param insideArray 是否在数组中(目前数组中的元素不能拥有单独的编辑权限)
     * @param arrayEntryJsonPath 如果该字段在数组中，标注入口数组元素的JSONPath路径
     */
    public record FieldMeta(
        boolean allowEdit,
        @NotNull List<String> titles,
        @NotNull List<String> paths,
        boolean insideArray,
        @Nullable String arrayEntryJsonPath
    ) {}

    private record RecursionContext(
        String jsonPath,
        List<String> titles,
        List<String> paths,
        String arrayEntryJsonPath,
        Map<String, FieldMeta> fieldMetadata
    ) {}

    /**
     * 为JSON Schema添加JSONPath注解和字段元数据
     * <p>
     * 遍历整个Schema树结构，为每个字段添加'x-jsonpath'扩展属性，
     * 同时收集字段的元数据信息并返回完整的字段路径映射。
     * </p>
     * <p>
     * 处理逻辑：
     * <ul>
     *   <li>从根路径'$'开始递归遍历Schema结构</li>
     *   <li>为每个字段生成唯一的JSONPath路径标识</li>
     *   <li>提取字段的readOnly属性来确定编辑权限</li>
     *   <li>支持对象属性和数组元素的路径生成</li>
     * </ul>
     * </p>
     *
     * @param root JSON Schema的根节点，必须是ObjectNode类型
     * @return 字段路径到元数据的映射表，如果根节点为null则返回空映射
     */
    public static Map<String, FieldMeta> annotate(ObjectNode root) {
        if (root == null) return Map.of();
        Map<String, FieldMeta> fieldMetadata = new HashMap<>();
        annotateUnder(root, new RecursionContext("$", new ArrayList<>(), new ArrayList<>(), null, fieldMetadata));
        return fieldMetadata;
    }

    /**
     * 递归为Schema节点添加JSONPath注解
     * <p>
     * 根据节点类型（object或array）递归处理Schema结构，
     * 为每个可访问的字段添加JSONPath路径标识。
     * </p>
     * <p>
     * 处理策略：
     * <ul>
     *   <li>object类型：遍历properties下的所有属性，为每个属性生成路径并递归处理</li>
     *   <li>array类型：处理items节点，使用'[*]'表示数组元素路径</li>
     *   <li>其他类型：不进行特殊处理</li>
     * </ul>
     * </p>
     *
     * @param node    当前处理的Schema节点
     * @param context 递归上下文，包含当前的路径信息和元数据收集器
     */
    private static void annotateUnder(ObjectNode node, RecursionContext context) {
        if (node == null) return;

        // object：对子属性写 x-jsonpath，并递归
        if (hasType(node, "object")) {
            ObjectNode props = obj(node.get("properties"));
            if (props != null) {
                for (var e : props.properties()) {
                    String key = e.getKey();
                    ObjectNode child = obj(e.getValue());
                    if (child == null) continue;

                    // 为子节点构造新的context
                    var childContext = new RecursionContext(
                        context.jsonPath + "." + key,
                        new ArrayList<>(context.titles),
                        new ArrayList<>(context.paths),
                        context.arrayEntryJsonPath,
                        context.fieldMetadata
                    );
                    childContext.titles.add(child.has("title") ? child.get("title").asText() : key);
                    childContext.paths.add(key);

                    addXJsonPathAndFieldMeta(child, childContext);
                    annotateUnder(child, childContext);
                }
            }
        }

        // array：仅处理单一 items；元素路径用 [*]
        if (hasType(node, "array")) {
            ObjectNode items = obj(node.get("items"));
            if (items != null) {
                var itemsContext = new RecursionContext(
                    context.jsonPath + "[*]",
                    new ArrayList<>(context.titles),
                    new ArrayList<>(context.paths),
                    context.arrayEntryJsonPath != null ? context.arrayEntryJsonPath : context.jsonPath,
                    context.fieldMetadata
                );
                annotateUnder(items, itemsContext);
            }
        }
    }

    /**
     * 为Schema节点添加JSONPath属性和字段元数据
     * <p>
     * 在Schema节点中添加'x-jsonpath'扩展属性，同时根据readOnly属性
     * 确定字段的编辑权限并存储到元数据映射中。
     * </p>
     *
     * @param node    要添加属性的Schema节点
     * @param context 递归上下文，包含当前的路径信息和元数据收集器
     */
    private static void addXJsonPathAndFieldMeta(ObjectNode node, RecursionContext context) {
        boolean allowEdit = node.get("readOnly") == null || !node.get("readOnly").asBoolean(false);
        node.put("x-jsonpath", context.jsonPath);
        context.fieldMetadata.put(
            context.jsonPath,
            new FieldMeta(
                allowEdit,
                new ArrayList<>(context.titles),
                new ArrayList<>(context.paths),
                context.arrayEntryJsonPath != null,
                context.arrayEntryJsonPath
            )
        );
    }

    /**
     * 检查Schema节点是否具有指定的类型
     * <p>
     * 支持JSON Schema中type属性的多种表示形式：
     * <ul>
     *   <li>字符串形式：直接比较类型名称，如 {"type": "object"} </li>
     *   <li>数组形式：检查数组中是否包含指定类型，如 {"type": ["object", "null"]}</li>
     * </ul>
     * </p>
     *
     * @param node Schema节点
     * @param type 要检查的类型名称（如"object"、"array"等）
     * @return 如果节点具有指定类型则返回true，否则返回false
     */
    private static boolean hasType(ObjectNode node, String type) {
        JsonNode t = node.get("type");
        if (t == null) return false;
        if (t.isTextual()) return type.equals(t.asText());
        if (t.isArray()) {
            for (JsonNode n : t) {
                if (n.isTextual() && type.equals(n.asText())) return true;
            }
        }
        return false;
    }

    private static ObjectNode obj(JsonNode n) {
        return (n instanceof ObjectNode) ? (ObjectNode) n : null;
    }
}