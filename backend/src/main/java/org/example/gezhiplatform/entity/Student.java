package org.example.gezhiplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.enums.Campus;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * 学生实体类
 */
@Entity
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 学生ID(数据库内自增)

    @NotNull
    @Column(unique = true, nullable = false)
    private String stuNo; // 学号

    @NotNull
    @Column(nullable = false)
    private String stuName; // 姓名

    @Nullable
    private Campus campus; // 校区

    @Embedded
    @Nullable
    private GradeClass gradeClass; // 年级-班级

    @Nullable
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private Archive archive; // 具体档案信息

    public Optional<Campus> getCampus() {
        return Optional.ofNullable(campus);
    }

    public Optional<GradeClass> getGradeClass() {
        return Optional.ofNullable(gradeClass);
    }

    public Optional<Archive> getArchiveDetail() {
        return Optional.ofNullable(archive);
    }

    public Student() {
    }

    public Student(@NotNull String stuNo, @NotNull String stuName) {
        this.stuNo = stuNo;
        this.stuName = stuName;
    }

    public Student(
        @NotNull String stuNo,
        @NotNull String stuName,
        @Nullable Integer gradeNo,
        @Nullable Integer classNo,
        @Nullable Campus campus
   ) {
        this.stuNo = stuNo;
        this.stuName = stuName;
        this.gradeClass = new GradeClass(gradeNo, classNo);
        this.campus = campus;
    }

}
