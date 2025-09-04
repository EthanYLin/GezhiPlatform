package org.example.gezhiplatform.entity.role;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;

/**
 * 学生用户(默认权限等级为0, 角色类的实现类)
 * 学生范围：单个学生(学号）
 * 构造函数：该学生的学号
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentUser extends Role {

    static {
        getField(Student.class, "stuNo", String.class)
            .orElseThrow(() -> new FieldNotFoundException("StudentUser角色需要依照学号(stuNo)进行筛选, 但未在Student类中找到String类型的stuNo字段。"));
    }

    @Nullable
    private String stuNo; // 学生学号

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (root, _, cb) ->
            stuNo == null
                ? cb.disjunction()
                : cb.equal(root.get("stuNo"), stuNo);
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.STUDENT_USER;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "学生用户: 学号 " + (stuNo != null ? stuNo : "暂未绑定");
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return stuNo != null && stuNo.equals(student.getStuNo());
    }
}
