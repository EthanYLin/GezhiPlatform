package org.example.gezhiplatform.entity.user_role;

import org.example.gezhiplatform.entity.Student;
import org.springframework.data.jpa.domain.Specification;

/**
 * 超级管理员(默认权限等级为10, 角色类的实现类)
 * 学生范围：所有学生
 */
public class SuperAdmin extends Role{

    public static final int DEFAULT_LEVEL = 10; // 默认权限等级（超级管理员=10）

    public SuperAdmin() {
        this.setLevel(DEFAULT_LEVEL);
    }

    @Override
    public Specification<Student> applyFilter() {
        return (_, _, cb) -> cb.conjunction();
    }
}
