package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.gezhiplatform.entity.enums.RoleType;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 权限组ID(由数据库自增)

    @NotNull
    @Column(unique = true, nullable = false)
    private String name; // 权限组名称

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<RoleType> roleTypes = new HashSet<>(); // 权限组中包含的角色类型

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> allowedReadableJsonPaths = new HashSet<>(); // 该权限组可见的JSON Path

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> allowedWritableJsonPaths = new HashSet<>(); // 该权限组可编辑的JSON Path

}
