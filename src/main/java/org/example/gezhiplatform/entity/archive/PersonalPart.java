package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.example.gezhiplatform.entity.enums.Gender;
import org.example.gezhiplatform.entity.enums.Nation;
import org.example.gezhiplatform.entity.enums.PoliticalStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.Optional;

@Entity
public class PersonalPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Nullable
    private String RIN; // 居民身份证号

    @Nullable
    private String mobile; // 手机号码

    @Nullable
    private Nation nation; // 民族

    @Nullable
    private PoliticalStatus politicalStatus; // 政治面貌

    public Optional<LocalDate> getBirthDate() {
        if (this.RIN == null || this.RIN.length() != 18) {
            return Optional.empty();
        }
        try {
            int year = Integer.parseInt(this.RIN.substring(6, 10));
            int month = Integer.parseInt(this.RIN.substring(10, 12));
            int day = Integer.parseInt(this.RIN.substring(12, 14));
            return Optional.of(LocalDate.of(year, month, day));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public @NonNull Gender getGender() {
        if (this.RIN == null || this.RIN.length() != 18) {
            return Gender.UNKNOWN;
        }
        try {
            int genderBit = Integer.parseInt(this.RIN.substring(16, 17));
            return genderBit % 2 == 0 ? Gender.FEMALE : Gender.MALE;
        } catch (NumberFormatException e) {
            return Gender.UNKNOWN;
        }
    }




}
