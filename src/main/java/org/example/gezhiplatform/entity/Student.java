package org.example.gezhiplatform.entity;

import jakarta.persistence.*;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.enums.Campus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * 学生实体类
 */
@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 学生ID(数据库内自增)

    @NonNull
    @Column(unique = true, nullable = false)
    private String stuNo; // 学号

    @NonNull
    @Column(nullable = false)
    private String stuName; // 姓名

    @Nullable
    private Campus campus; // 校区

    @Embedded
    @Nullable
    private GradeClass gradeClass; // 年级-班级

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private Archive archive; // 具体档案信息

    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public @NonNull String getStuNo() {
        return stuNo;
    }

    public void setStuNo(@NonNull String stuNo) {
        this.stuNo = stuNo;
    }

    public @NonNull String getStuName() {
        return stuName;
    }

    public void setStuName(@NonNull String stuName) {
        this.stuName = stuName;
    }

    public Optional<Campus> getCampus() {
        return Optional.ofNullable(campus);
    }

    public void setCampus(@Nullable Campus campus) {
        this.campus = campus;
    }

    public Optional<GradeClass> getGradeClass() {
        return Optional.ofNullable(gradeClass);
    }

    public void setGradeClass(@Nullable GradeClass gradeClass) {
        this.gradeClass = gradeClass;
    }

    public Optional<Archive> getArchiveDetail() {
        return Optional.ofNullable(archive);
    }

    public void setArchiveDetail(@Nullable Archive archive) {
        this.archive = archive;
    }

    public Student() {
    }

    public Student(@NonNull String stuNo, @NonNull String stuName) {
        this.stuNo = stuNo;
        this.stuName = stuName;
    }

    public Student(
        @NonNull String stuNo,
        @NonNull String stuName,
        @Nullable Integer gradeNo,
        @Nullable Integer classNo,
        @Nullable Campus campus
   ) {
        this.stuNo = stuNo;
        this.stuName = stuName;
        this.gradeClass = new GradeClass(gradeNo, classNo);
        this.campus = campus;
    }

    @Override
    public String toString() {
        return "Student{" +
               "id=" + id +
               ", stuNo='" + stuNo + '\'' +
               ", stuName='" + stuName + '\'' +
               ", gradeClass=" + gradeClass +
               '}';
    }
}
