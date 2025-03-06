package org.example.gezhiplatform.entity.teacher_role;

import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

/**
 * 班主任(默认权限等级为5, 角色类的实现类)
 * 学生范围：指定年级与班级的学生
 * 构造函数：规定能够管理哪个年级-班级
 */
@Entity
public class ClassAdviser extends Role{

    public static final int DEFAULT_LEVEL = 5; // 默认权限等级（班主任=5）

    static {
        getField(Student.class, "gradeClass", GradeClass.class)
            .orElseThrow(() -> new FilterSettingException("未找到gradeClass字段"));
    }

    @Nullable
    private GradeClass gradeClass; // 管理的年级-班级

    public @Nullable GradeClass getGradeClass() {
        return gradeClass;
    }

    public void setGradeClass(@Nullable GradeClass gradeClass) {
        this.gradeClass = gradeClass;
    }

    public ClassAdviser() {
        this.setLevel(DEFAULT_LEVEL);
    }

    public ClassAdviser(@Nullable GradeClass gradeClass) {
        this.setLevel(DEFAULT_LEVEL);
        this.gradeClass = gradeClass;
    }

    @Override
    public Specification<Student> applyFilter() {
        return (root, _, cb) ->
            cb.equal(root.get("gradeClass"), gradeClass);
    }
}
