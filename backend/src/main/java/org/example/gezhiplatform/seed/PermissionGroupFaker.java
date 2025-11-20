package org.example.gezhiplatform.seed;

import org.example.gezhiplatform.DTO.archive.FieldMetadata;
import org.example.gezhiplatform.entity.archive.PermissionGroup;
import org.example.gezhiplatform.entity.archive.ValidationExpr;
import org.example.gezhiplatform.entity.enums.RoleType;

import java.util.*;
import java.util.stream.Collectors;


public class PermissionGroupFaker {

    public static final String PERSONAL_PART = "$.personalPart";
    public static final String ADMISSION_PART = "$.admissionPart";
    public static final String ADDRESS_PART = "$.addressPart";
    public static final String FAMILY_PART = "$.familyPart";
    public static final String HEALTH_PART = "$.healthPart";

    public static final String RIN = "$.personalPart.rin";
    public static final String ADMISSION_PATH = "$.admissionPart.admissionPath";

    public static final String RELATIVES_ARRAY = "$.familyPart.otherRelatives";
    public static final String MENTAL_COND_ARRAY = "$.healthPart.mentalCondition";
    public static final String PHY_COND_ARRAY = "$.healthPart.physicalCondition";
    public static final Set<String> HEALTH_COND_ARRAYS = Set.of(MENTAL_COND_ARRAY, PHY_COND_ARRAY);

    private final Map<String, FieldMetadata> fieldMetadata;

    public PermissionGroupFaker(Map<String, FieldMetadata> fieldMetadata) {
        this.fieldMetadata = fieldMetadata;
    }

