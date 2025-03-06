package org.example.gezhiplatform.entity.enums;

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
