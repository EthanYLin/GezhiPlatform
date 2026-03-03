package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckDisable;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.archive.ArchivePermissionDetails;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.GlobalExceptionHandler;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.archive.ArchiveExportService;
import org.example.gezhiplatform.service.archive.ArchiveQueryService;
import org.example.gezhiplatform.service.archive.ArchiveUpdateService;
import org.example.gezhiplatform.service.permission.ArchiveAccessControlService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 档案查询与更新控制器
 * <p>
 * 该控制器面向所有已登录用户，提供学生档案信息的查询、导出和更新功能。
 * 所有操作都会根据当前用户的权限进行字段级别的过滤：
 * <ul>
 *   <li><b>查询操作</b>：只返回用户有权查看的字段</li>
 *   <li><b>导出操作</b>：只导出用户有权查看的字段到Excel文件</li>
 *   <li><b>更新操作</b>：只允许修改用户有权写入的字段</li>
 * </ul>
 * </p>
 * <p>
 * <b>权限控制</b>：
 * <ul>
 *   <li>用户必须已登录才能访问档案接口</li>
 *   <li>用户只能访问其角色范围内的学生档案</li>
 *   <li>所有操作结果受到当前用户权限的限制</li>
 *   <li>查询和导出操作会在审计日志中留下记录</li>
 * </ul>
 * </p>
 */
@SaCheckDisable
@RestController
@RequestMapping("/archive/students")
@Tag(name = "档案查询与更新", description = "学生档案信息的查询与更新接口")
public class ArchiveQueryAndUpdateController {

    private final ArchiveQueryService archiveQueryService;
    private final ArchiveUpdateService archiveUpdateService;
    private final ArchiveExportService archiveExportService;
    private final ArchiveAccessControlService archiveAccessControlService;

    public ArchiveQueryAndUpdateController(
        ArchiveQueryService archiveQueryService,
        ArchiveUpdateService archiveUpdateService,
        ArchiveExportService archiveExportService,
        ArchiveAccessControlService archiveAccessControlService
    ) {
        this.archiveQueryService = archiveQueryService;
        this.archiveUpdateService = archiveUpdateService;
        this.archiveExportService = archiveExportService;
        this.archiveAccessControlService = archiveAccessControlService;
    }

    /**
     * 根据学号查询学生档案
     * <p>
     * 根据提供的学号查询学生的完整档案信息。返回的档案数据会根据当前用户的权限进行过滤，
     * 只包含用户有权查看的字段，无权访问的字段将被移除。
     * </p>
     * <p>
     * <b>审计日志：</b>
     * 每次档案查询操作都会在系统审计日志中记录，包括操作用户、查询的学生信息等。
     * </p>
     *
     * @param stuNo 要查询的学生学号
     * @return 经过权限过滤的学生档案JSON字符串
     * @throws NotFoundException   当指定学号的学生不存在时抛出
     * @throws BadRequestException 当档案序列化失败时抛出
     * @apiNote GET /archive/students/{stuNo}
     */
    @GetMapping(value = "/{stuNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public String queryArchiveByStuNo(
        @Parameter(description = "要查询的学生学号", required = true, example = "260101")
        @PathVariable @NotNull String stuNo
    ) throws NotFoundException, BadRequestException {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return archiveQueryService.queryByStuNo(currentUserId, stuNo);
    }

