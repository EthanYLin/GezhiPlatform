package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h4>校验表达式</h4>
 * <p>用于存储权限组的校验表达式及其对应的错误提示消息。</p>
 * <p>
 * 支持两种类型的校验表达式：
 * <ul>
 *   <li><b>SpEL表达式（后端校验）</b>：在服务端执行，用于档案数据提交时的业务逻辑校验</li>
 *   <li><b>JavaScript表达式（前端校验）</b>：在客户端执行，用于提供即时的用户输入反馈</li>
 * </ul>
 * 两种表达式均应返回 boolean 类型，返回 true 时表示校验通过。
 * </p>
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationExpr {

    /**
     * SpEL表达式（后端校验）
     * <p>
     * 在服务端执行的校验表达式，应返回 boolean 类型，返回 true 时为校验通过。<br/>
     * 表达式中可引用 prev 与 cur 两个变量，分别表示修改前后的档案数据对象。<br/>
     * 例如：<code>#cur.personalPart.gender != null</code> 表示修改后的档案数据中的性别字段不能为空。
     * </p>
     */
    @NotNull(message = "SpEL表达式不能为空")
    private String spelExpr;

    /**
     * JavaScript表达式（前端校验）
     * <p>
     * 在客户端执行的校验表达式，应返回 boolean 类型，返回 true 时为校验通过。<br/>
     * 用于提供即时的用户输入反馈，提升用户体验。
     * </p>
     */
    @NotNull(message = "JavaScript表达式不能为空")
    private String jsExpr;

    /**
     * 校验未通过时的提示消息
     * <p>
     * 当校验失败时，向用户显示的错误提示信息。
     * </p>
     */
    @NotNull(message = "提示消息不能为空")
    private String message;

}
