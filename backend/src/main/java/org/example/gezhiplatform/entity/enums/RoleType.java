package org.example.gezhiplatform.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.example.gezhiplatform.entity.role.*;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
@Getter
public enum RoleType {

    SUPER_ADMIN("超级管理员", 10, SuperAdmin.class),
    PRINCIPAL("校级领导", 9, Principal.class),
    GRADE_DEAN("年级组长", 7, GradeDean.class),
    CLASS_ADVISOR("班主任", 5, ClassAdviser.class),
    MULTIPLE_CLASS_OBSERVER("多班级观察员", 5, MultipleClassObserver.class),
    COLLABORATIVE_USER("协作用户", 3, CollaborativeUser.class),
    PARENT_USER("家长用户", 1, ParentUser.class),
    FRESHMAN_PARENT("新生家长", 1, FreshmanParent.class),
    STUDENT_USER("学生用户", 0, StudentUser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonValue private final String desc;
    private final int level;
    private final Class<? extends Role> entityClass;

    RoleType(String desc, int level, Class<? extends Role> entityClass) {
        this.desc = desc;
        this.level = level;
        this.entityClass = entityClass;
    }

    /**
     * 根据提供的Json对象(以Map字典形式)构造一个Role实例。
     *
     * @param details 一个包含Role所需字段的Map字典对象，例如对于班主任来说是{ "gradeClass": {"gradeNo": 2027, "classNo": 3} }。
     * @return 构造出的Role实例
     * @throws CustomInvalidArgException 如果details字段的格式错误，或其包含的信息不足以构造该角色。
     */
    public Role fromJson(@NotNull Map<?, ?> details) throws CustomInvalidArgException {
        try {
            return objectMapper.convertValue(details, entityClass);
        } catch (IllegalArgumentException e) {
            throw new CustomInvalidArgException("details字段格式错误:\n" + e.getMessage());
        } catch (Exception e) {
            throw new CustomInvalidArgException("在构造Role时发生错误[通用Exception]:\n" + e.getMessage());
        }
    }

    /**
     * 根据角色名称获取对应的角色类型
     *
     * @param name 角色名称
     * @return 对应的角色类型
     * @throws CustomInvalidArgException 如果角色名称无效
     */
    @JsonCreator
    public static @NotNull RoleType fromDesc(String name) {
        for (RoleType role : RoleType.values()) {
            if (role.desc.equals(name)) {
                return role;
            }
        }
        throw new CustomInvalidArgException(String.format("角色类型 \"%s\" 无效", name));
    }
}
