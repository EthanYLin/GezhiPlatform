package org.example.gezhiplatform.entity.archive.family_part;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.lang.Nullable;

/**
 * 双亲类(包括姓名、手机号码、工作单位)
 * 用于：学生档案 - 亲属信息部分 - 父亲
 * 用于：学生档案 - 亲属信息部分 - 母亲
 */
@Entity
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Nullable
    private String name; // 姓名

    @Nullable
    private String mobile; // 手机号码

    @Nullable
    private String workUnit; // 工作单位

}
