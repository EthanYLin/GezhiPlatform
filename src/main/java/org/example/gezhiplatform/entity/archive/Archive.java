package org.example.gezhiplatform.entity.archive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.archive.address_part.AddressPart;
import org.example.gezhiplatform.entity.archive.admission_part.AdmissionPart;
import org.example.gezhiplatform.entity.archive.family_part.FamilyPart;
import org.example.gezhiplatform.entity.archive.health_part.HealthPart;
import org.example.gezhiplatform.entity.archive.personal_part.PersonalPart;
import org.springframework.lang.Nullable;

/**
 * 学生档案
 * 包括个人信息、入学信息、地址信息、亲属信息、健康信息
 */
@Entity
@Data
@JsonPropertyOrder({"personalPart", "admissionPart", "addressPart", "familyPart", "healthPart"})
public class Archive {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 档案详情ID(由数据库自增)

    @Nullable
    @JsonIgnore
    @ToString.Exclude
    @OneToOne(mappedBy = "archive")
    private Student student; // 反向关联到学生

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @JsonTitle("个人信息")
    private PersonalPart personalPart; // 个人信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @JsonTitle("入学信息")
    private AdmissionPart admissionPart; // 入学信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @JsonTitle("地址信息")
    private AddressPart addressPart; // 地址信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @JsonTitle("亲属信息")
    private FamilyPart familyPart; // 亲属信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    @JsonTitle("健康信息")
    private HealthPart healthPart; // 健康信息

}