    public List<PermissionGroup> defaultGroups() {
        var result = new LinkedList<PermissionGroup>();

        // 学生权限组(仅能查看部分基础信息)
        result.add(
            PermissionGroupBuilder
                .nameOf("学生权限组")
                .description("可查看基础个人信息、居住地址、父亲母亲姓名")
                .roleType(RoleType.STUDENT_USER)
                .readable(new Paths().beginsWith(PERSONAL_PART).except(RIN))
                .readable("$.addressPart.currentAddress")
                .readable("$.familyPart.father.name")
                .readable("$.familyPart.mother.name")
                .build()
        );

        // 老生家长权限组(能查看除身份证号外的个人信息、除录取信息之外的其他信息、仅能添加健康声明)
        result.add(
            PermissionGroupBuilder
                .nameOf("家长权限组")
                .description("可查看除身份证号外的个人信息、除录取信息之外的其他信息、仅能添加健康声明")
                .roleType(RoleType.PARENT_USER)
                .readable(new Paths().all().except(RIN).exceptBeginWith(ADMISSION_PART))
                .writable(new Paths().beginsWith(HEALTH_PART))
                .canArrayAdd(HEALTH_COND_ARRAYS)
                .build()
        );

        // 老生家长填报时段权限组(能修改学生手机号、政治面貌、地址信息、家庭信息)
        result.add(
            PermissionGroupBuilder
                .nameOf("家长(填报时段)权限组")
                .description("可修改学生手机号、政治面貌、地址信息、家庭信息")
                .roleType(RoleType.PARENT_USER)
                .writable("$.personalPart.mobile")
                .writable("$.personalPart.politicalStatus")
                .writable(new Paths().beginsWith(ADDRESS_PART))
                .writable(new Paths().beginsWith(FAMILY_PART))
                .arrayFullControl(RELATIVES_ARRAY)
                .build()
        );

        // 新生家长权限组(能查看并修改除录取方式之外的所有信息)
        result.add(
            PermissionGroupBuilder
                .nameOf("新生家长权限组")
                .description("可查看并修改除录取方式之外的所有信息")
                .roleType(RoleType.FRESHMAN_PARENT)
                .readable(new Paths().all().except(ADMISSION_PATH))
                .writable(new Paths().all().except(ADMISSION_PATH))
                .arrayFullControl(RELATIVES_ARRAY)
                .arrayFullControl(HEALTH_COND_ARRAYS)
                .build()
        );

        // 班主任权限组(查看: 除录取方式、健康声明外的所有信息; 修改: 除身份证号、录取信息、健康声明外的所有信息)
        result.add(
            PermissionGroupBuilder
                .nameOf("班主任权限组")
                .description("查看: 除录取方式、健康声明外的所有信息; 修改: 除身份证号、录取信息、健康声明外的所有信息")
                .roleType(RoleType.CLASS_ADVISOR)
                .readable(new Paths().all().except(ADMISSION_PATH).exceptBeginWith(HEALTH_PART))
                .writable(new Paths().all().except(RIN).except(ADMISSION_PATH).exceptBeginWith(HEALTH_PART))
                .arrayFullControl(RELATIVES_ARRAY)
                .build()
        );

        // 年级组长权限组(查看: 除入学信息、父母亲单位、健康声明外的所有信息; 修改: 仅可修改除身份证号外的个人信息)
        result.add(
            PermissionGroupBuilder
                .nameOf("年级组长权限组")
                .description("查看: 除入学信息、父母亲单位、健康声明外的所有信息; 修改: 仅可修改除身份证号外的个人信息")
                .roleType(RoleType.GRADE_DEAN)
                .readable(new Paths().all()
                    .exceptBeginWith(ADMISSION_PART)
                    .except("$.familyPart.father.workUnit")
                    .except("$.familyPart.mother.workUnit")
                    .exceptBeginWith(HEALTH_PART)
                )
                .writable(new Paths().beginsWith(PERSONAL_PART).except(RIN))
                .build()
        );

        // 协作用户权限组(仅可查看性别、出生日期、手机号、居住地址、父母亲姓名及手机号)
        result.add(
            PermissionGroupBuilder
                .nameOf("协作用户权限组")
                .description("仅可查看性别、出生日期、手机号、居住地址、父母亲姓名及手机号")
                .roleType(RoleType.COLLABORATIVE_USER)
                .readable("$.personalPart.gender")
                .readable("$.personalPart.birthDate")
                .readable("$.personalPart.mobile")
                .readable("$.addressPart.currentAddress")
                .readable("$.familyPart.father.name")
                .readable("$.familyPart.father.mobile")
                .readable("$.familyPart.mother.name")
                .readable("$.familyPart.mother.mobile")
                .build()
        );

        // 校级领导权限组(能查看所有信息，但不能修改)
        result.add(
            PermissionGroupBuilder
                .nameOf("校级领导权限组")
                .description("可查看所有信息，但不能修改")
                .roleType(RoleType.PRINCIPAL)
                .readable(new Paths().all())
                .build()
        );

        // 超级管理员权限组(能查看并修改所有信息)
        result.add(
            PermissionGroupBuilder
                .nameOf("超级管理员权限组")
                .description("可查看并修改所有信息")
                .roleType(RoleType.SUPER_ADMIN)
                .readable(new Paths().all())
                .writable(new Paths().all())
                .arrayFullControl(RELATIVES_ARRAY)
                .arrayFullControl(HEALTH_COND_ARRAYS)
                .build()
        );

        return result;
    }

    public static final class PermissionGroupBuilder {

        private final String name;
        private final Set<RoleType> roleTypes = new HashSet<>();
        private final Set<String> allowedReadableJsonPaths = new HashSet<>();
        private final Set<String> allowedWritableJsonPaths = new HashSet<>();
        private final Set<String> allowedAddArrayJsonPaths = new HashSet<>();
        private final Set<String> allowedEditArrayJsonPaths = new HashSet<>();
        private final Set<String> allowedDeleteArrayJsonPaths = new HashSet<>();
        private final Set<ValidationExpr> validations = new HashSet<>();
        private String description;
        private Boolean enabled = true;

