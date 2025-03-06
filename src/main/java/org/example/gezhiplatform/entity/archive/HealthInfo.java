package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.example.gezhiplatform.entity.enums.HealthStatus;
import org.springframework.lang.Nullable;

@Entity
public class HealthInfo {

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
