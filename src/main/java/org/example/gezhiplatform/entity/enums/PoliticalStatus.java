package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.Nullable;

/**
 * 政治面貌(枚举类)
 */
public enum PoliticalStatus {
    TUANYUAN("共青团员"),
    QUNZHONG("群众"),
    OTHER("其他");

    private final String name;

    PoliticalStatus(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static @Nullable PoliticalStatus fromName(String name) {
        for (PoliticalStatus politicalStatus : PoliticalStatus.values()) {
            if (politicalStatus.name.equals(name)) {
                return politicalStatus;
            }
        }
        return null;
    }

}
