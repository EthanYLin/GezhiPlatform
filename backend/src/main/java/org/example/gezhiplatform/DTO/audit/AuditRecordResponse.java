package org.example.gezhiplatform.DTO.audit;

import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.Audit;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 审计记录响应体
 *
 * @param id        审计记录ID
 * @param username  操作用户的用户名
 * @param name      操作用户的姓名
 * @param time      操作时间
 * @param operation 操作类型
 * @param details   操作详情
 */
public record AuditRecordResponse(
    @NotNull Long id, // 审计记录ID
    @NotNull String username, // 操作用户的用户名
    @NotNull String name, // 操作用户的姓名
    @NotNull LocalDateTime time, // 操作时间
    @NotNull AuditOperationType operation, // 操作类型
    @Nullable String details // 操作详情
) {
    public static AuditRecordResponse of(Audit audit) {
        return new AuditRecordResponse(
            audit.getId(),
            audit.getUser().getUsername(),
            audit.getUser().getName(),
            audit.getTime(),
            audit.getOperation(),
            audit.getDetails()
        );
    }
}