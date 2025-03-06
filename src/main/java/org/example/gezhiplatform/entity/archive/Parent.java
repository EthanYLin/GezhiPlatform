package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.springframework.lang.Nullable;

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
