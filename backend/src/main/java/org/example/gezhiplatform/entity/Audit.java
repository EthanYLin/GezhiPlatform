package org.example.gezhiplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 审计日志实体类
 */
@Entity
@Data
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 审计ID(数据库内自增)

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user; // 操作用户

    @NotNull
    @Column(nullable = false)
    private AuditOperationType operation; // 操作类型

    @Nullable
    @Column(length = 500)
    private String details; // 操作详情

    @NotNull
    @Column(nullable = false)
    private LocalDateTime time = LocalDateTime.now(); // 操作时间，默认当前时间

    public Audit() {
    }

    public Audit(@NotNull User user, @NotNull AuditOperationType operation, @Nullable String details) {
        this.user = user;
        this.operation = operation;
        this.details = details;
    }
}