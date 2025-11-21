package org.example.gezhiplatform.entity.archive.health_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.gezhiplatform.annotation.JsonIncludeMethod;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.example.gezhiplatform.service.metadata.Identifiable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 健康详情类(包括健康问题、服药情况、治疗情况)
 * 用于：学生档案 - 健康信息部分 - 身体健康
 * 用于：学生档案 - 健康信息部分 - 心理健康
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonPropertyOrder({"id", "healthIssue", "medicationUse", "ongoingTreatment", "createdAt", "updatedAt"})
public class HealthCondition implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

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

    @CreatedDate
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt; // 创建时间

    @LastModifiedDate
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private LocalDateTime updatedAt; // 更新时间

    @JsonProperty(value = "createdAt", access = JsonProperty.Access.READ_ONLY)
    @JsonTitle("创建时间")
    @JsonIncludeMethod
    public @Nullable String getCreatedAtStr() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    @JsonProperty(value = "updatedAt", access = JsonProperty.Access.READ_ONLY)
    @JsonTitle("更新时间")
    @JsonIncludeMethod
    public @Nullable String getUpdatedAtStr() {
        return updatedAt != null ? updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

}
