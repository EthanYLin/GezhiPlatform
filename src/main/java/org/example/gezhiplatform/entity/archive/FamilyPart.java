package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生档案 - 亲属信息部分
 * 包括父母、其他亲属
 */
@Entity
public class FamilyPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private Parent father; // 父亲

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private Parent mother; // 母亲

    @ElementCollection
    @NonNull
    private List<Relative> otherRelatives = new ArrayList<>(); // 其他亲属

}
