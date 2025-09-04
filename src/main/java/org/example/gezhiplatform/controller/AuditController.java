package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.audit.AuditRecordResponse;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.service.audit.AuditService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 审计日志查询控制器
 * <p>
 * 该控制器面向【系统管理员】，提供审计日志的查询功能，包括：
 * <ul>
 *   <li>1. 根据时间范围查询审计记录</li>
 *   <li>2. 根据操作类型过滤审计记录</li>
 *   <li>3. 根据用户名查询特定用户的操作记录</li>
 *   <li>4. 根据操作详情关键词搜索相关记录</li>
 *   <li>5. 支持多条件组合查询和分页返回</li>
 * </ul>
 * </p>
 * <p>
 * <b>该控制器仅面向【超级管理员】，所有操作都需要SUPER_ADMIN权限。</b>
 * </p>
 */
@SaCheckRole("SUPER_ADMIN")
@RestController
@RequestMapping("/admin/audit")
@Tag(name = "审计日志查询(面向管理员)", description = "系统审计日志的查询接口")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * 根据条件搜索审计记录
     * <p>
     * 支持<b>时间范围、操作类型、用户名、关键词</b>的组合查询。所有查询条件使用<b>AND逻辑</b>连接，
     * 关键词内部使用<b>模糊匹配</b>在操作详情中进行搜索。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出异常</b></p>
     * <p>
     * 查询条件组合方式：
     * <ul>
     *   <li><b>时间范围过滤</b>：当startTime不为null时，只返回该时间之后的记录；当endTime不为null时，只返回该时间之前的记录</li>
     *   <li><b>操作类型过滤</b>：当operation不为null时，只返回指定操作类型的记录</li>
     *   <li><b>用户名过滤</b>：当username不为null且非空时，只返回指定用户的操作记录（完全匹配）</li>
     *   <li><b>关键词搜索</b>：当keyword不为null且非空时，在操作详情中进行模糊匹配</li>
     *   <li><b>组合查询</b>：多个条件同时存在时，使用AND逻辑连接</li>
     *   <li><b>无条件查询</b>：所有参数为null时，返回系统中的所有审计记录</li>
     * </ul>
     * </p>
     * <p>
     * 使用场景举例：
     * <ul>
     *   <li>查找今日所有操作记录：startTime=今日00:00:00, endTime=今日23:59:59, operation=null, username=null, keyword=null</li>
     *   <li>查找所有档案查询操作：startTime=null, endTime=null, operation=档案查询, username=null, keyword=null</li>
     *   <li>查找某用户的所有操作：startTime=null, endTime=null, operation=null, username="admin", keyword=null</li>
     *   <li>搜索包含"张三"的操作记录：startTime=null, endTime=null, operation=null, username=null, keyword="张三"</li>
     *   <li>查找某用户在特定时间的档案查询：startTime=指定时间, endTime=指定时间, operation=档案查询, username="teacher001", keyword=null</li>
     * </ul>
     * </p>
     *
     * @param startTime 开始时间过滤条件，为null时不进行开始时间过滤，格式：yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间过滤条件，为null时不进行结束时间过滤，格式：yyyy-MM-dd HH:mm:ss
     * @param operation 操作类型过滤条件，为null时不进行操作类型过滤，可选值：档案查询、档案导出
     * @param username  操作用户用户名过滤条件，为null或空白时不进行用户过滤，完全匹配
     * @param keyword   关键词搜索条件，为null或空白时不进行关键词搜索，在操作详情中模糊匹配
     * @param pageable  分页参数，默认每页20条记录，最大页大小为1000
     * @return 符合所有条件的审计记录分页结果
     * @throws BadRequestException 当分页大小超过1000或排序字段无效时抛出
     * @apiNote GET /admin/audit?startTime=2024-01-01 00:00:00&endTime=2024-01-31 23:59:59&operation=档案查询&username=admin&keyword=张三&page=0&size=20&sort=time,desc
     */
    @GetMapping
    @Transactional
    @Operation(
        summary = "根据条件搜索审计记录", 
        description = "支持时间范围、操作类型、用户名、关键词的组合查询。关键词支持操作详情模糊匹配"
    )
    public PageResult<AuditRecordResponse> searchAuditRecords(
        @Parameter(description = "开始时间，格式：yyyy-MM-dd HH:mm:ss", example = "2024-01-01 00:00:00")
        @RequestParam(required = false) 
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") 
        LocalDateTime startTime,
        
        @Parameter(description = "结束时间，格式：yyyy-MM-dd HH:mm:ss", example = "2024-01-31 23:59:59")
        @RequestParam(required = false) 
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") 
        LocalDateTime endTime,
        
        @Parameter(description = "操作类型过滤", example = "档案查询")
        @RequestParam(required = false) 
        String operation,
        
        @Parameter(description = "操作用户用户名（完全匹配）", example = "admin")
        @RequestParam(required = false) 
        String username,
        
        @Parameter(description = "搜索关键词，支持操作详情模糊匹配")
        @RequestParam(required = false) 
        String keyword,
        
        @PageableDefault(size = 20) Pageable pageable
    ) throws BadRequestException {
        AuditOperationType operationType = null;
        if (operation != null && !operation.isBlank()) operationType = AuditOperationType.fromDesc(operation);
        return auditService.searchAuditRecords(startTime, endTime, keyword, operationType, username, pageable);
    }
}
