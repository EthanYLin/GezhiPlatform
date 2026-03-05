package org.example.gezhiplatform.service.permission;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.archive.FieldMetadata;
import org.example.gezhiplatform.entity.archive.PermissionGroup;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.PermissionGroupRepository;
import org.example.gezhiplatform.service.metadata.ArchiveMetadataService;
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
 * 档案权限组配置服务
 *
 * <p><b>职责：</b></p>
 * <ul>
 *   <li>支持权限组的配置、更新与管理</li>
 *   <li>维护角色与权限组的配置关系</li>
 * </ul>
 *
 * <p><b>该服务类【只面向管理员权限】及被其他服务调用</b></p>
 *
 * <p><b>架构说明：</b></p>
 * <p>
 * 访问控制服务（{@link ArchiveAccessControlService}）将根据用户的权限过滤档案数据：
 * <ul>
 *   <li>在<b>读取操作</b>中，只返回档案中用户有权限访问的字段。</li>
 *   <li>在<b>更新操作</b>中，去除请求体中用户不可写的字段数据。</li>
 * </ul>
 * </p>
 * <p>
 * 在进行权限判断时，依赖以下服务：
 * <ul>
 *   <li>档案元字段服务（{@link ArchiveMetadataService}）- 提供档案字段及类型信息。</li>
 *   <li>权限组配置服务（{@link ArchivePermissionGroupService}）- 提供用户角色所在的权限组及其读写权限。</li>
 * </ul>
 * </p>
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
                "档案权限组服务(ArchivePermissionGroupService)需要依照角色类型(roleTypes)进行筛选, " +
                "但未在PermissionGroup类中找到Set类型的roleTypes字段。"));

        getField(PermissionGroup.class, "allowedReadableJsonPaths", Set.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "档案权限组服务(ArchivePermissionGroupService)需要依照可读JsonPath(allowedReadableJsonPaths)进行筛选和权限验证, " +
                "但未在PermissionGroup类中找到Set类型的allowedReadableJsonPaths字段。"));

        getField(PermissionGroup.class, "allowedWritableJsonPaths", Set.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "档案权限组服务(ArchivePermissionGroupService)需要依照可写JsonPath(allowedWritableJsonPaths)进行筛选和权限验证, " +
                "但未在PermissionGroup类中找到Set类型的allowedWritableJsonPaths字段。"));
    }

    /**
     * 合法化权限组中的JsonPath权限配置
     * <p>
     * <b>注意</b>：该方法会直接修改传入的权限组对象的权限配置。
     * </p>
     *
     * @param request 需要合法化的权限组实体，方法会直接修改其权限字段
     */
    public void legalizeJsonPaths(PermissionGroup request) {

        // 去除所有不在档案(Archive)类中的JsonPath
        request.setAllowedReadableJsonPaths(
            archiveMetadataService.getIntersectSet(request.getAllowedReadableJsonPaths()));
        request.setAllowedWritableJsonPaths(
            archiveMetadataService.getIntersectSet(request.getAllowedWritableJsonPaths()));
        request.setAllowedAddArrayJsonPaths(
            archiveMetadataService.getIntersectSet(request.getAllowedAddArrayJsonPaths()));
        request.setAllowedEditArrayJsonPaths(
            archiveMetadataService.getIntersectSet(request.getAllowedEditArrayJsonPaths()));
        request.setAllowedDeleteArrayJsonPaths(
            archiveMetadataService.getIntersectSet(request.getAllowedDeleteArrayJsonPaths()));

        // AllowedAdd/Edit/DeleteArrayJsonPaths 中的元素必须是数组类型
        request.getAllowedAddArrayJsonPaths().removeIf(
            path -> !archiveMetadataService.getArrayFields().contains(path)
        );
        request.getAllowedEditArrayJsonPaths().removeIf(
            path -> !archiveMetadataService.getArrayFields().contains(path)
        );
        request.getAllowedDeleteArrayJsonPaths().removeIf(
            path -> !archiveMetadataService.getArrayFields().contains(path)
        );

        // 去除所有在可编辑组里的数组字段本身, 例如 $.familyPart.otherRelatives
        // 数组字段的编辑权限由 AllowedAdd/Edit/DeleteArrayJsonPaths 控制
        request.getAllowedWritableJsonPaths().removeAll(archiveMetadataService.getArrayFields());

        // 去除所有在可编辑组里的数组中的字段, 例如 $.familyPart.otherRelatives[*].birthYear
        // 数组中的字段没有单独编辑权限, 其权限与数组的AllowEdit一致
        request.getAllowedWritableJsonPaths().removeIf(
            path -> archiveMetadataService.getFields().get(path).insideArray());

        // 去除所有在可编辑组里的ReadOnly字段
        request.getAllowedWritableJsonPaths().removeIf(
            path -> !archiveMetadataService.getFields().get(path).allowEdit());

        // 若要可编辑下层元素, 则必须可编辑上层元素，例如要可编辑 $.A.B, 则必须可编辑 $.A
        // 该规则不对数组及数组内字段生效
        List<String> writeAddOns = new ArrayList<>();
        request.getAllowedWritableJsonPaths().forEach(path -> {
            FieldMetadata field = archiveMetadataService.getFields().get(path);
            if (field == null || field.isArray() || field.insideArray()) return;
            writeAddOns.addAll(field.ancestorPaths());
        });
        request.getAllowedWritableJsonPaths().addAll(writeAddOns);

        // 可编辑的JsonPath必须使其可见
        request.getAllowedReadableJsonPaths().addAll(request.getAllowedWritableJsonPaths());

        // 若要可见下层元素, 则必须可见上层元素，例如要可见 $.A.B, 则必须可见 $.A
        List<String> readAddOns = new ArrayList<>();
        request.getAllowedReadableJsonPaths().forEach(path -> {
            FieldMetadata field = archiveMetadataService.getFields().get(path);
            if (field == null) return;
            readAddOns.addAll(field.ancestorPaths());
        });
        request.getAllowedReadableJsonPaths().addAll(readAddOns);

        // 若某数组可见，其内部的id字段必须可见
        List<String> idAddOns = new ArrayList<>();
        request.getAllowedReadableJsonPaths().stream()
            .filter(archiveMetadataService.getArrayFields()::contains)
            .map(p -> p + "[*].id")
            .filter(archiveMetadataService.getFields()::containsKey)
            .forEach(idAddOns::add);
        request.getAllowedReadableJsonPaths().addAll(idAddOns);
    }

    /**
     * 搜索和筛选权限组列表
     *
     * @param keyword          关键词搜索(按名称)
     * @param roleType         按包含的角色类型筛选
     * @param readableJsonPath 按包含的可见JsonPath筛选
     * @param writableJsonPath 按包含的可编辑JsonPath筛选
     * @param pageable         分页信息
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
     *
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
     * 获取包含指定角色类型的所有权限组
     *
     * @param roleType 角色类型
     * @return 权限组实体集合
     */
    @Transactional
    public Set<PermissionGroup> getPermissionGroupsByRoleType(@NotNull RoleType roleType) {
        var result = permissionGroupRepository.findAll(
            (root, _, cb) -> cb.isMember(roleType, root.get("roleTypes"))
        );
        return new HashSet<>(result);
    }


    /**
     * 向数据库添加一个权限组
     *
     * @param permissionGroup 新增权限组实体
     * @return 新增的权限组实体
     * @throws BadRequestException 当权限组名称重复时
     */
    @Transactional
    public PermissionGroup addPermissionGroup(@NotNull PermissionGroup permissionGroup) throws BadRequestException {
        if (permissionGroupRepository.existsByName(permissionGroup.getName())) {
            throw new BadRequestException("名称为: " + permissionGroup.getName() + " 的权限组已存在");
        }
        // 强制清空ID，确保是新建而非更新
        permissionGroup.setId(null);
        // 合法化 JsonPath 权限
        this.legalizeJsonPaths(permissionGroup);
        return permissionGroupRepository.save(permissionGroup);
    }


    /**
     * 根据权限组ID更新权限组信息
     *
     * @param id      要修改的权限组ID
     * @param permissionGroup 新权限组信息实体(可以修改名称)
     * @return 更新后的权限组实体
     * @throws NotFoundException   当原权限组不存在时
     * @throws BadRequestException 当新名称与已有权限组重复时
     */
    @Transactional
    public PermissionGroup updatePermissionGroup(
        @NotNull Long id,
        @NotNull PermissionGroup permissionGroup
    ) throws BadRequestException {
        PermissionGroup oldPermissionGroup = permissionGroupRepository.findById(id).orElseThrow(
            () -> new NotFoundException("ID为: " + id + " 的权限组不存在")
        );

        if (permissionGroupRepository.existsByName(permissionGroup.getName()) &&
            !permissionGroup.getName().equals(oldPermissionGroup.getName())) {
            throw new BadRequestException(
                "不能将权限组的名称从 " + oldPermissionGroup.getName() + " 改为: " + permissionGroup.getName() +
                " , 因为该名称的权限组已存在。");
        }

        // 强制使用路径参数中的ID，忽略请求体中的ID
        permissionGroup.setId(id);
        // 合法化 JsonPath 权限
        this.legalizeJsonPaths(permissionGroup);
        return permissionGroupRepository.save(permissionGroup);
    }


    /**
     * 删除多个权限组
     *
     * @param ids 权限组ID列表
     */
    @Transactional
    public void deletePermissionGroups(@NotNull List<Long> ids) {
        permissionGroupRepository.deleteAllByIdInBatch(ids);
    }
}
