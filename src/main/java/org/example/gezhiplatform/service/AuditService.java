package org.example.gezhiplatform.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.audit.AuditRecordResponse;
import org.example.gezhiplatform.entity.Audit;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.example.gezhiplatform.repository.AuditRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;
import static org.example.gezhiplatform.utils.ReflectionUtils.getIllegalSortProperties;

/**
 * 审计服务
 * <p>
 * 负责审计日志的记录和查询功能。
 * </p>
 * <p><b>职责：</b></p>
 * <ul>
 *   <li>记录系统关键操作的审计日志</li>
 *   <li>提供审计日志的查询接口</li>
 * </ul>
 */
@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @PostConstruct
    private void checkFields() {
        getField(Audit.class, "time", LocalDateTime.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "审计服务(AuditService)需要依照时间(time)进行筛选, 但未在Audit类中找到LocalDateTime类型的time字段。"));

        getField(Audit.class, "operation", AuditOperationType.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "审计服务(AuditService)需要依照操作类型(operation)进行筛选, 但未在Audit类中找到AuditOperationType类型的operation字段。"));

        getField(Audit.class, "details", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "审计服务(AuditService)需要依照操作详情(details)进行搜索, 但未在Audit类中找到String类型的details字段。"));

        // 检查用户关联字段路径：Audit -> User -> username
        getField(Audit.class, "user", User.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "审计服务(AuditService)需要依照操作用户(user)进行关联查询, 但未在Audit类中找到User类型的user字段。"));

        getField(User.class, "username", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "审计服务(AuditService)需要依照用户名(username)进行搜索, 但未在User类中找到String类型的username字段。"));
    }

    /**
     * 记录审计事件
     * <p>
     * 创建一条新的审计记录并保存到数据库。
     * </p>
     *
     * @param user      操作用户
     * @param operation 操作类型
     * @param details   操作详情
     */
    @Transactional
    public void log(@NotNull User user, @NotNull AuditOperationType operation, @Nullable String details) {
        Audit audit = new Audit(user, operation, details);
        auditRepository.save(audit);
    }

    /**
     * 搜索审计记录
     * <p>
     * 根据多种条件查询审计记录，支持分页返回结果。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出 {@link BadRequestException}</b></p>
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param keyword   操作详情搜索关键字（可选）
     * @param operation 操作类型（可选）
     * @param username  操作用户用户名（完全匹配，可选）
     * @param pageable  分页参数
     * @return 审计记录分页结果
     * @throws BadRequestException 当分页大小超过1000时抛出
     */
    @Transactional(readOnly = true)
    public PageResult<AuditRecordResponse> searchAuditRecords(
        @Nullable LocalDateTime startTime,
        @Nullable LocalDateTime endTime,
        @Nullable String keyword,
        @Nullable AuditOperationType operation,
        @Nullable String username,
        @NotNull Pageable pageable
    ) throws BadRequestException {
        if (pageable.getPageSize() > 1000) {
            throw new BadRequestException("分页的最大页大小为1000条记录。");
        }
        Set<String> illegalSortProperties = getIllegalSortProperties(Audit.class, pageable);
        if (!illegalSortProperties.isEmpty()) {
            throw new BadRequestException("分页排序参数中包含无效的字段: " + String.join(", ", illegalSortProperties));
        }
        Specification<Audit> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 时间范围筛选
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("time"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("time"), endTime));
            }

            // 操作类型筛选
            if (operation != null) {
                predicates.add(cb.equal(root.get("operation"), operation));
            }

            // 操作用户用户名筛选（完全匹配）
            if (username != null && !username.isBlank()) {
                Join<Audit, User> userJoin = root.join("user");
                predicates.add(cb.equal(userJoin.get("username"), username));
            }

            // 操作详情关键字筛选（模糊匹配）
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(root.get("details"), "%" + keyword + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return new PageResult<>(
            auditRepository
                .findBy(spec, q -> q.sortBy(pageable.getSort()).page(pageable))
                .map(AuditRecordResponse::of)
        );
    }
}