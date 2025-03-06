package org.example.gezhiplatform.entity.enums;

public enum HealthStatus {
    HEALTHY("健康"),
    ATTENTION("关注");

    private final String name;

    HealthStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
