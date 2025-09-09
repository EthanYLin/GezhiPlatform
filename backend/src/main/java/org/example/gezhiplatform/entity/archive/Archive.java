package org.example.gezhiplatform.entity.archive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.archive.address_part.AddressPart;
import org.example.gezhiplatform.entity.archive.admission_part.AdmissionPart;
import org.example.gezhiplatform.entity.archive.family_part.FamilyPart;
import org.example.gezhiplatform.entity.archive.health_part.HealthPart;
import org.example.gezhiplatform.entity.archive.personal_part.PersonalPart;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

/**
 * 学生档案
 * 包括个人信息、入学信息、地址信息、家庭成员、健康申明部分
 */
@Entity
@Data
@NoArgsConstructor
@JsonPropertyOrder({"personalPart", "admissionPart", "addressPart", "familyPart", "healthPart"})
public class Archive {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 档案详情ID(由数据库自增)

    @NotNull
    @JsonIgnore
    @ToString.Exclude
    @OneToOne(mappedBy = "archive", optional = false)
    private Student student; // 反向关联到学生

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @Valid
    @JsonMerge
    @JsonTitle("个人信息")
    private PersonalPart personalPart; // 个人信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @Valid
    @JsonMerge
    @JsonTitle("入学信息")
    private AdmissionPart admissionPart; // 入学信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @Valid
    @JsonMerge
    @JsonTitle("地址信息")
    private AddressPart addressPart; // 地址信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @Valid
    @JsonMerge
    @JsonTitle("家庭成员")
    private FamilyPart familyPart; // 家庭成员

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @Valid
    @JsonMerge
    @JsonTitle("健康申明")
    private HealthPart healthPart; // 健康申明

}
