package org.example.gezhiplatform.entity.user_role;

import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

/**
 * 校级领导(高级管理员)(默认权限等级为9, 角色类的实现类)
 * 学生范围：所有学生
 */
@Entity
public class Principal extends Role{

    public Principal() {}

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (_, _, cb) -> cb.conjunction();
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.PRINCIPAL;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "校级领导";
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return true;
    }
}
