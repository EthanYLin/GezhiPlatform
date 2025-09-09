package org.example.gezhiplatform.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import org.example.gezhiplatform.annotation.JsonIncludeMethod;
import org.example.gezhiplatform.annotation.JsonTitle;

/**
 * JSON Schema 生成工具类
 * <p>
 * 该工具类用于为Java类生成符合JSON Schema规范的结构描述，
 * 用于支持前端表单自动生成工具（如React-JSON-Schema-Form）来生成表单。
 * </p>
 * <p>
 * 生成的Schema支持以下特性：
 * <ul>
 *   <li>Jackson序列化注解（如@JsonProperty、@JsonPropertyOrder等）</li>
 *   <li>Jakarta Validation验证注解（如@NotNull、@Pattern等）</li>
 *   <li>自定义注解支持：{@link JsonTitle}设置字段标题、{@link JsonIncludeMethod}包含方法字段</li>
 * </ul>
 * </p>
 */
public class JsonSchemaGenerateUtils {

    /**
     * 为指定的Java类生成JSON Schema
     * <p>
     * 根据类的结构、注解信息生成完整的JSON Schema，支持嵌套对象、数组、
     * 枚举类型以及各种验证规则的描述。生成的Schema遵循JSON Schema Draft 2020-12规范。
     * </p>
     * <p>
     * 配置特性：
     * <ul>
     *   <li>Jackson模块：处理枚举值平铺、尊重属性顺序</li>
     *   <li>Jakarta Validation模块：包含正则表达式模式</li>
     *   <li>字段标题解析：优先使用@JsonTitle注解值，否则使用字段名</li>
     *   <li>方法包含：仅包含标记@JsonIncludeMethod注解的方法</li>
     * </ul>
     * </p>
     *
     * @param clazz 要生成Schema的Java类
     * @return 生成的JSON Schema，以ObjectNode形式返回
     */
    public static ObjectNode generateSchema(Class<?> clazz) {
        SchemaGeneratorConfigBuilder configBuilder =
            new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.FULL_DOCUMENTATION)
                .with(new JacksonModule(
                    JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE,
                    JacksonOption.RESPECT_JSONPROPERTY_ORDER
                ))
                .with(new JakartaValidationModule(
                    JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
                ))
                .with(Option.NULLABLE_FIELDS_BY_DEFAULT)
                .with(Option.INLINE_ALL_SCHEMAS)
                .with(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS);
        configBuilder.forFields().withTitleResolver(field -> {
            var title = field.getAnnotationConsideringFieldAndGetter(JsonTitle.class);
            return title != null ? title.value() : field.getName();
        });
        configBuilder.forMethods()
                     .withIgnoreCheck(m -> m.getAnnotation(JsonIncludeMethod.class) == null)
                     .withTitleResolver(m -> {
                         var title = m.getAnnotationConsideringFieldAndGetter(JsonTitle.class);
                         return title != null ? title.value() : m.getName();
                     });

        var config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(clazz);
    }


}
