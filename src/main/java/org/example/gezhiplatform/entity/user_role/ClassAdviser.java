package org.example.gezhiplatform.entity.user_role;

import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;

/**
 * 班主任(默认权限等级为5, 角色类的实现类)
 * 学生范围：指定年级与班级的学生
 * 构造函数：规定能够管理哪个年级-班级
 */
@Entity
public class ClassAdviser extends Role{

    static {
        getField(Student.class, "gradeClass", GradeClass.class)
            .orElseThrow(() -> new FieldNotFoundException("ClassAdviser 角色需要依照班级(gradeClass)进行筛选, 但未在Student类中找到GradeClass类型的gradeClass字段。"));
    }

    @Nullable
    private GradeClass gradeClass; // 管理的年级-班级

    public ClassAdviser() {}

    public @Nullable GradeClass getGradeClass() {
        return gradeClass;
    }

    public void setGradeClass(@Nullable GradeClass gradeClass) {
        this.gradeClass = gradeClass;
    }

    public ClassAdviser(@Nullable GradeClass gradeClass) {
        this.gradeClass = gradeClass;
    }

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (root, _, cb) ->
            gradeClass == null
                ? cb.disjunction()
                : cb.equal(root.get("gradeClass"), gradeClass);
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.CLASS_ADVISOR;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "班主任: " + (gradeClass != null ? gradeClass.toRelativeExpr() : "未指定班级");
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return gradeClass != null
            && student.getGradeClass().map(gradeClass::equals).orElse(false);
    }
}
