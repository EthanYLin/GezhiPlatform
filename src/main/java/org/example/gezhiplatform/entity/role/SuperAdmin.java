package org.example.gezhiplatform.entity.role;

import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

/**
 * 超级管理员(默认权限等级为10, 角色类的实现类)
 * 学生范围：所有学生
 */
@Entity
public class SuperAdmin extends Role{

    public SuperAdmin() {
    }

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (_, _, cb) -> cb.conjunction();
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.SUPER_ADMIN;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "超级管理员";
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return true;
    }
}
