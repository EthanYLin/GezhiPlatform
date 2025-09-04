package org.example.gezhiplatform.entity.role;

import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import java.util.Objects;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;

/**
 * 年级组长(默认权限等级为7, 角色类的实现类)
 * 学生范围：指定年级的所有学生
 * 构造函数：规定能够管理哪个年级
 */
@Entity
public class GradeDean extends Role {

    @Nullable
    private Integer gradeNo; // 管理的届别

    static {
        getField(Student.class, "gradeClass", GradeClass.class)
            .orElseThrow(() -> new FieldNotFoundException("GradeDean 角色需要依照班级(gradeClass)进行筛选, 但未在Student类中找到GradeClass类型的gradeClass字段。"));
        getField(GradeClass.class, "gradeNo", Integer.class)
            .orElseThrow(() -> new FieldNotFoundException("GradeDean 角色需要依照年级号(gradeNo)进行筛选, 但未在GradeClass类中找到Integer类型的gradeNo字段。"));
    }

    public GradeDean() {}

    public GradeDean(@Nullable Integer gradeNo) {
        this.gradeNo = gradeNo;
    }

    public @Nullable Integer getGradeNo() {
        return gradeNo;
    }

    public void setGradeNo(@Nullable Integer gradeNo) {
        this.gradeNo = gradeNo;
    }

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (root, _, cb) ->
            gradeNo == null
                ? cb.disjunction()
                : cb.equal(root.get("gradeClass").get("gradeNo"), gradeNo);
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.GRADE_DEAN;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "年级组长: " + (gradeNo != null ? gradeNo + "届" : "未指定年级");
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return gradeNo != null &&
               student.getGradeClass().map(gradeClass -> Objects.equals(gradeClass.getGradeNo(), gradeNo)).orElse(false);
    }
}
