package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.Nullable;

/**
 * 健康状态(枚举类)
 */
public enum HealthStatus {
    HEALTHY("健康"),
    ATTENTION("关注");

    private final String name;

    HealthStatus(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static @Nullable HealthStatus fromName(String name) {
        for (HealthStatus healthStatus : HealthStatus.values()) {
            if (healthStatus.getName().equals(name)) {
                return healthStatus;
            }
        }
        return null;
    }
}
