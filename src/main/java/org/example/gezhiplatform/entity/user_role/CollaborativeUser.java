package org.example.gezhiplatform.entity.user_role;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import org.example.gezhiplatform.entity.Student;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 协作用户(可能包括社工、卫生室、心理老师、生涯导师)(默认权限等级为3, 角色类的实现类)
 * 学生范围：指定学生(学号）范围
 * 构造函数：规定能够管理哪些学生(学号)
 */
@Entity
public class CollaborativeUser extends Role{

    public static final int DEFAULT_LEVEL = 3; // 默认权限等级（协作用户=3）

    static {
        getField(Student.class, "stuNo", String.class)
            .orElseThrow(() -> new FilterSettingException("未找到学号(stuNo)字段"));
    }

    @NotNull
    @ElementCollection
    private final List<String> stuNos = new ArrayList<>(); // 管理的学生(学号)

    public @NotNull List<String> getStuNos() {
        return stuNos;
    }

    public CollaborativeUser() {
        this.setLevel(DEFAULT_LEVEL);
    }

    public CollaborativeUser(@NotNull List<String> stuNos) {
        this.setLevel(DEFAULT_LEVEL);
        this.stuNos.addAll(stuNos);
    }

    @Override
    public Specification<Student> applyFilter() {
        return (root, _, _) -> {
            var stuNo = root.get("stuNo");
            return stuNo.in(stuNos);
        };
    }
}
