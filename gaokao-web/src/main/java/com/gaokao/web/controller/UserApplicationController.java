package com.gaokao.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.common.result.Result;
import com.gaokao.data.dto.UserApplicationDTO;
import com.gaokao.data.service.UserApplicationService;
import com.gaokao.data.vo.UserApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户志愿控制器
 */
@Tag(name = "用户志愿", description = "用户志愿管理接口")
@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
public class UserApplicationController {

    private final UserApplicationService userApplicationService;

    @Operation(summary = "分页查询志愿列表")
    @GetMapping("/list")
    public Result<Page<UserApplicationVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.pageList(userId, pageNum, pageSize));
    }

    @Operation(summary = "获取志愿详情")
    @GetMapping("/{id}")
    public Result<UserApplicationVO> getDetail(
            @Parameter(description = "志愿ID") @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.getDetail(id, userId));
    }

    @Operation(summary = "创建志愿")
    @PostMapping
    public Result<Long> create(
            @RequestBody UserApplicationDTO dto,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.create(userId, dto));
    }

    @Operation(summary = "更新志愿")
    @PutMapping
    public Result<Boolean> update(
            @RequestBody UserApplicationDTO dto,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.update(userId, dto));
    }

    @Operation(summary = "删除志愿")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "志愿ID") @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.delete(id, userId));
    }

    @Operation(summary = "提交志愿")
    @PostMapping("/{id}/submit")
    public Result<Boolean> submit(
            @Parameter(description = "志愿ID") @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.submit(id, userId));
    }

    @Operation(summary = "复制志愿")
    @PostMapping("/{id}/copy")
    public Result<Long> copy(
            @Parameter(description = "志愿ID") @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.copy(id, userId));
    }

    @Operation(summary = "获取最新志愿")
    @GetMapping("/latest")
    public Result<UserApplicationVO> getLatest(@RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.getLatest(userId));
    }

    @Operation(summary = "分析志愿方案")
    @GetMapping("/{id}/analyze")
    public Result<Object> analyze(
            @Parameter(description = "志愿ID") @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return Result.success(userApplicationService.analyze(id, userId));
    }
}