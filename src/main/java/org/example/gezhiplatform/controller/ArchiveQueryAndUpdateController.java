package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckDisable;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.archive.ArchiveQueryService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}