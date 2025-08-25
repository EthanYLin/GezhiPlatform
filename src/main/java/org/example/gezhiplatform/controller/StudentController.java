package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.student_query.NewStudentRequest;
import org.example.gezhiplatform.DTO.student_query.StudentCoverResponse;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.StudentService;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生管理控制器
 * <p>
 * 提供学生基本信息的增删改查功能，包括学号、姓名、校区、年级、班级等信息。
 * 该接口仅面向【系统管理员】权限。
 * </p>
 */
@SaCheckRole("SUPER_ADMIN") // 仅超级管理员可访问
@RestController
@RequestMapping("/admin")
@Tag(name = "学生维护(面向管理员)", description = "学生基本信息的增删改查接口")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // =========================== 年级与班级查询 ===========================

    /**
     * 获取所有年级列表
     *
     * @return 年级列表
     * @apiNote GET /admin/grades
     */
    @Operation(summary = "获取所有年级")
    @GetMapping("/grades")
    public List<Integer> getAllGrades() {
        return studentService.getAllGrades();
    }

    /**
     * 获取指定年级的所有班级列表
     * <p>
     * 根据提供的年级(如2023)，返回该年级下所有班级列表(如1,2,3)。
     * </p>
     *
     * @param gradeNo 年级
     * @return 班级列表
     * @apiNote GET /admin/grades/{gradeNo}/classes
     */
    @Operation(summary = "获取指定年级的所有班级")
    @GetMapping("/grades/{gradeNo}/classes")
    public List<Integer> getClassesByGrade(
        @PathVariable @NotNull Integer gradeNo
    ) {
        return studentService.getClassesByGrade(gradeNo);
    }

    // =========================== 学生查询 ===========================

    /**
     * 获取学生列表
     * <p>
     * 支持三种查询模式：
     * <ul>
     *   <li>全量查询：不提供任何参数时返回所有学生</li>
     *   <li>按年级查询：仅提供gradeNo参数时返回指定年级的所有学生</li>
     *   <li>按班级查询：同时提供gradeNo和classNo参数时返回指定班级的学生</li>
     * </ul>
     * 同时，需要传入分页参数，默认每页20条记录。
     * size参数可调整每页记录数，page参数可调整页码（从0开始）。
     * </p>
     *
     * @param gradeNo  年级号（可选），示例：2022
     * @param classNo  班级号（可选），示例：1
     * @param pageable 分页参数，默认每页20条记录
     * @return 分页的学生基本信息列表
     * @throws CustomInvalidArgException 当班级号不为空但年级号为空时抛出
     * @apiNote GET /admin/students?gradeNo=2022&classNo=1&page=0&size=20
     */
    @Operation(summary = "按年级/班级查询学生")
    @GetMapping("/students")
    public PageResult<StudentCoverResponse> getStudents(
            @RequestParam(required = false) @Nullable Integer gradeNo,
            @RequestParam(required = false) @Nullable Integer classNo,
            @PageableDefault(size = 20) Pageable pageable
    ) throws CustomInvalidArgException {
        return studentService.getAllStudents(gradeNo, classNo, pageable);
    }

    /**
     * 根据学号获取单个学生信息
     *
     * @param stuNo 学生学号
     * @return 学生基本信息
     * @throws NotFoundException 当指定学号的学生不存在时抛出
     * @apiNote GET /admin/students/{stuNo}
     */
    @Operation(summary = "按学号获取学生信息")
    @GetMapping("/students/{stuNo}")
    public StudentCoverResponse getStudentByStuNo(
            @PathVariable @NotNull String stuNo
    ) throws NotFoundException {
        return studentService.getStudentByStuNo(stuNo);
    }

    // =========================== 学生增加、修改、删除 ===========================

    /**
     * 批量新增学生
     * <p>
     * 一次性向系统中添加多个学生记录。所有学生的学号必须唯一，
     * 如果任何一个学号已存在，则整个批量操作都会失败。
     * </p>
     *
     * @param requests 新学生信息请求列表
     * @return 创建成功的学生基本信息列表，HTTP状态码201
     * @throws BadRequestException 当任何学号已存在时抛出
     * @apiNote POST /admin/students
     */
    @Operation(summary = "新增学生")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/students")
    public List<StudentCoverResponse> createStudents(
            @RequestBody @Valid List<NewStudentRequest> requests
    ) throws BadRequestException {
        return studentService.addStudents(requests);
    }

    /**
     * 更新学生信息
     * <p>
     * 根据学号更新学生的基本信息，包括学号本身（新学号必须唯一）。
     * 如果原学号不存在或新学号与已有学生冲突，操作将失败。
     * </p>
     *
     * @param stuNo   要更新的学生原学号
     * @param request 新的学生信息
     * @return 更新后的学生基本信息
     * @throws NotFoundException   当原学号不存在时
     * @throws BadRequestException 当新学号与已有学生冲突时, 或新学生的信息不合法时
     * @apiNote PUT /admin/students/{stuNo}
     */
    @Operation(summary = "更新学生信息")
    @PutMapping("/students/{stuNo}")
    public StudentCoverResponse updateStudent(
            @PathVariable @NotNull String stuNo,
            @RequestBody @Valid NewStudentRequest request
    ) throws BadRequestException {
        return studentService.updateStudent(stuNo, request);
    }

    /**
     * 批量删除学生
     * <p>
     * 根据学号列表批量删除学生记录。如果某个学号不存在，该学号会被忽略，
     * 不会影响其他学号的删除操作。
     * </p>
     *
     * @param studentNos 要删除的学生学号列表
     * @return 被删除的学生基本信息列表
     * @apiNote DELETE /admin/students?stuNos=170101,170102,170103
     */
    @Operation(summary = "删除学生")
    @DeleteMapping("/students")
    public List<StudentCoverResponse> deleteStudents(
            @RequestParam("stuNos") @NotNull List<String> studentNos
    ) {
        return studentService.deleteStudents(studentNos);
    }

}
