package org.example.gezhiplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.gezhiplatform.entity.user_role.Role;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户实体类(校级领导、教师、生涯导师、学生、家长等）
 */
@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 用户ID(数据库内自增)

    @Nullable
    private String name; // 姓名

    @NotNull
    @OneToMany(cascade = CascadeType.ALL)
    private final List<Role> roles = new ArrayList<>(); // 具有的所有角色(角色专属于该用户, 不能被其他用户共享)

    public User() {}

    public User(@Nullable String name) {
        this.name = name;
    }

    public User(@Nullable String name, @NotNull List<Role> roles) {
        this.name = name;
        this.roles.addAll(roles);
    }

    public User(@Nullable String name, @NotNull Role role) {
        this.name = name;
        this.roles.add(role);
    }
}
