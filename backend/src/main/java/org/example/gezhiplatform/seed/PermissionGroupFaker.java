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
    public static final String CURRENT_ADDRESS = "$.addressPart.currentAddress";

    public static final String RELATIVES_ARRAY = "$.familyPart.otherRelatives";
    public static final String MENTAL_COND_ARRAY = "$.healthPart.mentalCondition";
    public static final String PHY_COND_ARRAY = "$.healthPart.physicalCondition";
    public static final Set<String> HEALTH_COND_ARRAYS = Set.of(MENTAL_COND_ARRAY, PHY_COND_ARRAY);

    public static final Set<String> AUDIT_FIELDS = Set.of(
      "$.healthPart.physicalCondition[*].createdAt",
      "$.healthPart.physicalCondition[*].updatedAt",
      "$.healthPart.mentalCondition[*].createdAt",
      "$.healthPart.mentalCondition[*].updatedAt"
    );

    public static final List<ValidationExpr> PERSONAL_PART_VALIDATIONS = List.of(
        new ValidationExpr(
            "#cur.personalPart?.mobile != null && !#cur.personalPart.mobile.trim().isEmpty()",
            "!!data.personalPart?.mobile && data.personalPart.mobile.trim().length > 0",
            "学生手机号为必填项"
        ),
        new ValidationExpr(
            "#cur.personalPart?.nation != null",
            "!!data.personalPart?.nation",
            "民族为必填项"
        )
    );

    public static final List<ValidationExpr> ADMISSION_PART_VALIDATIONS = List.of(
        new ValidationExpr(
            "#cur.admissionPart?.juniorHighSchoolDistrict != null",
            "!!data.admissionPart?.juniorHighSchoolDistrict",
            "初中所在区为必填项"
        ),
        new ValidationExpr(
            "#cur.admissionPart?.juniorHighSchoolName != null && !#cur.admissionPart.juniorHighSchoolName.trim().isEmpty()",
            "!!data.admissionPart?.juniorHighSchoolName && data.admissionPart.juniorHighSchoolName.trim().length > 0",
            "初中学校为必填项"
        )
    );

    public static final List<ValidationExpr> ADDRESS_PART_VALIDATIONS = List.of(
        new ValidationExpr(
            "#cur.addressPart?.domicileAddress != null",
            "!!data.addressPart?.domicileAddress",
            "户籍地址为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.currentAddress != null",
            "!!data.addressPart?.currentAddress",
            "现居地址为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.domicileAddress?.province != null && !#cur.addressPart.domicileAddress.province.trim().isEmpty()",
            "!!data.addressPart?.domicileAddress?.province && data.addressPart.domicileAddress.province.trim().length > 0",
            "户籍地址省份为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.domicileAddress?.city != null && !#cur.addressPart.domicileAddress.city.trim().isEmpty()",
            "!!data.addressPart?.domicileAddress?.city && data.addressPart.domicileAddress.city.trim().length > 0",
            "户籍地址城市为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.domicileAddress?.district != null && !#cur.addressPart.domicileAddress.district.trim().isEmpty()",
            "!!data.addressPart?.domicileAddress?.district && data.addressPart.domicileAddress.district.trim().length > 0",
            "户籍地址区县为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.domicileAddress?.detail != null && !#cur.addressPart.domicileAddress.detail.trim().isEmpty()",
            "!!data.addressPart?.domicileAddress?.detail && data.addressPart.domicileAddress.detail.trim().length > 0",
            "户籍地址详细地址为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.currentAddress?.province != null && !#cur.addressPart.currentAddress.province.trim().isEmpty()",
            "!!data.addressPart?.currentAddress?.province && data.addressPart.currentAddress.province.trim().length > 0",
            "现居地址省份为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.currentAddress?.city != null && !#cur.addressPart.currentAddress.city.trim().isEmpty()",
            "!!data.addressPart?.currentAddress?.city && data.addressPart.currentAddress.city.trim().length > 0",
            "现居地址城市为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.currentAddress?.district != null && !#cur.addressPart.currentAddress.district.trim().isEmpty()",
            "!!data.addressPart?.currentAddress?.district && data.addressPart.currentAddress.district.trim().length > 0",
            "现居地址区县为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.currentAddress?.detail != null && !#cur.addressPart.currentAddress.detail.trim().isEmpty()",
            "!!data.addressPart?.currentAddress?.detail && data.addressPart.currentAddress.detail.trim().length > 0",
            "现居地址详细地址为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.currentAddress?.street != null && !#cur.addressPart.currentAddress.street.trim().isEmpty()",
            "!!data.addressPart?.currentAddress?.street && data.addressPart.currentAddress.street.trim().length > 0",
            "现居地址街道为必填项"
        ),
        new ValidationExpr(
            "#cur.addressPart?.currentAddress?.committee != null && !#cur.addressPart.currentAddress.committee.trim().isEmpty()",
            "!!data.addressPart?.currentAddress?.committee && data.addressPart.currentAddress.committee.trim().length > 0",
            "现居地址居委为必填项"
        )
    );

    public static final List<ValidationExpr> FAMILY_PART_VALIDATIONS = List.of(
        new ValidationExpr(
            "#cur.familyPart?.father != null || #cur.familyPart?.mother != null || (#cur.familyPart?.otherRelatives != null && #cur.familyPart.otherRelatives.size() >= 1)",
            "!!data.familyPart?.father || !!data.familyPart?.mother || (data.familyPart?.otherRelatives && data.familyPart.otherRelatives.length > 0)",
            "请至少填写父亲、母亲或一位直系亲属信息"
        ),
        new ValidationExpr(
            "#cur.familyPart?.father == null || (#cur.familyPart.father.name != null && !#cur.familyPart.father.name.trim().isEmpty() && #cur.familyPart.father.mobile != null && !#cur.familyPart.father.mobile.trim().isEmpty() && #cur.familyPart.father.workUnit != null && !#cur.familyPart.father.workUnit.trim().isEmpty())",
            "!(data.familyPart?.father) || (data.familyPart.father.name && data.familyPart.father.name.trim().length > 0 && data.familyPart.father.mobile && data.familyPart.father.mobile.trim().length > 0 && data.familyPart.father.workUnit && data.familyPart.father.workUnit.trim().length > 0)",
            "父亲信息需填写完整"
        ),
        new ValidationExpr(
            "#cur.familyPart?.mother == null || (#cur.familyPart.mother.name != null && !#cur.familyPart.mother.name.trim().isEmpty() && #cur.familyPart.mother.mobile != null && !#cur.familyPart.mother.mobile.trim().isEmpty() && #cur.familyPart.mother.workUnit != null && !#cur.familyPart.mother.workUnit.trim().isEmpty())",
            "!(data.familyPart?.mother) || (data.familyPart.mother.name && data.familyPart.mother.name.trim().length > 0 && data.familyPart.mother.mobile && data.familyPart.mother.mobile.trim().length > 0 && data.familyPart.mother.workUnit && data.familyPart.mother.workUnit.trim().length > 0)",
            "母亲信息需填写完整"
        ),
        new ValidationExpr(
            "#cur.familyPart?.otherRelatives == null || #cur.familyPart.otherRelatives.isEmpty() || #cur.familyPart.otherRelatives.?[name == null || name.trim().isEmpty() || birthYear == null || info == null || info.trim().isEmpty()].isEmpty()",
            "!(data.familyPart?.otherRelatives && data.familyPart.otherRelatives.length > 0) || data.familyPart.otherRelatives.every(rel => rel && rel.name && rel.name.trim().length > 0 && rel.birthYear != null && rel.info && rel.info.trim().length > 0)",
            "直系亲属信息需填写完整"
        )
    );

    public static final List<ValidationExpr> HEALTH_PART_VALIDATIONS = List.of(
        new ValidationExpr(
            "#cur.healthPart?.physicalCondition == null || #cur.healthPart.physicalCondition.isEmpty() || #cur.healthPart.physicalCondition.?[healthIssue == null || healthIssue.trim().isEmpty() || medicationUse == null || medicationUse.trim().isEmpty() || ongoingTreatment == null || ongoingTreatment.trim().isEmpty()].isEmpty()",
            "!(data.healthPart?.physicalCondition && data.healthPart.physicalCondition.length > 0) || data.healthPart.physicalCondition.every(cond => cond && cond.healthIssue && cond.healthIssue.trim().length > 0 && cond.medicationUse && cond.medicationUse.trim().length > 0 && cond.ongoingTreatment && cond.ongoingTreatment.trim().length > 0)",
            "每条身体状况记录需填写完整"
        ),
        new ValidationExpr(
            "#cur.healthPart?.mentalCondition == null || #cur.healthPart.mentalCondition.isEmpty() || #cur.healthPart.mentalCondition.?[healthIssue == null || healthIssue.trim().isEmpty() || medicationUse == null || medicationUse.trim().isEmpty() || ongoingTreatment == null || ongoingTreatment.trim().isEmpty()].isEmpty()",
            "!(data.healthPart?.mentalCondition && data.healthPart.mentalCondition.length > 0) || data.healthPart.mentalCondition.every(cond => cond && cond.healthIssue && cond.healthIssue.trim().length > 0 && cond.medicationUse && cond.medicationUse.trim().length > 0 && cond.ongoingTreatment && cond.ongoingTreatment.trim().length > 0)",
            "每条心理状况记录需填写完整"
        )
    );

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
                .readable(new Paths().beginsWith(CURRENT_ADDRESS))
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
                .readable(new Paths().all().except(RIN).exceptBeginWith(ADMISSION_PART).except(AUDIT_FIELDS))
                .writable(new Paths().beginsWith(HEALTH_PART))
                .canArrayAdd(HEALTH_COND_ARRAYS)
                .validations(HEALTH_PART_VALIDATIONS)
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
                .validations(PERSONAL_PART_VALIDATIONS)
                .validations(ADDRESS_PART_VALIDATIONS)
                .validations(FAMILY_PART_VALIDATIONS)
                .validations(HEALTH_PART_VALIDATIONS)
                .build()
        );

        // 新生家长权限组(能查看并修改除录取方式之外的所有信息)
        result.add(
            PermissionGroupBuilder
                .nameOf("新生家长权限组")
                .description("可查看并修改除录取方式之外的所有信息")
                .roleType(RoleType.FRESHMAN_PARENT)
                .readable(new Paths().all().except(ADMISSION_PATH).except(AUDIT_FIELDS))
                .writable(new Paths().all().except(ADMISSION_PATH))
                .arrayFullControl(RELATIVES_ARRAY)
                .arrayFullControl(HEALTH_COND_ARRAYS)
                .validations(PERSONAL_PART_VALIDATIONS)
                .validations(ADMISSION_PART_VALIDATIONS)
                .validations(ADDRESS_PART_VALIDATIONS)
                .validations(FAMILY_PART_VALIDATIONS)
                .validations(HEALTH_PART_VALIDATIONS)
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
                .validations(PERSONAL_PART_VALIDATIONS)
                .validations(ADMISSION_PART_VALIDATIONS)
                .validations(ADDRESS_PART_VALIDATIONS)
                .validations(FAMILY_PART_VALIDATIONS)
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
                .validations(PERSONAL_PART_VALIDATIONS)
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
                .readable(new Paths().beginsWith(ADDRESS_PART))
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

        public PermissionGroupBuilder validations(Collection<ValidationExpr> validations) {
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

        public Paths except(Collection<String> paths) {
            this.paths.removeAll(paths);
            return this;
        }

        public Set<String> get() {
            return this.paths;
        }

    }

}
