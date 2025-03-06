package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.springframework.lang.Nullable;

@Entity
public class HealthPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private HealthInfo physicalCondition; // 身体状况

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private HealthInfo mentalCondition; // 心理状况

}
