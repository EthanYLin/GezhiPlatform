package org.example.gezhiplatform.entity.archive.family_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生档案 - 亲属信息部分
 * 包括父母、直系亲属
 */
@Entity
@Data
@JsonPropertyOrder({"father", "mother", "otherRelatives"})
public class FamilyPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id; // ID(由数据库自增)

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    @Valid
    @JsonMerge
    @JsonTitle("父亲")
    private Parent father; // 父亲

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    @Valid
    @JsonMerge
    @JsonTitle("母亲")
    private Parent mother; // 母亲

    @ElementCollection
    @NotNull
    @Valid
    @JsonTitle("直系亲属")
    private List<Relative> otherRelatives = new ArrayList<>(); // 直系亲属

}
