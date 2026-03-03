package org.example.gezhiplatform.DTO.archive;

import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.archive.PermissionGroup;
import org.example.gezhiplatform.entity.archive.ValidationExpr;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 档案权限详情
 * <p>
 * 封装用户对特定档案的完整权限信息，包括角色范围、权限组以及具体的字段访问权限。
 * 用户可能拥有多个权限组，通过合并多个权限组的权限来确定用户的最终访问范围。
 * </p>
 *
 * <p><strong>注意：在 {@code allowedWritableJsonPaths} 中</strong></p>
 * <ul>
 *   <li>数组字段本身（如 {@code $.array}）<strong>不能</strong>出现在 {@code allowedWritableJsonPaths} 中，
 *       数组的可写权限由 {@link #allowedEditArrayJsonPaths} 控制。</li>
 *   <li>数组内的子字段（如 {@code $.array[*].field}）<strong>不能</strong>出现在 {@code allowedWritableJsonPaths} 中，
 *       数组中的字段没有单独编辑权限，其权限与数组的 {@code allowedEdit} 一致。</li>
 *   <li>以上两条约束由
 *       {@link org.example.gezhiplatform.service.permission.ArchivePermissionGroupService#legalizeJsonPaths(org.example.gezhiplatform.entity.archive.PermissionGroup)}
 *       在持久化权限组时负责保障，调用方无需手动清理。</li>
 * </ul>
 *
 * @param grantedRoleAndScopes 用户拥有的且可访问该档案的角色范围列表
 * @param ownedPermissionGroups 用户拥有的且可访问该档案的权限组名称列表
 * @param displayCaption 要显示的公告(所有权限组的公告合并而成，若无公告则为null)
 * @param allowedReadableJsonPaths 允许读取的字段JSON Path集合
 * @param allowedWritableJsonPaths 允许写入的字段JSON Path集合（不含数组字段本身及数组内子字段，见类Javadoc）
 * @param allowedAddArrayJsonPaths 允许添加数组元素的JSON Path集合
 * @param allowedEditArrayJsonPaths 允许编辑数组元素的JSON Path集合
 * @param allowedDeleteArrayJsonPaths 允许删除数组元素的JSON Path集合
 * @param validationSpELs 数据提交时需要校验的SpEL表达式及错误消息集合
 */
public record ArchivePermissionDetails(
    @NotNull List<String> grantedRoleAndScopes,
    @NotNull List<String> ownedPermissionGroups,
    @Nullable String displayCaption,
    @NotNull Set<String> allowedReadableJsonPaths,
    @NotNull Set<String> allowedWritableJsonPaths,
    @NotNull Set<String> allowedAddArrayJsonPaths,
    @NotNull Set<String> allowedEditArrayJsonPaths,
    @NotNull Set<String> allowedDeleteArrayJsonPaths,
    @NotNull Set<ValidationExpr> validationSpELs
) {

    /**
     * 合并多个权限组的权限，返回它们的并集
     * <p>
     * 该方法会将所有权限组中的各类权限进行合并：
     * <ul>
     *   <li>可见的JSON Path集合进行并集操作</li>
     *   <li>可编辑的JSON Path集合进行并集操作</li>
     *   <li>数组操作权限（增删改）进行并集操作</li>
     *   <li>校验SpEL表达式进行并集操作（所有权限组的校验条件都需满足）</li>
     *   <li>收集所有权限组的名称</li>
     * </ul>
     * </p>
     *
     * @param ownedGroups 用户拥有的权限组集合
     * @param grantedRoleAndScopes 拥有的且可访问该学生的角色范围列表
     * @return 合并后的档案权限详情，包含所有权限组权限的并集
     */
    public static ArchivePermissionDetails unionOf(
        @NotNull Set<PermissionGroup> ownedGroups,
        @NotNull List<String> grantedRoleAndScopes
    ) {

        String displayCaption = ownedGroups.stream()
            .map(PermissionGroup::getDisplayCaption).filter(Objects::nonNull).collect(Collectors.joining("\n"));

        Set<String> allowedReadableJsonPaths = ownedGroups.stream()
            .flatMap(group -> group.getAllowedReadableJsonPaths().stream())
            .collect(Collectors.toSet());

        Set<String> allowedWritableJsonPaths = ownedGroups.stream()
            .flatMap(group -> group.getAllowedWritableJsonPaths().stream())
            .collect(Collectors.toSet());

        Set<String> allowedAddArrayJsonPaths = ownedGroups.stream()
            .flatMap(group -> group.getAllowedAddArrayJsonPaths().stream())
            .collect(Collectors.toSet());

        Set<String> allowedEditArrayJsonPaths = ownedGroups.stream()
            .flatMap(group -> group.getAllowedEditArrayJsonPaths().stream())
            .collect(Collectors.toSet());

        Set<String> allowedDeleteArrayJsonPaths = ownedGroups.stream()
            .flatMap(group -> group.getAllowedDeleteArrayJsonPaths().stream())
            .collect(Collectors.toSet());

        Set<ValidationExpr> validationSpELs = ownedGroups.stream()
            .flatMap(group -> group.getValidations().stream())
            .collect(Collectors.toSet());

        List<String> ownedPermissionGroups = ownedGroups.stream()
            .map(PermissionGroup::getName)
            .collect(Collectors.toList());

        return new ArchivePermissionDetails(
            grantedRoleAndScopes,
            ownedPermissionGroups,
            displayCaption,
            allowedReadableJsonPaths,
            allowedWritableJsonPaths,
            allowedAddArrayJsonPaths,
            allowedEditArrayJsonPaths,
            allowedDeleteArrayJsonPaths,
            validationSpELs
        );
    }
}
