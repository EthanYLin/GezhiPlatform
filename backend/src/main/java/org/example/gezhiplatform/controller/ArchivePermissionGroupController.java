package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.entity.archive.PermissionGroup;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.permission.ArchivePermissionGroupService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 档案权限组管理控制器
 * <p>
 * 该控制器仅面向【系统管理员】，提供档案权限组的维护功能，包括：
 * <ul>
 *   <li>1. 根据条件搜索权限组列表</li>
 *   <li>2. 获取指定权限组的详细信息</li>
 *   <li>3. 新增权限组</li>
 *   <li>4. 更新权限组信息和权限配置</li>
 *   <li>5. 批量删除权限组</li>
 * </ul>
 * </p>
 * <p>
 * <b>该控制器仅面向【超级管理员】，所有操作都需要SUPER_ADMIN权限。</b>
 * </p>
 * <p>
 * 权限组用于管理不同角色类型对档案JsonPath的访问权限，支持细粒度的权限控制。
 * </p>
 *
 */
@SaCheckRole("SUPER_ADMIN")
@RestController
@RequestMapping("/archive/permission-groups")
@Tag(name = "档案权限组管理(面向管理员)", description = "档案权限组的维护接口")
public class ArchivePermissionGroupController {

    private final ArchivePermissionGroupService archivePermissionGroupService;

    public ArchivePermissionGroupController(ArchivePermissionGroupService archivePermissionGroupService) {
        this.archivePermissionGroupService = archivePermissionGroupService;
    }

    // ========================= GET 读取 =========================

    /**
     * 根据条件搜索权限组列表
     * <p>
     * 支持<b>关键词、角色类型、可读JsonPath、可写JsonPath</b>的组合查询。所有查询条件使用<b>AND逻辑</b>连接，
     * 关键词内部使用<b>模糊匹配</b>在权限组名称中进行搜索。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出异常</b></p>
     * <p>
     * 查询条件组合方式：
     * <ul>
     *   <li><b>关键词搜索</b>：当keyword不为null且非空时，在权限组名称进行模糊匹配</li>
     *   <li><b>角色类型过滤</b>：当roleType不为null时，只返回包含指定角色类型的权限组</li>
     *   <li><b>可读JsonPath过滤</b>：当readableJsonPath不为null且非空时，只返回包含指定可读JsonPath的权限组</li>
     *   <li><b>可写JsonPath过滤</b>：当writableJsonPath不为null且非空时，只返回包含指定可写JsonPath的权限组</li>
     *   <li><b>组合查询</b>：多个条件同时存在时，使用AND逻辑连接</li>
     *   <li><b>无条件查询</b>：所有参数为null时，返回系统中的所有权限组</li>
     * </ul>
     * </p>
     * <p>
     * 使用场景举例：
     * <ul>
     *   <li>查找所有班主任权限组：keyword=null, roleType=班主任, readableJsonPath=null, writableJsonPath=null</li>
     *   <li>查找名称包含"学生"的权限组：keyword="学生", roleType=null, readableJsonPath=null, writableJsonPath=null</li>
     *   <li>查找可读取个人信息的权限组：keyword=null, roleType=null, readableJsonPath="$.personalPart", writableJsonPath=null</li>
     *   <li>查找可编辑身份证号的班主任权限组：keyword=null, roleType=班主任, readableJsonPath=null, writableJsonPath="$.personalPart.rin"</li>
     * </ul>
     * </p>
     *
     * @param keyword            关键词搜索条件，为null或空白时不进行关键词搜索，支持权限组名称模糊匹配
     * @param roleType           角色类型过滤条件，为null时不进行角色类型过滤
     * @param readableJsonPath   可读JsonPath过滤条件，为null或空白时不进行过滤
     * @param writableJsonPath   可写JsonPath过滤条件，为null或空白时不进行过滤
     * @param pageable           分页参数，默认每页20条记录，最大页大小为1000
     * @return 符合所有条件的权限组详细信息分页结果
     * @throws BadRequestException 当分页大小超过1000或排序字段无效时抛出
     * @apiNote GET /archive/permission-groups?keyword=学生&roleType=班主任&readableJsonPath=$.personalPart&page=0&size=20&sort=id,asc
     */
    @GetMapping
    @Transactional
    @Operation(summary = "条件搜索权限组列表")
    public PageResult<PermissionGroup> searchPermissionGroups(
        @Parameter(description = "关键词搜索，支持权限组名称模糊匹配")
        @RequestParam(required = false) String keyword,
        @Parameter(description = "角色类型过滤")
        @RequestParam(required = false) String roleType,
        @Parameter(description = "可读JsonPath过滤")
        @RequestParam(required = false) String readableJsonPath,
        @Parameter(description = "可写JsonPath过滤")
        @RequestParam(required = false) String writableJsonPath,
        @PageableDefault(size = 20) Pageable pageable
    ) throws BadRequestException {
        @Nullable RoleType roleTypeEnum = null;
        if (roleType != null && !roleType.isBlank()) roleTypeEnum = RoleType.fromDesc(roleType);
        
        return archivePermissionGroupService.getAllPermissionGroups(
            keyword, roleTypeEnum, readableJsonPath, writableJsonPath, pageable
        );
    }

