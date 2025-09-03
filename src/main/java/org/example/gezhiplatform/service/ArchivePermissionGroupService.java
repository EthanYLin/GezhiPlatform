package org.example.gezhiplatform.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.permission.PermissionGroupRequest;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.entity.permission.PermissionGroup;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.PermissionGroupRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;
import static org.example.gezhiplatform.utils.ReflectionUtils.getIllegalSortProperties;

/**
 * 档案权限组Service类
 * 用于对权限组进行增删改查操作
 * 该服务类【只面向管理员权限】
 */
@Service
public class ArchivePermissionGroupService {

    private final PermissionGroupRepository permissionGroupRepository;
    private final ArchiveMetadataService archiveMetadataService;

    public ArchivePermissionGroupService(
        PermissionGroupRepository permissionGroupRepository,
        ArchiveMetadataService archiveMetadataService
    ) {
        this.permissionGroupRepository = permissionGroupRepository;
        this.archiveMetadataService = archiveMetadataService;
    }

    @PostConstruct
    private void checkFields() {
        // 检查PermissionGroup实体的必需字段
        getField(PermissionGroup.class, "name", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "档案权限组服务(ArchivePermissionGroupService)需要依照名称(name)进行搜索和验证, 但未在PermissionGroup类中找到String类型的name字段。"));

