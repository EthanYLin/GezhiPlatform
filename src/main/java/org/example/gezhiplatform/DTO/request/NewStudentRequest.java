package org.example.gezhiplatform.DTO.request;

import jakarta.validation.constraints.*;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.Campus;
import org.springframework.lang.Nullable;

/**
 * 新增学生或更新学生的请求
 * 用于UserController及UserService
 * 【仅面向管理员】
 * @param stuNo 学号
 * @param stuName 姓名
 * @param campus 校区
 * @param gradeNo 年级
 * @param classNo 班级
 */
public record NewStudentRequest(

    @NotNull(message = "学号不能为空")
    @Size(max = 50, message = "学号长度不能超过 50 个字符")
    @Pattern(regexp = "^[0-9]+$", message = "学号只能由数字组成")
    String stuNo, // 学号

    @NotNull(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50 个字符")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "姓名只能包含中文、英文和空格")
    String stuName, // 姓名

    @Nullable
    Campus campus, // 校区

    @Nullable
    @Min(value = 1900, message = "年级不能小于 1900")
    @Max(value = 2100, message = "年级不能大于 2100")
    Integer gradeNo, // 年级（可为空）

    @Nullable
    @Min(value = 1, message = "班级编号不能小于 1")
    @Max(value = 100, message = "班级编号不能大于 100")
    Integer classNo // 班级（可为空）

) {
    public Student toStudent() {
        return new Student(
            stuNo, stuName, gradeNo, classNo, campus
        );
    }
}
