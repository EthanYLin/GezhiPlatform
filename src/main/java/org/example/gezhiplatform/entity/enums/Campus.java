package org.example.gezhiplatform.entity.enums;

/**
 * 校区(枚举类)
 */
public enum Campus {
    HUANGPU("黄浦校区"), FENGXIAN("奉贤校区");

    private final String name;

    Campus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
