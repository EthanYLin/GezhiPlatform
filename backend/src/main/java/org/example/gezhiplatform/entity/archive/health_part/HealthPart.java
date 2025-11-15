package org.example.gezhiplatform.entity.archive.health_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @NotNull
    @Valid
    @JsonTitle("身体状况")
    private List<HealthCondition> physicalCondition = new ArrayList<>(); // 身体状况

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @NotNull
    @Valid
    @JsonTitle("心理状况")
    private List<HealthCondition> mentalCondition = new ArrayList<>(); // 心理状况

}
