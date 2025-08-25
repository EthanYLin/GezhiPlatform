package org.example.gezhiplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.gezhiplatform.entity.user_role.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
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

    @Nullable
    private String username; // 用户名(登录用)

    @Nullable
    private String encryptedPassword; // 加密后的密码(登录用)

    private boolean isLocked = false; // 是否被锁定

    private boolean isEnabled = false; // 是否启用(如果当前密码是默认的初始密码, 则不启用, 只能进行修改密码功能)

    @Nullable
    private LocalDateTime lastLoginTime; // 上次登录时间


    public User() {}

    public User(@Nullable String name, @NotNull List<Role> roles) {
        this.name = name;
        this.roles.addAll(roles);
    }

    public User(@Nullable String name, @NotNull Role role) {
        this.name = name;
        this.roles.add(role);
    }

    /**
     * 获取用户权限过滤规格
     * <p>
     * 根据用户拥有的所有角色，生成一个用于过滤学生数据的JPA Specification。
     * 该方法将用户的所有角色权限进行合并，<b>使用"或"逻辑连接</b>，
     * 即用户可以查看任意一个角色权限范围内的学生数据。
     *
     * @return 学生数据过滤的JPA Specification
     *         <ul>
     *           <li>如果用户没有任何角色，返回null（无权限过滤，查询结果为空）</li>
     *           <li>如果用户有角色，返回所有角色权限的并集过滤条件</li>
     *         </ul>
     */
    public Specification<Student> getSpec() {
        if (roles.isEmpty()) return Specification.where(null);
        return Specification.anyOf(
            roles.stream().map(Role::applyFilter).toList()
        );
    }
}
