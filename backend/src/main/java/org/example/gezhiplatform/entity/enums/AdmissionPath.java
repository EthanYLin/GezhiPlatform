package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.Nullable;

/**
 * 录取方式(枚举类)
 */
public enum AdmissionPath {
    NORMAL("普通录取");

    private final String name;

    AdmissionPath(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static @Nullable AdmissionPath fromName(String name) {
        for (AdmissionPath admissionPath : AdmissionPath.values()) {
            if (admissionPath.name.equals(name)) {
                return admissionPath;
            }
        }
        return null;
    }

}
