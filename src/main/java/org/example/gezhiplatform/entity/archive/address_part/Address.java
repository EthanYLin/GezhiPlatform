package org.example.gezhiplatform.entity.archive.address_part;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * 地址类(包括省、市、区、详细地址、街道、居委)
 * 用于：学生档案 - 地址信息部分 - 户籍地址
 * 用于：学生档案 - 地址信息部分 - 居住地址
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

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
