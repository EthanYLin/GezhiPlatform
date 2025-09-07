package org.example.gezhiplatform.entity.archive.health_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.springframework.lang.Nullable;

/**
 * 学生档案 - 健康信息部分
 */
@Entity
@Data
@JsonPropertyOrder({"physicalCondition", "mentalCondition"})
public class HealthPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id; // ID(由数据库自增)

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    @Valid
    @JsonMerge
    @JsonTitle("身体状况")
    private HealthCondition physicalCondition; // 身体状况

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    @Valid
    @JsonMerge
    @JsonTitle("心理状况")
    private HealthCondition mentalCondition; // 心理状况

}