    /**
     * 根据ID获取权限组详细信息
     * <p>
     * 返回指定ID的权限组完整信息，包括权限组名称、包含的角色类型、
     * 可读JsonPath集合、可写JsonPath集合等详细配置。
     * </p>
     *
     * @param id 权限组ID
     * @return 权限组详细信息
     * @throws NotFoundException 当指定ID的权限组不存在时
     * @apiNote GET /archive/permission-groups/{id}
     */
    @GetMapping("/{id}")
    @Transactional
    @Operation(summary = "获取权限组详细信息")
    public PermissionGroup getPermissionGroupById(
        @Parameter(description = "权限组ID", required = true)
        @PathVariable @NotNull Long id
    ) throws NotFoundException {
        return archivePermissionGroupService.getPermissionGroupById(id);
    }

    // ========================= POST 创建 =========================

    /**
     * 新增权限组
     * <p>
     * 创建一个新的档案权限组，包括权限组名称、角色类型配置、
     * JsonPath权限配置等。系统会自动对JsonPath进行合法化处理。
     * </p>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>权限组名称必须唯一，不能与现有权限组重复</li>
     *   <li>JsonPath权限会经过系统验证和合法化处理</li>
     *   <li>可编辑的JsonPath必须同时具有可读权限</li>
     *   <li>只读字段不能设置为可编辑</li>
     * </ul>
     * </p>
     *
     * @param permissionGroup 新增权限组请求体，包含权限组的完整配置信息
     * @return 新创建的权限组详细信息
     * @throws BadRequestException 当权限组名称重复或配置无效时
     * @apiNote POST /archive/permission-groups
     */
    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增权限组")
    public PermissionGroup addPermissionGroup(
        @Parameter(description = "权限组配置信息", required = true)
        @RequestBody @Valid PermissionGroup permissionGroup
    ) throws BadRequestException {
        return archivePermissionGroupService.addPermissionGroup(permissionGroup);
    }

    // ========================= PUT 更新 =========================

    /**
     * 更新权限组信息
     * <p>
     * 更新指定ID的权限组配置，包括权限组名称、角色类型配置、
     * JsonPath权限配置等。系统会自动对JsonPath进行合法化处理。
     * </p>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>可以修改权限组名称，但新名称不能与其他权限组重复</li>
     *   <li>JsonPath权限会经过系统验证和合法化处理</li>
     *   <li>可编辑的JsonPath必须同时具有可读权限</li>
     *   <li>只读字段不能设置为可编辑</li>
     * </ul>
     * </p>
     *
     * @param id      要更新的权限组ID
     * @param permissionGroup 更新权限组请求体，包含权限组的完整配置信息
     * @return 更新后的权限组详细信息
     * @throws NotFoundException   当指定ID的权限组不存在时
     * @throws BadRequestException 当新名称重复或配置无效时
     * @apiNote PUT /archive/permission-groups/{id}
     */
    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "更新权限组信息")
    public PermissionGroup updatePermissionGroup(
        @Parameter(description = "权限组ID", required = true)
        @PathVariable @NotNull Long id,
        @Parameter(description = "更新的权限组配置信息", required = true)
        @RequestBody @Valid PermissionGroup permissionGroup
    ) throws NotFoundException, BadRequestException {
        return archivePermissionGroupService.updatePermissionGroup(id, permissionGroup);
    }

    // ========================= DELETE 删除 =========================

    /**
     * 批量删除权限组
     * <p>
     * 根据权限组ID列表批量删除权限组记录。如果某个ID不存在，该ID会被忽略，
     * 不会影响其他ID的删除操作。
     * </p>
     *
     * @param ids 要删除的权限组ID列表
     * @apiNote DELETE /archive/permission-groups?ids=1,2,3
     */
    @DeleteMapping
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "批量删除权限组")
    public void deletePermissionGroups(
        @Parameter(description = "要删除的权限组ID列表", required = true)
        @RequestParam("ids") @NotNull List<Long> ids
    ) {
        archivePermissionGroupService.deletePermissionGroups(ids);
    }
}
