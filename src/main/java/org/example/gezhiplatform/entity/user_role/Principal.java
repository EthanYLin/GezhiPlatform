package org.example.gezhiplatform.entity.user_role;

import org.example.gezhiplatform.entity.Student;
import org.springframework.data.jpa.domain.Specification;

/**
 * 校级领导(高级管理员)(默认权限等级为9, 角色类的实现类)
 * 学生范围：所有学生
 */
public class Principal extends Role{

    public static final int DEFAULT_LEVEL = 9; // 默认权限等级（校级领导=9）

    public Principal() {
        this.setLevel(DEFAULT_LEVEL);
    }

    @Override
    public Specification<Student> applyFilter() {
        return (_, _, cb) -> cb.conjunction();
    }
}
