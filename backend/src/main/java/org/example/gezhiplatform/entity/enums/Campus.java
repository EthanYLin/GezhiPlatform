package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.jetbrains.annotations.NotNull;

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

    /**
     * 根据校区名称获取对应的 Campus 枚举实例
     * @param name 校区名称
     * @return 对应的 Campus 枚举实例
     * @throws CustomInvalidArgException 如果没有找到对应的校区名称
     */
    @JsonCreator
    public static @NotNull Campus fromName(String name) {
        for (Campus campus : Campus.values()) {
            if (campus.name.equals(name)) {
                return campus;
            }
        }
        throw new CustomInvalidArgException("无效的校区名称: " + name);
    }

}
