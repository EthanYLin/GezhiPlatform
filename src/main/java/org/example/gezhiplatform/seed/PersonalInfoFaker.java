package org.example.gezhiplatform.seed;


import io.github.yindz.random.RandomSource;
import io.github.yindz.random.constant.Province;
import io.github.yindz.random.source.PersonInfoSource;
import lombok.Builder;
import org.example.gezhiplatform.entity.archive.address_part.Address;
import org.example.gezhiplatform.entity.enums.Gender;
import org.example.gezhiplatform.entity.enums.Nation;
import org.example.gezhiplatform.entity.enums.PoliticalStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;


public class PersonalInfoFaker {

    private final PersonInfoSource personInfoSource = RandomSource.personInfoSource();

    @NotNull
    private final Gender gender;

    @NotNull
    private final Province province;

    @NotNull
    private final Integer age;

    @Builder
    public PersonalInfoFaker(@Nullable Gender gender, @Nullable Province province, Integer age) {
        this.gender = Objects.requireNonNullElseGet(
            gender,
            () -> new Random().nextBoolean() ? Gender.MALE : Gender.FEMALE
        );
        this.province = Objects.requireNonNullElse(province, Province.SH);
        this.age = Objects.requireNonNullElse(age, 16);
    }

    public String name() {
        return switch (gender) {
            case MALE -> personInfoSource.randomMaleChineseName();
            case FEMALE -> personInfoSource.randomFemaleChineseName();
            default -> personInfoSource.randomChineseName();
        };
    }

    public String rin() {
        if (gender == Gender.FEMALE) {
            return personInfoSource.randomFemaleIdCard(province, age);
        } else {
            return personInfoSource.randomMaleIdCard(province, age);
        }
    }

    public String mobile() {
        return personInfoSource.randomChineseMobile();
    }

    public Nation nation() {
        if (RandomUtils.roll(80)) {
            return Nation.HAN; // 汉族
        } else {
            return RandomUtils.pickOneFrom(Nation.values());
        }
    }

    public PoliticalStatus politicalStatus() {
        if (RandomUtils.roll(40)) {
            return PoliticalStatus.TUANYUAN;
        } else {
            return PoliticalStatus.QUNZHONG;
        }
    }

    public static Address shanghaiAddress() {
        String fullAddress = RandomSource.areaSource().randomAddress(Province.SH);
        fullAddress = fullAddress.replaceFirst("上海市", "");
        String district = fullAddress.split("区")[0] + "区";
        String detail = fullAddress.replaceFirst(district, "").trim();
        String street = detail.split("路")[0] + "路街道";
        String committee = RandomSource.languageSource().randomChinese(2) + "居委会";
        return new Address(
            null,
            "上海市",
            "上海市",
            district,
            detail,
            street,
            committee
        );
    }

}
