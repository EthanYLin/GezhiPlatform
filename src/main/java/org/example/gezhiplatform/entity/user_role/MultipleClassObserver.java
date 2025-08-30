package org.example.gezhiplatform.entity.user_role;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;

/**
 * 多班级观察员(角色类的实现类, 主要用于测试)
 * 学生范围：指定多个年级-班级的学生
 * 构造函数：规定能够管理哪些年级-班级
 */
@Entity
public class MultipleClassObserver extends Role {

    @NotNull
    @ElementCollection
    private final Set<GradeClass> gradeClasses = new HashSet<>(); // 管理的年级-班级

    static {
        getField(Student.class, "gradeClass", GradeClass.class)
            .orElseThrow(() -> new FieldNotFoundException("MultipleClassObserver 角色需要依照班级(gradeClass)进行筛选, 但未在Student类中找到GradeClass类型的gradeClass字段。"));
    }

    public @NotNull Set<GradeClass> getGradeClasses() {
        return gradeClasses;
    }

    public MultipleClassObserver() {}

    public MultipleClassObserver(Collection<GradeClass> gradeClasses) {
        this.gradeClasses.addAll(gradeClasses);
    }

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (root, _, _) ->
            root.get("gradeClass").in(gradeClasses);
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.MULTIPLE_CLASS_OBSERVER;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "多班级观察员: " + gradeClasses.stream().map(GradeClass::toRelativeExpr).toList();
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return student.getGradeClass().map(gradeClasses::contains).orElse(false);
    }
}
