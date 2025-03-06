package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Entity
public class RelativePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Embedded
    @Nullable
    private Parent father; // 父亲

    @Embedded
    @Nullable
    private Parent mother; // 母亲

    @ElementCollection
    @NonNull
    private List<Relative> otherRelatives = new ArrayList<>(); // 其他亲属

}
