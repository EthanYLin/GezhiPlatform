package org.example.gezhiplatform.DTO.archive;

import com.jayway.jsonpath.DocumentContext;
import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.archive.address_part.AddressPart;
import org.example.gezhiplatform.entity.archive.address_part.CurrentAddress;
import org.example.gezhiplatform.entity.archive.address_part.DomicileAddress;
import org.example.gezhiplatform.entity.archive.admission_part.AdmissionPart;
import org.example.gezhiplatform.entity.archive.family_part.FamilyPart;
import org.example.gezhiplatform.entity.archive.family_part.Parent;
import org.example.gezhiplatform.entity.archive.family_part.Relative;
import org.example.gezhiplatform.entity.archive.health_part.HealthCondition;
import org.example.gezhiplatform.entity.archive.health_part.HealthPart;
import org.example.gezhiplatform.entity.archive.personal_part.PersonalPart;
import org.example.gezhiplatform.entity.enums.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 将档案数据导出为Excel文件时，会先将 {@link Archive} 或 Jayway JSON 格式的 Archive 转换为该类。
 * 再用该类的数据填充到Excel文件。
 * <b>注意：本类中的字段名必须与Excel导出模板中的一致</b>
 */
public record ArchiveExportResponse(
    @NotNull String exporter, // 导出人
    @Nullable String exportTime, // 导出时间
    @Nullable String stuNo, // 学号
    @Nullable String stuName, // 姓名
    @Nullable String stuClass, // 班级
    @Nullable String campus, // 校区
    @Nullable String gender, // 性别
    @Nullable String birthDate, // 出生日期
    @Nullable String nation, // 民族
    @Nullable String politicalStatus, // 政治面貌
    @Nullable String mobile, // 手机号码
    @Nullable String rin, // 身份证号
    @Nullable String juniorHighSchoolDistrict, // 生源地
    @Nullable String admissionPath, // 录取方式
    @Nullable String juniorHighSchoolName, // 初中学校
    @Nullable String currentAddress, // 居住地址
    @Nullable String street, // 街道
    @Nullable String committee, // 居委会
    @Nullable String domicileAddress, // 户籍地址
    @Nullable String fatherName, // 父亲姓名
    @Nullable String motherName, // 母亲姓名
    @Nullable String fatherMobile, // 父亲电话
    @Nullable String motherMobile, // 母亲电话
    @Nullable String fatherWorkUnit, // 父亲工作单位
    @Nullable String motherWorkUnit, // 母亲工作单位
    @Nullable String rel1Name, // 其他直系亲属（一）姓名
    @Nullable Integer rel1Age, // 其他直系亲属（一）年龄
    @Nullable String rel2Name, // 其他直系亲属（二）姓名
    @Nullable Integer rel2Age, // 其他直系亲属（二）年龄
    @Nullable String rel1Info, // 其他直系亲属（一）工作/就学信息
    @Nullable String rel2Info, // 其他直系亲属（二）工作/就学信息
    @Nullable String remainRelativeTips, // 其他直系亲属提示
    @Nullable String physicalHealthStatus, // 身体健康状况
    @Nullable String mentalHealthStatus, // 心理健康状况
    @Nullable String physicalHealthIssue, // 身体关注问题
    @Nullable String mentalHealthIssue, // 心理关注问题
    @Nullable String physicalMedicationUse, // 身体服药状况
    @Nullable String mentalMedicationUse, // 心理服药状况
    @Nullable String physicalTreatment, // 身体治疗情况
    @Nullable String mentalTreatment // 心理治疗情况
) {

    public static ArchiveExportResponse of(DocumentContext document, Student student, String exporter) {
        
        // 地址信息部分
        String currentAddressProvince = document.read("$.addressPart.currentAddress.province", String.class);
        String currentAddressCity = document.read("$.addressPart.currentAddress.city", String.class);
        String currentAddressDistrict = document.read("$.addressPart.currentAddress.district", String.class);
        String currentAddressDetail = document.read("$.addressPart.currentAddress.detail", String.class);
        String currentAddressStr = buildAddressString(currentAddressProvince, currentAddressCity, currentAddressDistrict, currentAddressDetail);
        
        String domicileAddressProvince = document.read("$.addressPart.domicileAddress.province", String.class);
        String domicileAddressCity = document.read("$.addressPart.domicileAddress.city", String.class);
        String domicileAddressDistrict = document.read("$.addressPart.domicileAddress.district", String.class);
        String domicileAddressDetail = document.read("$.addressPart.domicileAddress.detail", String.class);
        String domicileAddressStr = buildAddressString(domicileAddressProvince, domicileAddressCity, domicileAddressDistrict, domicileAddressDetail);
        
        // 其他直系亲属信息（数组）
        @Nullable List<?> otherRelatives = document.read("$.familyPart.otherRelatives", List.class);
        String remainRelativeTips =
            otherRelatives != null && otherRelatives.size() > 2
                ? "剩余 " + (otherRelatives.size() - 2) + " 位直系亲属未显示，请登录系统查看。"
                : null;
        
        return new ArchiveExportResponse(
            exporter,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            student.getStuNo(),
            student.getStuName(),
            student.getGradeClass().map(GradeClass::toRelativeExpr).orElse(null),
            student.getCampus().map(Campus::getName).orElse(null),
            document.read("$.personalPart.gender", String.class),
            document.read("$.personalPart.birthDate", String.class),
            document.read("$.personalPart.nation", String.class),
            document.read("$.personalPart.politicalStatus", String.class),
            document.read("$.personalPart.mobile", String.class),
            document.read("$.personalPart.rin", String.class),
            document.read("$.admissionPart.juniorHighSchoolDistrict", String.class),
            document.read("$.admissionPart.admissionPath", String.class),
            document.read("$.admissionPart.juniorHighSchoolName", String.class),
            currentAddressStr,
            document.read("$.addressPart.currentAddress.street", String.class),
            document.read("$.addressPart.currentAddress.committee", String.class),
            domicileAddressStr,
            document.read("$.familyPart.father.name", String.class),
            document.read("$.familyPart.mother.name", String.class),
            document.read("$.familyPart.father.mobile", String.class),
            document.read("$.familyPart.mother.mobile", String.class),
            document.read("$.familyPart.father.workUnit", String.class),
            document.read("$.familyPart.mother.workUnit", String.class),
            document.read("$.familyPart.otherRelatives[0].name", String.class),
            document.read("$.familyPart.otherRelatives[0].age", Integer.class),
            document.read("$.familyPart.otherRelatives[1].name", String.class),
            document.read("$.familyPart.otherRelatives[1].age", Integer.class),
            document.read("$.familyPart.otherRelatives[0].info", String.class),
            document.read("$.familyPart.otherRelatives[1].info", String.class),
            remainRelativeTips,
            document.read("$.healthPart.physicalCondition.healthStatus", String.class),
            document.read("$.healthPart.mentalCondition.healthStatus", String.class),
            document.read("$.healthPart.physicalCondition.healthIssue", String.class),
            document.read("$.healthPart.mentalCondition.healthIssue", String.class),
            document.read("$.healthPart.physicalCondition.medicationUse", String.class),
            document.read("$.healthPart.mentalCondition.medicationUse", String.class),
            document.read("$.healthPart.physicalCondition.ongoingTreatment", String.class),
            document.read("$.healthPart.mentalCondition.ongoingTreatment", String.class)
        );
    }


    public static ArchiveExportResponse of(Archive archive, Student student, String exporter) {

        PersonalPart personalPart = archive.getPersonalPart();
        AdmissionPart admissionPart = archive.getAdmissionPart();
        AddressPart addressPart = archive.getAddressPart();
        FamilyPart familyPart = archive.getFamilyPart();
        HealthPart healthPart = archive.getHealthPart();
        
        // 获取地址信息
        CurrentAddress currentAddress = Optional.ofNullable(addressPart).map(AddressPart::getCurrentAddress).orElse(null);
        DomicileAddress domicileAddress = Optional.ofNullable(addressPart).map(AddressPart::getDomicileAddress).orElse(null);
        String currentAddressStr = currentAddress == null ? null : buildAddressString(currentAddress.getProvince(), currentAddress.getCity(), currentAddress.getDistrict(), currentAddress.getDetail());
        String domicileAddressStr = domicileAddress == null ? null : buildAddressString(domicileAddress.getProvince(), domicileAddress.getCity(), domicileAddress.getDistrict(), domicileAddress.getDetail());
        
        // 获取家庭成员信息
        Parent father = Optional.ofNullable(familyPart).map(FamilyPart::getFather).orElse(null);
        Parent mother = Optional.ofNullable(familyPart).map(FamilyPart::getMother).orElse(null);
        List<Relative> relatives = Optional.ofNullable(familyPart).map(FamilyPart::getOtherRelatives).orElse(List.of());
        
        // 获取健康信息
        HealthCondition physicalCondition = Optional.ofNullable(healthPart).map(HealthPart::getPhysicalCondition).orElse(null);
        HealthCondition mentalCondition = Optional.ofNullable(healthPart).map(HealthPart::getMentalCondition).orElse(null);

        // 获取其他直系亲属信息
        String remainRelativeTips = relatives.size() > 2
            ? "剩余 " + (relatives.size() - 2) + " 位直系亲属未显示，请登录系统查看。"
            : null;
        
        return new ArchiveExportResponse(
            exporter,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            student.getStuNo(),
            student.getStuName(),
            student.getGradeClass().map(GradeClass::toRelativeExpr).orElse(null),
            student.getCampus().map(Campus::getName).orElse(null),
            Optional.ofNullable(personalPart).map(PersonalPart::getGender).map(Gender::getName).orElse(null),
            Optional.ofNullable(personalPart).map(PersonalPart::getBirthDateStr).orElse(null),
            Optional.ofNullable(personalPart).map(PersonalPart::getNation).map(Nation::getDesc).orElse(null),
            Optional.ofNullable(personalPart).map(PersonalPart::getPoliticalStatus).map(PoliticalStatus::getName).orElse(null),
            Optional.ofNullable(personalPart).map(PersonalPart::getMobile).orElse(null),
            Optional.ofNullable(personalPart).map(PersonalPart::getRin).orElse(null),
            Optional.ofNullable(admissionPart).map(AdmissionPart::getJuniorHighSchoolDistrict).map(District::getName).orElse(null),
            Optional.ofNullable(admissionPart).map(AdmissionPart::getAdmissionPath).map(AdmissionPath::getName).orElse(null),
            Optional.ofNullable(admissionPart).map(AdmissionPart::getJuniorHighSchoolName).orElse(null),
            currentAddressStr,
            Optional.ofNullable(currentAddress).map(CurrentAddress::getStreet).orElse(null),
            Optional.ofNullable(currentAddress).map(CurrentAddress::getCommittee).orElse(null),
            domicileAddressStr,
            Optional.ofNullable(father).map(Parent::getName).orElse(null),
            Optional.ofNullable(mother).map(Parent::getName).orElse(null),
            Optional.ofNullable(father).map(Parent::getMobile).orElse(null),
            Optional.ofNullable(mother).map(Parent::getMobile).orElse(null),
            Optional.ofNullable(father).map(Parent::getWorkUnit).orElse(null),
            Optional.ofNullable(mother).map(Parent::getWorkUnit).orElse(null),
            !relatives.isEmpty() ? relatives.get(0).getName() : null,
            !relatives.isEmpty() ? relatives.get(0).calcAge() : null,
            relatives.size() > 1 ? relatives.get(1).getName() : null,
            relatives.size() > 1 ? relatives.get(1).calcAge() : null,
            !relatives.isEmpty() ? relatives.get(0).getInfo() : null,
            relatives.size() > 1 ? relatives.get(1).getInfo() : null,
            remainRelativeTips,
            Optional.ofNullable(physicalCondition).map(HealthCondition::getHealthStatus).map(HealthStatus::getName).orElse(null),
            Optional.ofNullable(mentalCondition).map(HealthCondition::getHealthStatus).map(HealthStatus::getName).orElse(null),
            Optional.ofNullable(physicalCondition).map(HealthCondition::getHealthIssue).orElse(null),
            Optional.ofNullable(mentalCondition).map(HealthCondition::getHealthIssue).orElse(null),
            Optional.ofNullable(physicalCondition).map(HealthCondition::getMedicationUse).orElse(null),
            Optional.ofNullable(mentalCondition).map(HealthCondition::getMedicationUse).orElse(null),
            Optional.ofNullable(physicalCondition).map(HealthCondition::getOngoingTreatment).orElse(null),
            Optional.ofNullable(mentalCondition).map(HealthCondition::getOngoingTreatment).orElse(null)
        );
    }
    
    /**
     * 拼接地址字符串
     */
    private static String buildAddressString(
        @Nullable String province, @Nullable String city, @Nullable String district, @Nullable String detail
    ) {
        StringBuilder sb = new StringBuilder();
        if (province != null && !province.isEmpty()) sb.append(province);
        if (city != null && !city.isEmpty() && !Objects.equals(province, city)) sb.append(city);
        if (district != null && !district.isEmpty()) sb.append(district);
        if (detail != null && !detail.isEmpty()) sb.append(detail);
        return !sb.isEmpty() ? sb.toString() : null;
    }
}
