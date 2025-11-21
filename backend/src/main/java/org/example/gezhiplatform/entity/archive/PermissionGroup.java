package org.example.gezhiplatform.entity.archive;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id; // 权限组ID(由数据库自增)

    @NotNull(message = "权限组名称不能为空")
    @Column(unique = true, nullable = false)
    private String name; // 权限组名称

    @Nullable
    private String description; // 权限组描述

    @Nullable
    private String displayCaption; // 页面上给用户的文本提示

    @NotNull(message = "权限组是否启用不能为空")
    @Column(nullable = false)
    private Boolean enabled; // 权限组是否启用

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<RoleType> roleTypes = new HashSet<>(); // 权限组中包含的角色类型

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<@NotNull String> allowedReadableJsonPaths = new HashSet<>(); // 该权限组可见的JSON Path

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<@NotNull String> allowedWritableJsonPaths = new HashSet<>(); // 该权限组可编辑的JSON Path

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<@NotNull String> allowedAddArrayJsonPaths = new HashSet<>(); // 该权限组可添加元素的数组的JSON Path

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<@NotNull String> allowedEditArrayJsonPaths = new HashSet<>(); // 该权限组可修改元素的数组的JSON Path

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<@NotNull String> allowedDeleteArrayJsonPaths = new HashSet<>(); // 该权限组可删除元素的数组的JSON Path

    /**
     * <h4>该权限组提交时需要校验的表达式集合</h4>
     * <p>每个校验表达式应<b>返回 bool 类型</b>, 返回 true 时为校验通过。</p>
     * <p>
     *     <b>ValidationExpr</b><br/>
     *     用于在用户提交档案修改时进行自定义校验。<br/>
     *     支持后端表达式（spelExpr）和前端表达式（jsExpr）两种类型，message 字段为校验未通过时的提示消息。
     * </p>
     * <p>
     *     <b>表达式变量（后端）</b><br/>
     *     后端表达式中可引用 prev 与 cur 两个变量, 分别表示修改前后的档案数据对象。<br/>
     *     例如, 表达式 <code>#cur.personalPart.gender != null</code> 表示修改后的档案数据中的性别字段不能为空。
     * </p>
     * <p>
     *     <b>多权限组校验</b><br/>
     *     若用户拥有多个权限组, 则所有权限组中的校验表达式均需通过, 才能允许提交修改。
     * </p>
     */
    @Valid
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<@NotNull ValidationExpr> validations = new HashSet<>(); // 该权限组提交时需要校验的表达式及错误消息

}
