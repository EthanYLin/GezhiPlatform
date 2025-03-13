package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * 校区(枚举类)
 */
public enum Campus {
    HUANGPU("黄浦校区"), FENGXIAN("奉贤校区");

    private final String name;

    Campus(String name) {
        this.name = name;
    }

    @JsonValue
    public @NotNull String getName() {
        return name;
    }

    @JsonCreator
    public static @NotNull Campus fromName(String name) {
        return Arrays.stream(Campus.values())
                     .filter(c -> c.name.equals(name))
                     .findFirst()
                     .orElseThrow(() -> new CustomInvalidArgException("无效的校区名称: " + name));
    }

}
