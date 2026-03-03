package org.example.gezhiplatform.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.archive.ArchiveManagementService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 档案管理控制器
 * <p>
 * 提供学生档案的管理员级操作，包括重置单个或批量档案。
 * 该接口仅面向【系统管理员】权限。
 * </p>
 */
@SaCheckRole("SUPER_ADMIN") // 仅超级管理员可访问
@RestController
@RequestMapping("/admin")
@Tag(name = "档案维护(面向管理员)", description = "学生档案的管理员级操作接口")
public class ArchiveManagementController {

    private final ArchiveManagementService archiveManagementService;

    public ArchiveManagementController(ArchiveManagementService archiveManagementService) {
        this.archiveManagementService = archiveManagementService;
    }

    // =========================== 档案重置 ===========================

    /**
     * 重置指定学号学生的档案
     * <p>
     * 将指定学号学生的档案清空为初始状态，可用于初始化或清除学生的档案数据。
     * 学生记录本身不受影响，仅档案内容被重置。
     * </p>
     *
     * @param stuNo 学生学号
     * @throws NotFoundException 当指定学号的学生不存在时抛出
     * @apiNote PUT /admin/archives/{stuNo}/reset
     */
    @Operation(summary = "重置学生档案")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/archives/{stuNo}/reset")
    public void resetArchive(
            @PathVariable @NotNull String stuNo
    ) throws NotFoundException {
        archiveManagementService.resetArchive(stuNo);
    }

    /**
     * 批量重置学生档案
     * <p>
     * 一次性重置多名学生的档案，将其清空为初始状态。
     * 如果任何一个学号不存在，整个批量操作都会失败。
     * </p>
     *
     * @param stuNos 要重置档案的学生学号列表
     * @throws NotFoundException 当任一学号对应的学生不存在时抛出
     * @apiNote PUT /admin/archives/reset?stuNos=170101,170102,170103
     */
    @Operation(summary = "批量重置学生档案")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/archives/reset")
    public void resetArchives(
            @RequestParam("stuNos") @NotNull List<String> stuNos
    ) throws NotFoundException {
        archiveManagementService.resetArchive(stuNos);
    }

}
