package org.example.gezhiplatform.entity.archive.health_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.example.gezhiplatform.entity.enums.HealthStatus;
import org.springframework.lang.Nullable;

/**
 * 健康详情类(包括健康状况、健康问题、服药情况、治疗情况)
 * 用于：学生档案 - 健康信息部分 - 身体健康
 * 用于：学生档案 - 健康信息部分 - 心理健康
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"healthStatus", "healthIssue", "medicationUse", "ongoingTreatment"})
public class HealthCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id; // ID(由数据库自增)

    @Nullable
    @JsonTitle("健康状况")
    private HealthStatus healthStatus; // 健康状况(健康或关注)

    @Nullable
    @JsonTitle("健康问题")
    @Size(max = 1000, message = "健康问题长度不能超过1000个字符")
    private String healthIssue; // 健康问题

    @Nullable
    @JsonTitle("服药情况")
    @Size(max = 1000, message = "服药情况长度不能超过1000个字符")
    private String medicationUse; // 服药情况

    @Nullable
    @JsonTitle("治疗情况")
    @Size(max = 1000, message = "治疗情况长度不能超过1000个字符")
    private String ongoingTreatment; // 治疗情况

}
