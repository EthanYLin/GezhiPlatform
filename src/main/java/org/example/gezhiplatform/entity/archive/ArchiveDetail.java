package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.springframework.lang.Nullable;

@Entity
public class ArchiveDetail {

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
    private RelativePart relativePart; // 亲属信息

    @Nullable
    @OneToOne(cascade = CascadeType.ALL)
    private HealthPart healthPart; // 健康信息

}
