package org.example.gezhiplatform.entity.archive.health_part;

import jakarta.persistence.*;
import org.springframework.lang.Nullable;

/**
 * 学生档案 - 健康信息部分
 */
@Entity
public class HealthPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private HealthCondition physicalCondition; // 身体状况

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private HealthCondition mentalCondition; // 心理状况

}
