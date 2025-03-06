package org.example.gezhiplatform.entity;

import jakarta.persistence.Embeddable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * 年级-班级类
 * toAbsoluteExpr() 返回例如: 2024届1班
 * toRelativeExpr() 返回例如: 高一(01)班 (默认以当前时间所在的学年为准)
 * toRelativeExpr(int currentAcademicYear) 返回例如: 高一(01)班 (以指定的学年为准)
 */
@Embeddable
public class GradeClass {

    @Nullable
    private Integer gradeNo; // 届(Integer)

    @Nullable
    private Integer classNo; // 班级(Integer)

    public GradeClass() {
    }

    public GradeClass(@Nullable Integer gradeNo, @Nullable Integer classNo) {
        this.gradeNo = gradeNo;
        this.classNo = classNo;
    }

    public @Nullable Integer getGradeNo() {
        return gradeNo;
    }

    public void setGradeNo(@Nullable Integer gradeNo) {
        this.gradeNo = gradeNo;
    }

    public @Nullable Integer getClassNo() {
        return classNo;
    }

    public void setClassNo(@Nullable Integer classNo) {
        this.classNo = classNo;
    }

    public @NonNull String toAbsoluteExpr() {
        return (gradeNo != null ? gradeNo.toString() : "?") + "届" +
               (classNo != null ? classNo.toString() : "?") + "班";
    }

    public @NonNull String toRelativeExpr(int currentAcademicYear) {
        String gradeStr;
        String classStr;

        if (gradeNo == null) {
            gradeStr = "?届";
        } else {
            gradeStr = switch (gradeNo - currentAcademicYear) {
                case 3 -> "高一";
                case 2 -> "高二";
                case 1 -> "高三";
                default -> gradeNo + "届";
            };
        }

        if (classNo == null) {
            classStr = "(?)班";
        } else {
            classStr = "(" + String.format("%02d", classNo) + ")班";
        }

        return gradeStr + classStr;
    }

    public @NonNull String toRelativeExpr() {
        var currentAcademicYear = LocalDateTime.now().getMonthValue() >= 9 ?
            LocalDateTime.now().getYear() :
            LocalDateTime.now().getYear() - 1;
        return toRelativeExpr(currentAcademicYear);
    }

    @Override
    public String toString() {
        return this.toAbsoluteExpr();
    }
}

