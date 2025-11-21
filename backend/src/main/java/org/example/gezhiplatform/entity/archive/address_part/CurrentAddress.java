package org.example.gezhiplatform.entity.archive.address_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.springframework.lang.Nullable;

/**
 * 居住地址类(包括省、市、区、详细地址、街道、居委)
 * 用于：学生档案 - 地址信息部分 - 居住地址
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"province", "city", "district", "detail", "street", "committee"})
public class CurrentAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id; // ID(由数据库自增)

    @Nullable
    @JsonTitle("省")
    @Size(max = 20, message = "现居地址的省名称长度不能超过20个字符")
    @JsonPropertyDescription("必填")
    private String province; // 省

    @Nullable
    @JsonTitle("市")
    @Size(max = 20, message = "现居地址的市名称长度不能超过20个字符")
    @JsonPropertyDescription("必填")
    private String city; // 市

    @Nullable
    @JsonTitle("区")
    @Size(max = 20, message = "现居地址的区名称长度不能超过20个字符")
    @JsonPropertyDescription("必填")
    private String district; // 区

    @Nullable
    @JsonTitle("详细地址")
    @Size(max = 100, message = "现居地址的详细地址长度不能超过100个字符")
    @JsonPropertyDescription("必填")
    private String detail; // 详细地址

    @Nullable
    @JsonTitle("街道")
    @Size(max = 20, message = "现居地址的街道名称长度不能超过20个字符")
    @JsonPropertyDescription("必填")
    private String street; // 街道

    @Nullable
    @JsonTitle("居委")
    @Size(max = 20, message = "现居地址的居委名称长度不能超过20个字符")
    @JsonPropertyDescription("必填")
    private String committee; // 居委

}
