package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.jetbrains.annotations.NotNull;

/**
 * 系统中的所有用户角色类型(枚举类)
 * 目前的角色有：
 * 10级: 超级管理员
 * 9级: 校级领导(高级管理员)
 * 7级: 年级组长(普通管理员)
 * 5级: 班主任
 * 5级: 多班级观察员(测试用)
 * 3级: 协作用户(可能包括社工、卫生室、心理老师、生涯导师)
 * 1级: 家长用户
 * 0级: 学生用户
 */
public enum RoleType {
    SUPER_ADMIN("超级管理员", 10),
    PRINCIPAL("校级领导", 9),
    GRADE_DEAN("年级组长", 7),
    CLASS_ADVISOR("班主任", 5),
    MULTIPLE_CLASS_OBSERVER("多班级观察员", 5),
    COLLABORATIVE_USER("协作用户", 3),
    PARENT_USER("家长用户", 1),
    STUDENT_USER("学生用户", 0);

    private final String name;
    private final int level;

    RoleType(String name, int level) {
        this.name = name;
        this.level = level;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonValue
    public int getLevel() {
        return level;
    }

    /**
     * 根据角色名称获取对应的角色类型
     *
     * @param name 角色名称
     * @return 对应的角色类型
     * @throws CustomInvalidArgException 如果角色名称无效
     */
    @JsonCreator
    public static @NotNull RoleType fromName(String name) {
        for (RoleType role : RoleType.values()) {
            if (role.name.equals(name)) {
                return role;
            }
        }
        throw new CustomInvalidArgException(String.format("角色类型 \"%s\" 无效", name));
    }
}
