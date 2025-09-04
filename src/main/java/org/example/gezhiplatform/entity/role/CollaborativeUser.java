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
 * 协作用户(可能包括社工、卫生室、心理老师、生涯导师)(默认权限等级为3, 角色类的实现类)
 * 学生范围：指定学生(学号）范围
 * 构造函数：规定能够管理哪些学生(学号)
 */
@Entity
public class CollaborativeUser extends Role {

    static {
        getField(Student.class, "stuNo", String.class)
            .orElseThrow(() -> new FieldNotFoundException("CollaborativeUser 角色需要依照学号(stuNo)进行筛选, 但未在Student类中找到String类型的stuNo字段。"));
    }

    @NotNull
    @ElementCollection
    private final Set<String> stuNos = new HashSet<>(); // 管理的学生(学号)

    public @NotNull Set<String> getStuNos() {
        return stuNos;
    }

    public CollaborativeUser() {}

    public CollaborativeUser(@NotNull Collection<String> stuNos) {
        this.stuNos.addAll(stuNos);
    }

    @Override
    public @NotNull Specification<Student> applyFilter() {
        return (root, _, _) ->
            root.get("stuNo").in(stuNos);
    }

    @Override
    public @NotNull RoleType getRoleType() {
        return RoleType.COLLABORATIVE_USER;
    }

    @Override
    public @NotNull String getRoleAndScope() {
        return "协作用户 - 允许查看学生范围: " +
               (stuNos.isEmpty() ? "无" : String.join(", ", stuNos));
    }

    @Override
    public boolean canAccessStudent(@NotNull Student student) {
        return stuNos.contains(student.getStuNo());
    }
}
