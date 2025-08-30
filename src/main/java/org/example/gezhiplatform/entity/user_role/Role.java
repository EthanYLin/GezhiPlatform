package org.example.gezhiplatform.entity.user_role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

/**
 * 角色(抽象类、实体类)
 * 角色规定了能够查看哪些学生(通过实现applyFilter()方法)
 * 角色规定了能够查看学生的哪些信息(通过设定level)
 * 注：教师-角色是一对多关系，而非多对多关系。即使两位教师都是班主任，也会根据管理的不同班级分别生成两个角色实例对象。
 * 依赖：applyFilter()的返回值依赖学生(Student)类
 */
@Entity
@Data
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Role {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 角色ID(由数据库自增)

    /**
     * 过滤学生的方法
     * @return Specification<Student>对象
     */
    public abstract @NotNull Specification<Student> applyFilter();

    /**
     * 获取角色类型
     * 角色类型是枚举类RoleType的一个实例, 例如: 超级管理员、校级领导、年级组长等。
     * @return 角色类型
     */
    @JsonIgnore
    public abstract @NotNull RoleType getRoleType();

    /**
     * 获取角色及其作用范围的字符串表示
     * 例如: "班主任: 高一(03)班" 或者 "年级组长: 2027届"
     * @return 角色及其作用范围的字符串
     */
    @JsonIgnore
    public abstract @NotNull String getRoleAndScope();


    /**
     * 检查当前角色是否有权限访问指定学生
     *
     * @param student 要检查的是否能访问该学生对象，不能为null。
     * @return 如果当前角色可以访问该学生，则返回true；否则返回false。
     */
    public abstract boolean canAccessStudent(@NotNull Student student);

}
