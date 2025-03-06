package org.example.gezhiplatform.entity;

import jakarta.persistence.*;
import org.example.gezhiplatform.entity.user_role.Role;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户实体类(校级领导、教师、生涯导师、学生、家长等）
 */
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 用户ID(数据库内自增)

    @Nullable
    private String name; // 姓名

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
    private final List<Role> roles = new ArrayList<>(); // 具有的所有角色

    public User() {
    }

    public User(@Nullable String name) {
        this.name = name;
    }

    public User(@Nullable String name, @NonNull List<Role> roles) {
        this.name = name;
        this.roles.addAll(roles);
    }

    public User(@Nullable String name, @NonNull Role role) {
        this.name = name;
        this.roles.add(role);
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
        return "User{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", roles=" + roles +
               '}';
    }
}
