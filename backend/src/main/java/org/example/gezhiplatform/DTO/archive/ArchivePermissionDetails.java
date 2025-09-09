package org.example.gezhiplatform.DTO.archive;

import java.util.List;

/**
 * 档案权限详情响应体
 * <p>
 * 用于返回用户对档案访问的完整权限信息，
 * 包括：拥有的且可访问该学生的角色范围、拥有的且可访问该学生的权限组 以及 允许访问的JSON Path。
 * </p>
 * @param grantedRoleAndScopes 拥有的且可访问该学生的角色范围列表
 * @param ownedPermissionGroups 拥有的且可访问该学生的权限组列表
 * @param allowedJsonPaths 允许访问的JSON Path信息
 */
public record ArchivePermissionDetails(
    List<String> grantedRoleAndScopes,
    List<String> ownedPermissionGroups,
    AllowedJsonPathsResponse allowedJsonPaths
) {
}
