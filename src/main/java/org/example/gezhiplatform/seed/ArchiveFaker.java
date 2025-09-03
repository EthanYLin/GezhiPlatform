package org.example.gezhiplatform.seed;

import io.github.yindz.random.RandomSource;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.archive.address_part.AddressPart;
import org.example.gezhiplatform.entity.archive.admission_part.AdmissionPart;
import org.example.gezhiplatform.entity.archive.family_part.FamilyPart;
import org.example.gezhiplatform.entity.archive.family_part.Parent;
import org.example.gezhiplatform.entity.archive.family_part.Relative;
import org.example.gezhiplatform.entity.archive.health_part.HealthCondition;
import org.example.gezhiplatform.entity.archive.health_part.HealthPart;
import org.example.gezhiplatform.entity.archive.personal_part.PersonalPart;
import org.example.gezhiplatform.entity.enums.AdmissionPath;
import org.example.gezhiplatform.entity.enums.District;
import org.example.gezhiplatform.entity.enums.Gender;
import org.example.gezhiplatform.entity.enums.HealthStatus;
import org.example.gezhiplatform.utils.RandomUtils;

import java.util.List;

public class ArchiveFaker {

    public static PersonalPart personalPart(PersonalInfoFaker faker) {
        var personalPart = new PersonalPart();
        personalPart.setRIN(faker.rin());
        personalPart.setMobile(faker.mobile());
        personalPart.setNation(faker.nation());
        personalPart.setPoliticalStatus(faker.politicalStatus());
        return personalPart;
    }

    public static AdmissionPart admissionPart() {
        var admissionPart = new AdmissionPart();
        admissionPart.setJuniorHighSchoolDistrict(RandomUtils.pickOneFrom(District.values()));
        admissionPart.setJuniorHighSchoolName(
            RandomUtils.pickOneFrom(List.of("第一初级中学", "第二初级中学", "第三初级中学", "第四初级中学"))
        );
        admissionPart.setAdmissionPath(RandomUtils.pickOneFrom(AdmissionPath.values()));
        return admissionPart;
    }

    public static AddressPart addressPart() {
        var addressPart = new AddressPart();
        addressPart.setDomicileAddress(PersonalInfoFaker.shanghaiDomicileAddress());
        addressPart.setCurrentAddress(PersonalInfoFaker.shanghaiCurrentAddress());
        return addressPart;
    }

    public static Parent parent(Gender gender) {
        var parent = new Parent();
        parent.setName(switch (gender) {
            case MALE -> RandomSource.personInfoSource().randomMaleChineseName();
            case FEMALE -> RandomSource.personInfoSource().randomFemaleChineseName();
            case null, default -> RandomSource.personInfoSource().randomChineseName();
        });
        parent.setMobile(RandomSource.personInfoSource().randomChineseMobile());
        parent.setWorkUnit(RandomSource.otherSource().randomCompanyName(RandomSource.areaSource().randomProvince()));
        return parent;
    }

    public static Relative relative() {
        var relative = new Relative();
        relative.setName(RandomSource.personInfoSource().randomChineseName());
        relative.setBirthYear(RandomUtils.randInt(1970, 1999));
        return relative;
    }

    public static FamilyPart familyPart() {
        var familyPart = new FamilyPart();
        familyPart.setFather(parent(Gender.MALE));
        familyPart.setMother(parent(Gender.FEMALE));
        if (RandomUtils.roll(40)) {
            familyPart.setOtherRelatives(List.of());
        } else if (RandomUtils.roll(60)) {
            familyPart.setOtherRelatives(List.of(relative()));
        } else {
            familyPart.setOtherRelatives(List.of(relative(), relative()));
        }
        return familyPart;
    }

    private record HealthConditionSample(
        String healthIssue,
        String medicationUse,
        String ongoingTreatment
    ) {}

    private static final List<HealthConditionSample> mentalHealthConditionSamples = List.of(
        new HealthConditionSample("轻度焦虑", "偶尔服用镇静剂", "心理咨询"),
        new HealthConditionSample("中度抑郁", "规律服用抗抑郁药", "认知行为疗法"),
        new HealthConditionSample("失眠障碍", "偶尔服用助眠药物", "睡眠卫生指导"),
        new HealthConditionSample("社交焦虑", "无规律用药", "团体心理治疗"),
        new HealthConditionSample("双相情感障碍（轻度）", "情绪稳定剂（间断使用）", "定期精神科复诊"),
        new HealthConditionSample("强迫症状", "长期服用抗焦虑药", "暴露反应预防训练"),
        new HealthConditionSample("惊恐障碍", "按需服用抗焦虑药", "呼吸训练与放松训练"),
        new HealthConditionSample("创伤后应激反应", "偶尔使用安眠药", "暴露疗法与心理支持"),
        new HealthConditionSample("饮食障碍", "暂未用药", "营养指导与心理干预"),
        new HealthConditionSample("产后抑郁", "短期抗抑郁药物治疗", "家庭支持与心理疏导")
    );

    private static final List<HealthConditionSample> physicianHealthConditionSamples = List.of(
        new HealthConditionSample("高血压", "长期服用降压药", "定期体检与饮食控制"),
        new HealthConditionSample("糖尿病（Ⅱ型）", "规律注射胰岛素", "饮食管理与血糖监测"),
        new HealthConditionSample("高血脂", "按医嘱服用调脂药", "低脂饮食与规律运动"),
        new HealthConditionSample("哮喘", "按需吸入支气管扩张剂", "避免过敏原与肺功能监测"),
        new HealthConditionSample("慢性胃炎", "间断服用抑酸药", "饮食规律与避免刺激性食物"),
        new HealthConditionSample("腰椎间盘突出", "止痛药（按需）", "物理治疗与康复训练"),
        new HealthConditionSample("慢性支气管炎", "支气管扩张剂和止咳药", "戒烟与呼吸康复锻炼"),
        new HealthConditionSample("甲状腺功能减退", "规律服用甲状腺素片", "定期监测甲状腺激素水平"),
        new HealthConditionSample("贫血", "口服铁剂", "营养补充与定期复查血常规")
    );

    public static HealthPart healthPart() {
        var healthPart = new HealthPart();
        var mentalCondition = RandomUtils.pickOneFrom(mentalHealthConditionSamples);
        var physicianCondition = RandomUtils.pickOneFrom(physicianHealthConditionSamples);
        if (RandomUtils.roll(15)) {
            healthPart.setMentalCondition(
                new HealthCondition(
                    null,
                    HealthStatus.ATTENTION,
                    mentalCondition.healthIssue(),
                    mentalCondition.medicationUse(),
                    mentalCondition.ongoingTreatment()
                )
            );
        } else {
            healthPart.setMentalCondition(
                new HealthCondition(null, HealthStatus.HEALTHY, null, null, null)
            );
        }
        if (RandomUtils.roll(15)) {
            healthPart.setPhysicalCondition(
                new HealthCondition(
                    null,
                    HealthStatus.ATTENTION,
                    physicianCondition.healthIssue(),
                    physicianCondition.medicationUse(),
                    physicianCondition.ongoingTreatment()
                )
            );
        } else {
            healthPart.setPhysicalCondition(
                new HealthCondition(null, HealthStatus.HEALTHY, null, null, null)
            );
        }
        return healthPart;
    }

    public static Archive of(PersonalInfoFaker faker) {
        var archive = new Archive();
        archive.setPersonalPart(personalPart(faker));
        archive.setAdmissionPart(admissionPart());
        archive.setAddressPart(addressPart());
        archive.setFamilyPart(familyPart());
        archive.setHealthPart(healthPart());
        return archive;
    }

}
