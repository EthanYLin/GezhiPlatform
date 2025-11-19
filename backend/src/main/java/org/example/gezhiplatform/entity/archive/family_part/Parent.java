package org.example.gezhiplatform.entity.archive.family_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.springframework.lang.Nullable;

/**
 * 双亲类(包括姓名、手机号码、工作单位)
 * 用于：学生档案 - 亲属信息部分 - 父亲
 * 用于：学生档案 - 亲属信息部分 - 母亲
 */
@Entity
@Data
@JsonPropertyOrder({"name", "mobile", "workUnit"})
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id; // ID(由数据库自增)

    @Nullable
    @JsonTitle("姓名")
    @Size(max = 20, message = "父/母亲姓名长度不能超过20个字符")
    private String name; // 姓名

    @Nullable
    @JsonTitle("手机号码")
    @Pattern(regexp = "^1\\d{10}$", message = "父/母亲手机号格式不正确(仅支持中国大陆手机号)")
    private String mobile; // 手机号码

    @Nullable
    @JsonTitle("工作单位")
    @Size(max = 100, message = "父/母亲工作单位长度不能超过100个字符")
    private String workUnit; // 工作单位

}
