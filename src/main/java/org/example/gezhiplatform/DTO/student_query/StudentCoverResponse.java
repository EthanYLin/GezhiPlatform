package org.example.gezhiplatform.DTO.student_query;

import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.Campus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 学生基本信息响应类
 * 用于：UserService和UserController
 * 用于：ArchiveQueryService和ArchiveQueryController
 * @param stuNo 学号
 * @param stuName 姓名
 * @param campus 校区
 * @param gradeClassName 年级班级(形如: 高一(01)班)
 */
public record StudentCoverResponse(
    @NotNull String stuNo,
    @NotNull String stuName,
    @Nullable String campus,
    @Nullable String gradeClassName
){
    public StudentCoverResponse(Student s) {
        this(
            s.getStuNo(),
            s.getStuName(),
            s.getCampus().map(Campus::getName).orElse(null),
            s.getGradeClass().map(GradeClass::toRelativeExpr).orElse(null)
        );
    }
}
