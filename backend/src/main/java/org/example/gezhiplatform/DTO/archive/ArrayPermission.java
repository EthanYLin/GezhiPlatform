package org.example.gezhiplatform.DTO.archive;


/**
 * 数组字段的权限信息
 * <p>
 * 用于表示用户对档案中数组字段的操作权限，
 * 包括是否可以添加、编辑和删除数组元素。
 * </p>
 * <p>
 * <strong>注意：</strong>数组内的子字段（如 {@code $.array[*].field}）没有单独的编辑权限，
 * 其可编辑性与所属数组的 {@link #canEdit} 保持一致。
 * 详见 {@link ArchivePermissionDetails} 类注释。
 * </p>
 * @param jsonPath 数组字段的JSON Path
 * @param canAdd 是否有权限添加数组元素
 * @param canEdit 是否有权限编辑数组元素
 * @param canDelete 是否有权限删除数组元素
 */
public record ArrayPermission(
    String jsonPath,
    boolean canAdd,
    boolean canEdit,
    boolean canDelete
) {}
