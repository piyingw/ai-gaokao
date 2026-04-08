package com.gaokao.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.common.result.Result;
import com.gaokao.data.dto.MajorQueryDTO;
import com.gaokao.data.service.MajorService;
import com.gaokao.data.vo.MajorDetailVO;
import com.gaokao.data.vo.MajorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业控制器
 */
@Tag(name = "专业管理", description = "专业信息查询接口")
@RestController
@RequestMapping("/api/major")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    @Operation(summary = "分页查询专业")
    @GetMapping("/list")
    public Result<Page<MajorVO>> list(MajorQueryDTO dto) {
        return Result.success(majorService.pageList(dto));
    }

    @Operation(summary = "获取专业详情")
    @GetMapping("/{id}")
    public Result<MajorDetailVO> getDetail(
            @Parameter(description = "专业ID") @PathVariable Long id) {
        return Result.success(majorService.getDetail(id));
    }

    @Operation(summary = "按学科门类获取专业")
    @GetMapping("/category/{category}")
    public Result<List<MajorVO>> listByCategory(
            @Parameter(description = "学科门类") @PathVariable String category) {
        return Result.success(majorService.listByCategory(category));
    }

    @Operation(summary = "搜索专业")
    @GetMapping("/search")
    public Result<List<MajorVO>> search(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(majorService.search(keyword, limit));
    }

    @Operation(summary = "根据性格推荐专业")
    @GetMapping("/recommend")
    public Result<List<MajorVO>> recommend(
            @Parameter(description = "性格类型") @RequestParam String personalityType) {
        return Result.success(majorService.recommendByPersonality(personalityType));
    }

    @Operation(summary = "获取所有学科门类")
    @GetMapping("/categories")
    public Result<List<String>> listCategories() {
        return Result.success(majorService.listCategories());
    }

    @Operation(summary = "获取专业类列表")
    @GetMapping("/subcategories")
    public Result<List<String>> listSubCategories(
            @Parameter(description = "学科门类") @RequestParam String category) {
        return Result.success(majorService.listSubCategories(category));
    }
}