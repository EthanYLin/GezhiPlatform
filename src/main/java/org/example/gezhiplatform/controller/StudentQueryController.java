package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckDisable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.student.StudentCoverResponse;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.service.auth.AuthService;
import org.example.gezhiplatform.service.student.StudentQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生查询控制器
 * <p>
 * 该控制器面向【所有用户】，提供学生基本信息的查询功能，包括：
 * <ul>
 *   <li>1. 按照年级/班级查询学生名单，名单内只包括学号、姓名等基本信息</li>
 *   <li>2. 按照学号、姓名或手机号查找学生，查找结果只包括学号、姓名等基本信息</li>
 *   <li>3. 获取用户可访问的所有班级列表</li>
 * </ul>
 * </p>
 * <p>
 * <b>权限控制</b>：查询结果受到当前用户的权限控制，用户只能查看其权限范围内的学生信息和班级信息。
 * </p>
 * <p>
 * <ul>
 *   <li>系统管理员维护学生信息请使用 {@link StudentManagementController}</li>
 *   <li>查询学生具体档案信息请使用 {@link ArchiveQueryAndUpdateController}</li>
 * </ul>
 * </p>
 * 
 */
@SaCheckDisable
@RestController
@RequestMapping("/students")
@Tag(name = "学生查询(面向所有用户)", description = "学生基本信息的查询接口")
public class StudentQueryController {

    private final AuthService authService;
    private final StudentQueryService studentQueryService;

    public StudentQueryController(AuthService authService, StudentQueryService studentQueryService) {
        this.authService = authService;
        this.studentQueryService = studentQueryService;
    }

    /**
     * 获取当前用户可访问的所有班级
     * <p>
     * 根据用户的角色权限，返回该用户可以访问的所有班级列表：
     * <ul>
     *   <li>班主任：返回管理的班级</li>
     *   <li>年级组长：返回该年级的所有班级</li>
     *   <li>多班级观察员：返回所有关联的班级</li>
     *   <li>校级领导及超级管理员：返回学校中的所有班级</li>
     *   <li>协同用户：返回所有关联的学生所在班级</li>
     *   <li>学生本人及家长：返回学生所在班级</li>
     *   <li>其他角色：返回空列表</li>
     * </ul>
     * 如果一个用户有多个角色，取所有角色权限范围内班级的并集。
     * </p>
     * 
     * @return 该用户可访问的班级列表，按年级和班级升序排列
     * @apiNote GET /students/classes
     */
    @GetMapping("/classes")
    @Transactional
    @Operation(summary = "获取当前用户可访问的所有班级")
    public List<GradeClass> getAllAccessibleClasses() {
        return studentQueryService.getAllAccessibleClasses(authService.getCurrentUser());
    }

    /**
     * 根据条件搜索学生
     * <p>
     * 支持<b>年级、班级、关键词</b>的组合查询。所有查询条件使用<b>AND逻辑</b>连接，
     * 关键词内部使用<b>OR逻辑</b>在学号、姓名、手机号、父母手机号、父母姓名之间进行匹配。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出异常</b></p>
     * <p>
     * 查询条件组合方式：
     * <ul>
     *   <li><b>年级过滤</b>：当gradeNo不为null时，只返回指定年级的学生</li>
     *   <li><b>班级过滤</b>：当classNo不为null时，只返回指定班级的学生</li>
     *   <li><b>关键词搜索</b>：当keyword不为null且非空时，在学号、姓名进行模糊匹配，在手机号、父母手机号、父母姓名进行精确匹配</li>
     *   <li><b>组合查询</b>：多个条件同时存在时，使用AND逻辑连接</li>
     *   <li><b>无条件查询</b>：所有参数为null时，返回用户权限范围内的所有学生</li>
     * </ul>
     * </p>
     * <p>
     * 使用场景举例：
     * <ul>
     *   <li>查找2026届所有学生：gradeNo=2026, classNo=null, keyword=null</li>
     *   <li>查找2026届1班所有学生：gradeNo=2026, classNo=1, keyword=null</li>
     *   <li>在2026届中搜索姓名包含"张"的学生：gradeNo=2026, classNo=null, keyword="张"</li>
     *   <li>在2026届1班中搜索学号包含"01"的学生：gradeNo=2026, classNo=1, keyword="01"</li>
     *   <li>全局搜索自己或父母手机号为"13800138000"的学生：gradeNo=null, classNo=null, keyword="13800138000"</li>
     *   <li>全局搜索父母姓名为"张三"的学生：gradeNo=null, classNo=null, keyword="张三"</li>
     * </ul>
     * </p>
     * <p>
     * <b>权限控制</b>：
     * 所有查询结果都会受到用户权限的限制，只返回用户有权查看的学生。
     * 即使指定了具体的年级或班级，如果用户没有相应权限，结果也会为空或部分显示。
     * </p>
     *
     * @param gradeNo  年级过滤条件，为null时不进行年级过滤，例如：2026
     * @param classNo  班级过滤条件，为null时不进行班级过滤，例如：1
     * @param keyword  关键词搜索条件，为null或空白时不进行关键词搜索，支持学号、姓名模糊匹配，手机号、父母手机号、父母姓名精确匹配
     * @param pageable 分页参数，默认每页20条记录，最大页大小为1000
     * @return 符合所有条件且在用户权限范围内的学生基本信息分页结果
     * @apiNote GET /students?gradeNo=2026&classNo=1&keyword=张三&page=0&size=20&sort=stuNo,asc
     */
    @GetMapping
    @Transactional
    @Operation(summary = "根据条件搜索学生", 
              description = "支持年级、班级、关键词的组合查询。关键词支持学号、姓名模糊匹配，手机号、父母手机号、父母姓名精确匹配")
    public PageResult<StudentCoverResponse> searchStudents(
        @Parameter(description = "年级号，例如：2026", example = "2026")
        @RequestParam(required = false) Integer gradeNo,
        
        @Parameter(description = "班级号，例如：1", example = "1") 
        @RequestParam(required = false) Integer classNo,
        
        @Parameter(description = "搜索关键词，支持学号、姓名模糊匹配，手机号、父母信息精确匹配", example = "张三")
        @RequestParam(required = false) String keyword,
        
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return studentQueryService.searchStudents(gradeNo, classNo, keyword, authService.getCurrentUser(), pageable);
    }
}
