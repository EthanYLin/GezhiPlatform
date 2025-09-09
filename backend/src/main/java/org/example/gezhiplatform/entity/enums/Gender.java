package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

/**
 * 性别(枚举类)
 */
public enum Gender {
    MALE("男"),
    FEMALE("女"),
    OTHER("其他"),
    UNKNOWN("未知");

    private final String name;

    Gender(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    /**
     * 根据“男”、“女”等性别名称获取枚举实例
     * @param name 性别名称
     * @return 枚举实例(默认会返回 UNKNOWN)
     */
    @JsonCreator
    public static @NotNull Gender fromName(String name) {
        for (Gender gender : Gender.values()) {
            if (gender.getName().equals(name)) {
                return gender;
            }
        }
        return Gender.UNKNOWN;
    }
}
