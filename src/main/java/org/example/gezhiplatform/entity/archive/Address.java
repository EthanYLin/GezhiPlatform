package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.Embeddable;
import org.springframework.lang.Nullable;


@Embeddable
public class Address {

    @Nullable
    private String province; // 省

    @Nullable
    private String city; // 市

    @Nullable
    private String district; // 区

    @Nullable
    private String detail; // 详细地址

    @Nullable
    private String street; // 街道

    @Nullable
    private String committee; // 居委

}