    /**
     * 查询对指定学号学生的档案权限
     * <p>
     * 根据提供的学号查询当前用户对该学生档案的完整访问权限，
     * 返回结果包括：<br>
     * (a) 当前用户拥有的且可访问该学生的角色范围<br>
     * (b) 当前用户拥有的且可访问该学生的权限组<br>
     * (c) 可读的 JSON Path<br>
     * (d) 可写的 JSON Path<br>
     * </p>
     *
     * @param stuNo 要查询的学生学号
     * @return 学生档案权限详情
     * @throws NotFoundException   当指定学号的学生不存在时抛出
     * @throws BadRequestException 当权限计算失败时抛出
     * @apiNote GET /archive/students/{stuNo}/permission
     */
    @GetMapping(value = "/{stuNo}/permission", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ArchivePermissionDetails queryArchivePermissionByStuNo(
        @Parameter(description = "要查询的学生学号", required = true, example = "260101")
        @PathVariable @NotNull String stuNo
    ) throws NotFoundException, BadRequestException {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return archiveAccessControlService.new UserStudentArchive(currentUserId, stuNo).permissionDetails();
    }

    /**
     * 导出指定学号学生的档案为Excel文件
     * <p>
     * 根据提供的学号导出学生的档案信息为Excel文件。导出的档案数据会根据当前用户的权限进行过滤，
     * 只包含用户有权查看的字段。导出的文件会使用预定义的Excel模板格式。
     * </p>
     * <p>
     * <b>文件处理：</b>
     * <ul>
     *   <li>自动生成包含学生姓名、学号和时间戳的文件名</li>
     *   <li>支持中文文件名的UTF-8编码</li>
     * </ul>
     * </p>
     * <p>
     * <b>审计日志：</b>
     * 每次档案导出操作都会在系统审计日志中记录，包括操作用户、导出的学生信息等。
     * </p>
     *
     * @param stuNo    要导出的学生学号
     * @param response HTTP响应对象，用于设置响应头和获取输出流
     * @throws NotFoundException   当指定学号的学生不存在时抛出
     * @throws BadRequestException 当档案序列化失败、IO错误或Excel处理错误时抛出
     * @apiNote POST /archive/students/{stuNo}/export
     */
    @PostMapping("/{stuNo}/export")
    @Transactional
    public void download(
        @Parameter(description = "要查询的学生学号", required = true, example = "260101")
        @PathVariable @NotNull String stuNo,
        HttpServletResponse response
    ) throws BadRequestException {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        String fileName = archiveExportService.getExportFileName(currentUserId, stuNo);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        ServletOutputStream outputStream;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            var traceId = GlobalExceptionHandler.logError("在导出时发生错误(追踪点:AQC01):", e);
            throw new BadRequestException("在导出时发生错误(追踪点:AQC01), 请持有追踪编号向管理员查询:" + traceId);
        }
        archiveExportService.exportByStuNo(currentUserId, stuNo, outputStream);
    }

    /**
     * 更新指定学号学生的档案
     * <p>
     * 根据提供的学号和JSON数据更新学生的档案信息。
     * </p>
     * <p>
     * <b>权限控制：</b>
     * <ul>
     *   <li>只有当前用户能够访问该学生时才能执行更新操作。</li>
     *   <li>更新操作会根据当前用户的权限进行字段级别的过滤，只有用户具备写入权限的字段才会被实际更新，无权修改的字段将被忽略。</li>
     *   <li>若用户传入某字段为null，则会将该字段设置为null；用户没有传入的字段将不更新。</li>
     *   <li>请求体中不在档案元数据中的额外字段将被忽略。</li>
     *   <li>更新后的档案数据会进行合法性校验(例如手机号与身份证号)。</li>
     * </ul>
     * </p>
     *
     * @param stuNo         要更新的学生学号
     * @param jsonForUpdate 包含更新数据的JSON字符串
     * @throws NotFoundException   当指定学号的学生不存在或当前用户无权访问时抛出
     * @throws BadRequestException 当JSON数据格式错误、权限不足或数据校验失败时抛出
     * @apiNote PUT /archive/students/{stuNo}
     */
    @PutMapping("/{stuNo}")
    @Transactional
    public void updateArchive(
        @Parameter(description = "要更新的学生学号", required = true, example = "260101")
        @PathVariable @NotNull String stuNo,
        @RequestBody @NotNull String jsonForUpdate
    ) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        archiveUpdateService.updateArchive(jsonForUpdate, currentUserId, stuNo);
    }

}