package org.example.gezhiplatform.entity.user_role;

import jakarta.persistence.*;
import org.example.gezhiplatform.entity.Student;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.Optional;

/*
 * 目前的角色有：
 * 10级: 超级管理员
 * 9级: 校级领导(高级管理员)
 * 7级: 年级组长(普通管理员)
 * 5级: 班主任
 * 3级: 协作用户(可能包括社工、卫生室、心理老师、生涯导师)
 */

/**
 * 角色(抽象类、实体类)
 * 角色规定了能够查看哪些学生(通过实现applyFilter()方法)
 * 角色规定了能够查看学生的哪些信息(通过设定level)
 * 注：教师-角色是一对多关系，而非多对多关系。即使两位教师拥有同样的角色权限，也会分别生成两个角色实例对象。
 * 依赖：applyFilter()的返回值依赖学生(Student)类
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 角色ID(由数据库自增)

    @NotNull
    @Column(nullable = false)
    private Integer level = 0; // 等级(数字越大，权限越大，默认为0)

    @Nullable
    private String remark; // 角色备注

    /**
     * 按名称和类型从指定的类检索字段。
     *
     * @param clazz 要搜索字段的类
     * @param fieldName 要查找的字段的名称
     * @param fieldType 要查找的字段类型
     * @return 如果找到该字段，则返回Optional<该字段>，否则返回空Optional
     */
    protected static Optional<Field> getField(Class<?> clazz, String fieldName, Class<?> fieldType) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(fieldName) && field.getType().equals(fieldType)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    /**
     * 过滤学生的方法
     * @return Specification<Student>对象
     */
    public abstract Specification<Student> applyFilter();

    public Long getId() {
        return id;
    }

    public void setId(@NotNull Long id) {
        this.id = id;
    }

    public @NotNull Integer getLevel() {
        return level;
    }

    public void setLevel(@NotNull Integer level) {
        this.level = level;
    }

    public @Nullable String getRemark() {
        return remark;
    }

    public void setRemark(@Nullable String remark) {
        this.remark = remark;
    }
}
