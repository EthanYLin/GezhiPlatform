package org.example.gezhiplatform.entity.archive.admission_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.example.gezhiplatform.entity.enums.AdmissionPath;
import org.example.gezhiplatform.entity.enums.District;
import org.springframework.lang.Nullable;

/**
 * 学生档案 - 录取信息部分
 */
@Entity
@Data
@JsonPropertyOrder({"juniorHighSchoolDistrict", "juniorHighSchoolName", "admissionPath"})
public class AdmissionPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id; // ID(由数据库自增)

    @Nullable
    @JsonTitle("初中所在区")
    private District juniorHighSchoolDistrict; // 初中所在区

    @Nullable
    @JsonTitle("初中学校")
    @Size(max = 100, message = "初中名称长度不能超过100个字符")
    private String juniorHighSchoolName; // 初中名称

    @Nullable
    @JsonTitle("录取方式")
    private AdmissionPath admissionPath; // 录取方式

}
