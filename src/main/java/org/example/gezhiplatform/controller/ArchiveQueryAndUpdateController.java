package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckDisable;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.archive.ArchiveQueryService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 档案查询控制器
 * <p>
 * 该控制器面向所有已登录用户，提供学生档案信息的查询功能。
 * 返回的档案数据会根据当前用户的权限进行字段级别的过滤，只显示用户有权查看的字段。
 * </p>
 * <p>
 * <b>权限控制</b>：
 * <ul>
 *   <li>用户必须已登录才能访问档案查询接口</li>
 *   <li>查询结果受到当前用户的权限控制，只返回用户有权访问的字段</li>
 *   <li>查询操作会在审计日志中留下记录</li>
 * </ul>
 * </p>
 */
@SaCheckDisable
@RestController
@RequestMapping("/archive/students")
@Tag(name = "档案查询与更新", description = "学生档案信息的查询与更新接口")
public class ArchiveQueryAndUpdateController {

    private final ArchiveQueryService archiveQueryService;

    public ArchiveQueryAndUpdateController(ArchiveQueryService archiveQueryService) {
        this.archiveQueryService = archiveQueryService;
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
     * @param stuNo 要导出的学生学号
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
    ) throws BadRequestException{
        Long currentUserId = StpUtil.getLoginIdAsLong();
        String fileName = archiveQueryService.getExportFileName(stuNo);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        ServletOutputStream outputStream;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            throw new BadRequestException("在导出时发生错误(追踪点:AQC01):" + e.getMessage());
        }
        archiveQueryService.exportByStuNo(currentUserId, stuNo, outputStream);
    }

}