package org.example.gezhiplatform.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.gezhiplatform.DTO.archive.FieldMetadata;
import org.example.gezhiplatform.exception.FieldNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段元数据生成工具类
 * <p>
 * 该工具类用于从JSON Schema中提取字段元数据，生成字段路径到元数据的映射表。
 * 在遍历Schema的过程中，为每个字段添加'x-jsonpath'扩展属性，并收集字段的详细元数据信息。
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *   <li>为Schema中的每个字段添加'x-jsonpath'扩展属性，标识字段的JSONPath路径</li>
 *   <li>收集字段的完整元数据信息，包括：编辑权限、标题链、路径链、是否为数组、是否为数组元素等</li>
 *   <li>支持嵌套对象和数组的路径标识，数组元素使用'[*]'通配符</li>
 *   <li>验证数组字段的合法性（内部必须是Object类型且包含id字段）</li>
 * </ul>
 * </p>
 */
public class FieldMetadataGenerateUtils {

    /**
     * 从JSON Schema生成字段元数据映射表
     * <p>
     * 遍历整个Schema树结构，为每个字段添加'x-jsonpath'扩展属性，
     * 同时收集字段的完整元数据信息并返回字段路径到元数据的映射表。
     * </p>
     * <p>
     * 处理逻辑：
     * <ul>
     *   <li>从根路径'$'开始递归遍历Schema结构</li>
     *   <li>为每个字段生成唯一的JSONPath路径标识</li>
     *   <li>收集字段的标题链（titles）和路径链（paths）</li>
     *   <li>提取字段的readOnly属性来确定编辑权限</li>
     *   <li>识别字段类型（是否为数组、是否为数组元素）</li>
     *   <li>记录数组元素所属的数组路径（arrayEntryJsonPath）</li>
     * </ul>
     * </p>
     *
     * @param root JSON Schema的根节点，必须是ObjectNode类型
     * @return 字段路径到元数据的映射表，键为JSONPath路径，值为{@link FieldMetadata}对象
     */
    public static Map<String, FieldMetadata> generate(ObjectNode root) {
        if (root == null) return Map.of();
        Map<String, FieldMetadata> fieldMetadata = new HashMap<>();
        annotateUnder(root, new RecursionContext("$", new ArrayList<>(), new ArrayList<>(), null, fieldMetadata));
        return fieldMetadata;
    }

    /**
     * 递归为Schema节点添加JSONPath注解和收集元数据
     * <p>
     * 根据节点类型（object或array）递归处理Schema结构，
     * 为每个可访问的字段添加JSONPath路径标识并收集元数据信息。
     * </p>
     * <p>
     * 处理策略：
     * <ul>
     *   <li><b>object类型</b>：遍历properties下的所有属性，为每个子属性构造新的上下文，并递归处理；</li>
     *   <li><b>array类型</b>：调用checkArray验证其合法性；处理items节点，使用'[*]'表示数组元素路径</li>
     *   <li><b>其他类型</b>：不进行特殊处理</li>
     * </ul>
     * </p>
     *
     * @param node    当前处理的Schema节点
     * @param context 递归上下文，包含当前的jsonPath、标题链、路径链、数组入口路径和元数据收集器
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
            checkArray(context.jsonPath, node);
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
     * 为Schema节点添加'x-jsonpath'属性并存储字段元数据
     * <p>
     * 在Schema节点中添加'x-jsonpath'扩展属性，同时根据当前上下文信息
     * 创建{@link FieldMetadata}对象并存储到元数据映射中。
     * </p>
     *
     * @param node    要添加属性的Schema节点
     * @param context 递归上下文，包含当前的jsonPath、标题链、路径链、数组入口路径和元数据收集器
     */
    private static void addXJsonPathAndFieldMeta(ObjectNode node, RecursionContext context) {
        boolean allowEdit = node.get("readOnly") == null || !node.get("readOnly").asBoolean(false);
        node.put("x-jsonpath", context.jsonPath);
        context.fieldMetadata.put(
            context.jsonPath,
            new FieldMetadata(
                allowEdit,
                new ArrayList<>(context.titles),
                new ArrayList<>(context.paths),
                hasType(node, "array"),
                context.arrayEntryJsonPath != null,
                context.arrayEntryJsonPath
            )
        );
    }

    /**
     * 验证数组字段的合法性
     * <p>
     * 检查数组字段内部元素必须是Object类型，且该Object必须包含id字段。
     * </p>
     *
     * @param path 数组字段的JSONPath路径
     * @param node 数组字段的Schema节点
     * @throws FieldNotFoundException 当数组内部不是Object类型或Object缺少id字段时抛出
     */
    private static void checkArray(String path, ObjectNode node) {
        try {
            boolean objectInside = node.get("items").get("type").asText().equals("object");
            if (!objectInside) throw new FieldNotFoundException(
                "Archive 的数组字段[" + path + "]内部必须是 Object 类型，现在是 " + node.get("items").get("type").asText()
            );
            boolean hasId = node.get("items").get("properties").has("id");
            if (!hasId) throw new FieldNotFoundException(
                "Archive 的数组字段[" + path + "]内部的 Object 类型必须包含 id 字段"
            );
        } catch (FieldNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FieldNotFoundException("对 Archive 的数组字段[" + path + "]进行检查时抛出异常: " + e.getMessage());
        }
    }

    private record RecursionContext(
        String jsonPath,
        List<String> titles,
        List<String> paths,
        String arrayEntryJsonPath,
        Map<String, FieldMetadata> fieldMetadata
    ) {}

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