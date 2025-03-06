package org.example.gezhiplatform.entity.enums;

public enum Gender {
    MALE("男"),
    FEMALE("女"),
    UNKNOWN("未知"),
    OTHER("其他");

    private final String name;

    Gender(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
