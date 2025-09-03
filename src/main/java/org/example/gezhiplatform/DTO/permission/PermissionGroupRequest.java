package org.example.gezhiplatform.DTO.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.entity.permission.PermissionGroup;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限组请求体
 * 用于新增和更新权限组
 * @param name 权限组名称
 * @param roleTypes 权限组中包含的角色类型
 * @param allowedReadableJsonPaths 该权限组可见的JSON Path
 * @param allowedWritableJsonPaths 该权限组可编辑的JSON Path
 */
public record PermissionGroupRequest(
    @NotBlank(message = "权限组名称不能为空")
    @Size(max = 100, message = "权限组名称长度不能超过 100 个字符")
    String name,
    
    @NotNull(message = "角色类型集合不能为null")
    Set<RoleType> roleTypes,
    
    @NotNull(message = "可读JsonPath集合不能为null")
    Set<String> allowedReadableJsonPaths,
    
    @NotNull(message = "可写JsonPath集合不能为null")
    Set<String> allowedWritableJsonPaths
) {
    
    public PermissionGroup toPermissionGroup() {
        PermissionGroup permissionGroup = new PermissionGroup();
        permissionGroup.setName(name);
        permissionGroup.setRoleTypes(new HashSet<>(roleTypes));
        permissionGroup.setAllowedReadableJsonPaths(new HashSet<>(allowedReadableJsonPaths));
        permissionGroup.setAllowedWritableJsonPaths(new HashSet<>(allowedWritableJsonPaths));
        return permissionGroup;
    }
}