        public PermissionGroupBuilder(String name) {
            this.name = name;
        }

        public static PermissionGroupBuilder nameOf(String name) {
            return new PermissionGroupBuilder(name);
        }

        public PermissionGroupBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PermissionGroupBuilder disabled() {
            this.enabled = false;
            return this;
        }

        public PermissionGroupBuilder roleType(RoleType roleType) {
            this.roleTypes.add(roleType);
            return this;
        }

        public PermissionGroupBuilder readable(String path) {
            this.allowedReadableJsonPaths.add(path);
            return this;
        }

        public PermissionGroupBuilder readable(Paths paths) {
            this.allowedReadableJsonPaths.addAll(paths.get());
            return this;
        }

        public PermissionGroupBuilder writable(String path) {
            this.allowedWritableJsonPaths.add(path);
            return this;
        }

        public PermissionGroupBuilder writable(Paths paths) {
            this.allowedWritableJsonPaths.addAll(paths.get());
            return this;
        }

        public PermissionGroupBuilder canArrayAdd(Set<String> arrayPaths) {
            this.allowedAddArrayJsonPaths.addAll(arrayPaths);
            return this;
        }

        public PermissionGroupBuilder arrayFullControl(String arrayPath) {
            this.allowedAddArrayJsonPaths.add(arrayPath);
            this.allowedEditArrayJsonPaths.add(arrayPath);
            this.allowedDeleteArrayJsonPaths.add(arrayPath);
            return this;
        }

        public PermissionGroupBuilder arrayFullControl(Set<String> arrayPaths) {
            this.allowedAddArrayJsonPaths.addAll(arrayPaths);
            this.allowedEditArrayJsonPaths.addAll(arrayPaths);
            this.allowedDeleteArrayJsonPaths.addAll(arrayPaths);
            return this;
        }

        public PermissionGroupBuilder validation(ValidationExpr validation) {
            this.validations.add(validation);
            return this;
        }

        public PermissionGroupBuilder validations(Set<ValidationExpr> validations) {
            this.validations.addAll(validations);
            return this;
        }

        public PermissionGroup build() {
            PermissionGroup permissionGroup = new PermissionGroup();
            permissionGroup.setName(this.name);
            permissionGroup.setDescription(this.description);
            permissionGroup.setEnabled(this.enabled);
            permissionGroup.setRoleTypes(this.roleTypes);
            permissionGroup.setAllowedReadableJsonPaths(this.allowedReadableJsonPaths);
            permissionGroup.setAllowedWritableJsonPaths(this.allowedWritableJsonPaths);
            permissionGroup.setAllowedAddArrayJsonPaths(this.allowedAddArrayJsonPaths);
            permissionGroup.setAllowedEditArrayJsonPaths(this.allowedEditArrayJsonPaths);
            permissionGroup.setAllowedDeleteArrayJsonPaths(this.allowedDeleteArrayJsonPaths);
            permissionGroup.setValidations(this.validations);
            return permissionGroup;
        }

    }

    public final class Paths {

        private final HashSet<String> paths = new HashSet<>();

        public Paths beginsWith(String prefix) {
            this.paths.clear();
            this.paths.addAll(fieldMetadata.keySet().stream()
                             .filter(path -> path.startsWith(prefix))
                             .collect(Collectors.toSet())
            );
            return this;
        }

        public Paths all() {
            this.paths.clear();
            this.paths.addAll(fieldMetadata.keySet());
            return this;
        }

        public Paths exceptBeginWith(String prefix) {
            Set<String> toRemove = fieldMetadata
                .keySet().stream()
                .filter(path -> path.startsWith(prefix))
                .collect(Collectors.toSet());
            this.paths.removeAll(toRemove);
            return this;
        }

        public Paths except(String path) {
            this.paths.remove(path);
            return this;
        }

        public Set<String> get() {
            return this.paths;
        }

    }

}
