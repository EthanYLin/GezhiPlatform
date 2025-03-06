package org.example.gezhiplatform.entity;

import jakarta.persistence.*;
import org.example.gezhiplatform.entity.teacher_role.Role;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 教师实体类
 */
@Entity
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 教师ID(数据库内自增)

    @Nullable
    private String name; // 姓名

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
    private final List<Role> roles = new ArrayList<>(); // 具有的所有角色

    public Teacher() {
    }

    public Teacher(@Nullable String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @NonNull List<Role> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "Teacher{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", roles=" + roles +
               '}';
    }
}