        getField(PermissionGroup.class, "roleTypes", Set.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "档案权限组服务(ArchivePermissionGroupService)需要依照角色类型(roleTypes)进行筛选, 但未在PermissionGroup类中找到Set类型的roleTypes字段。"));

        getField(PermissionGroup.class, "allowedReadableJsonPaths", Set.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "档案权限组服务(ArchivePermissionGroupService)需要依照可读JsonPath(allowedReadableJsonPaths)进行筛选和权限验证, " +
                "但未在PermissionGroup类中找到Set类型的allowedReadableJsonPaths字段。"));

        getField(PermissionGroup.class, "allowedWritableJsonPaths", Set.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "档案权限组服务(ArchivePermissionGroupService)需要依照可写JsonPath(allowedWritableJsonPaths)进行筛选和权限验证, " +
                "但未在PermissionGroup类中找到Set类型的allowedWritableJsonPaths字段。"));
    }

    public record allowedJsonPaths(HashSet<String> readableJsonPaths, HashSet<String> writableJsonPaths) {}

    /**
     * 合法化JsonPath路径
     * 根据档案元数据服务验证和标准化JsonPath权限
     * @param allowedReadableJsonPaths 允许读取的JsonPath集合
     * @param allowedWritableJsonPaths 允许写入的JsonPath集合
     * @return 合法化后的JsonPath权限集合
     */
    public allowedJsonPaths legalizeJsonPaths(Set<String> allowedReadableJsonPaths, Set<String> allowedWritableJsonPaths) {
        // 去除所有不在档案(Archive)类中的JsonPath
        var readableJsonPaths = new HashSet<>(archiveMetadataService.getIntersectSet(allowedReadableJsonPaths));
        var writableJsonPaths = new HashSet<>(archiveMetadataService.getIntersectSet(allowedWritableJsonPaths));
        
        // 所有在数组中的JsonPath没有单独的编辑权限, 其权限与其数组入口权限一致
        // 先移除所有在数组中的JsonPath
        writableJsonPaths.removeIf(path -> archiveMetadataService.getFieldMetadata().get(path).insideArray());
        // 然后将所有在数组中的JsonPath的权限与其数组入口权限一致
        for (var entry : archiveMetadataService.getFieldMetadata().entrySet()) {
            if (!entry.getValue().insideArray()) continue;
            if (writableJsonPaths.contains(entry.getValue().arrayEntryJsonPath()))
                writableJsonPaths.add(entry.getKey());
        }
        
        // 去除所有在可编辑组里的ReadOnly字段
        writableJsonPaths.removeIf(path -> !archiveMetadataService.getFieldMetadata().get(path).allowEdit());
        
        // 可编辑的JsonPath必须使其可见
        readableJsonPaths.addAll(writableJsonPaths);

        return new allowedJsonPaths(readableJsonPaths, writableJsonPaths);
    }

    /**
     * 搜索和筛选权限组列表
     * @param keyword 关键词搜索(按名称)
     * @param roleType 按包含的角色类型筛选
     * @param readableJsonPath 按包含的可见JsonPath筛选
     * @param writableJsonPath 按包含的可编辑JsonPath筛选
     * @param pageable 分页信息
     * @return 分页列表(权限组实体)
     * @throws BadRequestException 当分页排序参数包含无效字段时
     */
    @Transactional(readOnly = true)
    public PageResult<PermissionGroup> getAllPermissionGroups(
        @Nullable String keyword,
        @Nullable RoleType roleType,
        @Nullable String readableJsonPath,
        @Nullable String writableJsonPath,
        @NotNull Pageable pageable
    ) throws BadRequestException {
        
        if (pageable.getPageSize() > 1000) {
            throw new BadRequestException("分页的最大页大小为1000条记录。");
        }
        Set<String> illegalSortProperties = getIllegalSortProperties(PermissionGroup.class, pageable);
        if (!illegalSortProperties.isEmpty()) {
            throw new BadRequestException("分页排序参数中包含无效的字段: " + String.join(", ", illegalSortProperties));
        }

        Specification<PermissionGroup> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 关键词搜索(按名称)
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }
            
            // 按包含的角色类型筛选
            if (roleType != null) {
                predicates.add(cb.isMember(roleType, root.get("roleTypes")));
            }
            
            // 按包含的可见JsonPath筛选
            if (readableJsonPath != null && !readableJsonPath.isBlank()) {
                predicates.add(cb.isMember(readableJsonPath, root.get("allowedReadableJsonPaths")));
            }
            
            // 按包含的可编辑JsonPath筛选
            if (writableJsonPath != null && !writableJsonPath.isBlank()) {
                predicates.add(cb.isMember(writableJsonPath, root.get("allowedWritableJsonPaths")));
            }

            // 如果没有任何筛选条件，返回所有权限组；否则AND合并所有条件
            if (predicates.isEmpty()) {
                return cb.conjunction();
            } else {
                return cb.and(predicates.toArray(Predicate[]::new));
            }
        };

        Page<PermissionGroup> result = permissionGroupRepository.findAll(spec, pageable);
        return new PageResult<>(result);
    }

    /**
     * 根据ID获取权限组信息
     * @param id 权限组ID
     * @return 权限组实体
     * @throws NotFoundException 当权限组不存在时
     */
    @Transactional(readOnly = true)
    public PermissionGroup getPermissionGroupById(@NotNull Long id) throws NotFoundException {
        return permissionGroupRepository.findById(id).orElseThrow(
            () -> new NotFoundException("ID为: " + id + " 的权限组不存在")
        );
    }



    /**
     * 向数据库添加一个权限组
     * @param request 新增权限组请求
     * @return 新增的权限组实体
     * @throws BadRequestException 当权限组名称重复时
     */
    @Transactional
    public PermissionGroup addPermissionGroup(@NotNull PermissionGroupRequest request) throws BadRequestException {
        if (permissionGroupRepository.existsByName(request.name())) {
            throw new BadRequestException("名称为: " + request.name() + " 的权限组已存在");
        }
        
        // 合法化JsonPath权限
        allowedJsonPaths legalizedPaths = legalizeJsonPaths(
            request.allowedReadableJsonPaths(),
            request.allowedWritableJsonPaths()
        );
        
        PermissionGroup permissionGroup = request.toPermissionGroup();
        permissionGroup.setAllowedReadableJsonPaths(legalizedPaths.readableJsonPaths());
        permissionGroup.setAllowedWritableJsonPaths(legalizedPaths.writableJsonPaths());
        
        return permissionGroupRepository.save(permissionGroup);
    }



    /**
     * 根据权限组ID更新权限组信息
     * @param id 要修改的权限组ID
     * @param request 新权限组信息请求(可以修改名称)
     * @return 更新后的权限组实体
     * @throws NotFoundException 当原权限组不存在时
     * @throws BadRequestException 当新名称与已有权限组重复时
     */
    @Transactional
    public PermissionGroup updatePermissionGroup(
        @NotNull Long id,
        @NotNull PermissionGroupRequest request
    ) throws BadRequestException {
        PermissionGroup oldPermissionGroup = permissionGroupRepository.findById(id).orElseThrow(
            () -> new NotFoundException("ID为: " + id + " 的权限组不存在")
        );
        
        if (permissionGroupRepository.existsByName(request.name()) && !request.name().equals(oldPermissionGroup.getName())) {
            throw new BadRequestException("不能将权限组的名称从 " + oldPermissionGroup.getName() + " 改为: " + request.name() + " , 因为该名称的权限组已存在。");
        }
        
        // 合法化JsonPath权限
        allowedJsonPaths legalizedPaths = legalizeJsonPaths(
            request.allowedReadableJsonPaths(),
            request.allowedWritableJsonPaths()
        );
        
        PermissionGroup newPermissionGroup = request.toPermissionGroup();
        newPermissionGroup.setId(oldPermissionGroup.getId());
        newPermissionGroup.setAllowedReadableJsonPaths(legalizedPaths.readableJsonPaths());
        newPermissionGroup.setAllowedWritableJsonPaths(legalizedPaths.writableJsonPaths());
        
        return permissionGroupRepository.save(newPermissionGroup);
    }



    /**
     * 删除多个权限组
     * @param ids 权限组ID列表
     */
    @Transactional
    public void deletePermissionGroups(@NotNull List<Long> ids) {
        permissionGroupRepository.deleteAllByIdInBatch(ids);
    }
}
