package org.example.gezhiplatform.entity.role;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
 * 新生家长用户(默认权限等级为1, 角色类的实现类)
 * 学生范围：指定学生(学号）范围
 * 构造函数：规定能够查看哪些学生(学号)
 */
@Entity
public class FreshmanParent extends Role {

    static {
        getField(Student.class, "stuNo", String.class)
            .orElseThrow(() -> new FieldNotFoundException("FreshmanParent 角色需要依照学号(stuNo)进行筛选, 但未在Student类中找到String类型的stuNo字段。"));
    }

    @NotNull
    @ElementCollection
    private final Set<String> stuNos = new HashSet<>(); // 能够查看的学生(学号)

    public FreshmanParent() {}

    public FreshmanParent(@NotNull Collection<String> stuNos) {
        this.stuNos.addAll(stuNos);
    }

    public @NotNull Set<String> getStuNos() {
        return stuNos;
    }

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (root, _, _) ->
            root.get("stuNo").in(stuNos);
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.FRESHMAN_PARENT;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "新生家长 - 孩子学号: " +
               (stuNos.isEmpty() ? "暂未绑定" : String.join(", ", stuNos));
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return stuNos.contains(student.getStuNo());
    }
}
