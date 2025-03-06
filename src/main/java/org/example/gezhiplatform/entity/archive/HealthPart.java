package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.lang.Nullable;

@Entity
public class HealthPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Nullable
    private HealthInfo physicalCondition; // 身体状况

    @Nullable
    private HealthInfo mentalCondition; // 心理状况

}
