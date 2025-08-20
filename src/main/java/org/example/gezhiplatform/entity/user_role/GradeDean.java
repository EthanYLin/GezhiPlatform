package org.example.gezhiplatform.entity.user_role;

import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

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
            .orElseThrow(() -> new FilterSettingException("未找到gradeClass字段"));
        getField(GradeClass.class, "gradeNo", Integer.class)
            .orElseThrow(() -> new FilterSettingException("未找到gradeNo字段"));
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
    public @NotNull Specification<Student> applyFilter() throws FilterSettingException{
        return (root, _, cb) ->
            cb.equal(root.get("gradeClass").get("gradeNo"), gradeNo);
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.GRADE_DEAN;
    }
}
