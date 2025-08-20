package org.example.gezhiplatform.entity.user_role;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 多班级观察员(角色类的实现类, 主要用于测试)
 * 学生范围：指定多个年级-班级的学生
 * 构造函数：规定能够管理哪些年级-班级
 */
@Entity
public class MultipleClassObserver extends Role {

    @NotNull
    @ElementCollection
    private final List<GradeClass> gradeClasses = new ArrayList<>(); // 管理的年级-班级

    static {
        getField(Student.class, "gradeClass", GradeClass.class)
            .orElseThrow(() -> new FilterSettingException("未找到gradeClass字段"));
    }

    public @NotNull List<GradeClass> getGradeClasses() {
        return gradeClasses;
    }

    public MultipleClassObserver() {}

    public MultipleClassObserver(List<GradeClass> gradeClasses) {
        this.gradeClasses.addAll(gradeClasses);
    }

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (root, _, _) -> {
            var gradeClass = root.get("gradeClass");
            return gradeClass.in(gradeClasses);
        };
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.MULTIPLE_CLASS_OBSERVER;
    }
}
