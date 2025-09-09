package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.jetbrains.annotations.NotNull;

/**
 * 审计操作类型(枚举类)
 */
public enum AuditOperationType {
    ARCHIVE_QUERY("档案查询"),
    ARCHIVE_EXPORT("档案导出");

    private final String desc;

    AuditOperationType(String desc) {
        this.desc = desc;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    @JsonCreator
    public static @NotNull AuditOperationType fromDesc(String desc) throws CustomInvalidArgException {
        for (AuditOperationType operation : AuditOperationType.values()) {
            if (operation.desc.equals(desc)) {
                return operation;
            }
        }
        throw new CustomInvalidArgException(String.format("操作类型 \"%s\" 无效", desc));
    }
}
