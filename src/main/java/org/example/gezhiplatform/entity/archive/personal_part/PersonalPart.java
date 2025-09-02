package org.example.gezhiplatform.entity.archive.personal_part;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonIncludeMethod;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.example.gezhiplatform.annotation.RIN;
import org.example.gezhiplatform.entity.enums.Gender;
import org.example.gezhiplatform.entity.enums.Nation;
import org.example.gezhiplatform.entity.enums.PoliticalStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 学生档案 - 个人信息部分
 */
@Entity
@Data
@JsonPropertyOrder({"mobile", "nation", "RIN", "gender", "birthDate", "politicalStatus"})
public class PersonalPart {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @RIN
    @Nullable
    @JsonProperty("RIN")
    @JsonAlias({"rin"})
    @JsonTitle("身份证号")
    @JsonPropertyDescription("若最后一位为X，X需要大写。")
    private String RIN; // 居民身份证号

    @JsonTitle("手机号码")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确(仅支持中国大陆手机号)")
    @Nullable
    private String mobile; // 手机号码

    @JsonTitle("民族")
    @Nullable
    private Nation nation; // 民族

    @JsonTitle("政治面貌")
    @Nullable
    private PoliticalStatus politicalStatus; // 政治面貌

    // 性别与出生日期从身份证号中解析得到

    public Optional<LocalDate> getBirthDate() {
        if (this.RIN == null || this.RIN.length() != 18) {
            return Optional.empty();
        }
        try {
            int year = Integer.parseInt(this.RIN.substring(6, 10));
            int month = Integer.parseInt(this.RIN.substring(10, 12));
            int day = Integer.parseInt(this.RIN.substring(12, 14));
            return Optional.of(LocalDate.of(year, month, day));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @JsonProperty(value = "birthDate", access = JsonProperty.Access.READ_ONLY)
    @JsonTitle("出生日期")
    @JsonIncludeMethod
    public @Nullable String getBirthDateStr() {
        return this.getBirthDate().map(LocalDate::toString).orElse(null);
    }

    @JsonProperty(value = "gender", access = JsonProperty.Access.READ_ONLY)
    @JsonTitle("性别")
    @JsonIncludeMethod
    public @NotNull Gender getGender() {
        if (this.RIN == null || this.RIN.length() != 18) {
            return Gender.UNKNOWN;
        }
        try {
            int genderBit = Integer.parseInt(this.RIN.substring(16, 17));
            return genderBit % 2 == 0 ? Gender.FEMALE : Gender.MALE;
        } catch (NumberFormatException e) {
            return Gender.UNKNOWN;
        }
    }

}
