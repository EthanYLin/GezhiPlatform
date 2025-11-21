package org.example.gezhiplatform.entity.archive.family_part;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonIncludeMethod;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.example.gezhiplatform.service.metadata.Identifiable;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;
/**
 * 其他亲属类(包括姓名、出生年份、工作/就学信息)
 * 用于：用于：学生档案 - 亲属信息部分 - 其他亲属
 */
@Entity
@Data
@JsonPropertyOrder({"id", "name", "birthYear", "age", "info"})
public class Relative implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Nullable
    @JsonTitle("姓名")
    @Size(max = 20, message = "亲属姓名长度不能超过20个字符")
    private String name; // 亲属姓名

    @Nullable
    @JsonTitle("出生年份")
    @Min(value = 1900, message = "亲属出生年份必须大于1900年")
    @Max(value = 2100, message = "亲属出生年份必须小于2100年")
    private Integer birthYear; // 出生年份

    @Nullable
    @JsonTitle("工作/就学信息")
    @Size(max = 100, message = "亲属工作/就学信息长度不能超过100个字符")
    private String info; // 工作/就学信息

    @JsonProperty(value = "age", access = JsonProperty.Access.READ_ONLY)
    @JsonTitle("年龄")
    @JsonIncludeMethod
    public @Nullable Integer calcAge() {
        return Optional.ofNullable(birthYear)
                       .map(year -> LocalDateTime.now().getYear() - year)
                       .filter(age -> age >= 0)
                       .orElse(null);
    }

}
