package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.springframework.lang.Nullable;

/**
 * 学生档案
 * 包括个人信息、入学信息、地址信息、亲属信息、健康信息
 */
@Entity
public class Archive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 档案详情ID(由数据库自增)

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    private PersonalPart personalPart; // 个人信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    private AdmissionPart admissionPart; // 入学信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    private AddressPart addressPart; // 地址信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    private FamilyPart familyPart; // 亲属信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    private HealthPart healthPart; // 健康信息

}
