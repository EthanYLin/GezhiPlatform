package org.example.gezhiplatform.entity.enums;

/**
 * 录取方式(枚举类)
 */
public enum AdmissionPath {
    NORMAL("普通录取");

    private final String name;

    AdmissionPath(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
