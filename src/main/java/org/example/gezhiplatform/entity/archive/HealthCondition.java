package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.example.gezhiplatform.entity.enums.HealthStatus;
import org.springframework.lang.Nullable;

/**
 * 健康详情类(包括健康状况、健康问题、服药情况、治疗情况)
 * 用于：学生档案 - 健康信息部分 - 身体健康
 * 用于：学生档案 - 健康信息部分 - 心理健康
 */
@Entity
public class HealthCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Nullable
    private HealthStatus healthStatus; // 健康状况(健康或关注)

    @Nullable
    private String healthIssue; // 健康问题

    @Nullable
    private String medicationUse; // 服药情况

    @Nullable
    private String ongoingTreatment; // 治疗情况

}
