package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.Embeddable;
import org.springframework.lang.Nullable;

@Embeddable
public class Parent {

    @Nullable
    private String name; // 姓名

    @Nullable
    private String mobile; // 手机号码

    @Nullable
    private String workUnit; // 工作单位

}
