package org.example.gezhiplatform.entity.enums;

/**
 * 性别(枚举类)
 */
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
