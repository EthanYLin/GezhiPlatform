package org.example.gezhiplatform.entity.enums;

public enum PoliticalStatus {
    TUANYUAN("共青团员"),
    QUNZHONG("群众"),
    OTHER("其他");

    private final String name;

    PoliticalStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
