package com.gaokao.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.common.result.Result;
import com.gaokao.data.dto.PolicyQueryDTO;
import com.gaokao.data.service.PolicyDocumentService;
import com.gaokao.data.vo.PolicyDocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 政策文档控制器
 */
@Tag(name = "政策文档", description = "政策文档管理接口")
@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class PolicyDocumentController {

    private final PolicyDocumentService policyDocumentService;

    @Operation(summary = "分页查询政策文档")
    @GetMapping("/list")
    public Result<Page<PolicyDocumentVO>> list(PolicyQueryDTO dto) {
        return Result.success(policyDocumentService.pageList(dto));
    }

    @Operation(summary = "获取政策文档详情")
    @GetMapping("/{id}")
    public Result<PolicyDocumentVO> getDetail(
            @Parameter(description = "文档ID") @PathVariable Long id) {
        return Result.success(policyDocumentService.getDetail(id));
    }

    @Operation(summary = "搜索政策文档")
    @GetMapping("/search")
    public Result<List<PolicyDocumentVO>> search(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(policyDocumentService.search(keyword, limit));
    }

    @Operation(summary = "按类型获取政策文档")
    @GetMapping("/type/{type}")
    public Result<List<PolicyDocumentVO>> listByType(
            @Parameter(description = "文档类型") @PathVariable String type) {
        return Result.success(policyDocumentService.listByType(type));
    }

    @Operation(summary = "按省份获取政策文档")
    @GetMapping("/province/{province}")
    public Result<List<PolicyDocumentVO>> listByProvince(
            @Parameter(description = "省份") @PathVariable String province) {
        return Result.success(policyDocumentService.listByProvince(province));
    }

    @Operation(summary = "获取热门政策")
    @GetMapping("/hot")
    public Result<List<PolicyDocumentVO>> getHotPolicies(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(policyDocumentService.getHotPolicies(limit));
    }

    @Operation(summary = "获取政策文档类型列表")
    @GetMapping("/types")
    public Result<List<String>> listTypes() {
        return Result.success(policyDocumentService.listTypes());
    }
}