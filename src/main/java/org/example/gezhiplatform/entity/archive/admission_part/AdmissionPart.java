package org.example.gezhiplatform.entity.archive.admission_part;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.example.gezhiplatform.entity.enums.AdmissionPath;
import org.example.gezhiplatform.entity.enums.District;
import org.springframework.lang.Nullable;

/**
 * 学生档案 - 录取信息部分
 */
@Entity
public class AdmissionPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Nullable
    private District juniorHighSchoolDistrict; // 初中所在区

    @Nullable
    private String juniorHighSchoolName; // 初中名称

    @Nullable
    private AdmissionPath admissionPath; // 录取方式

}
