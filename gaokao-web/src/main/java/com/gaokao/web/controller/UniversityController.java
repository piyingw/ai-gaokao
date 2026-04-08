package com.gaokao.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.common.result.Result;
import com.gaokao.data.dto.UniversityQueryDTO;
import com.gaokao.data.service.UniversityService;
import com.gaokao.data.vo.UniversityDetailVO;
import com.gaokao.data.vo.UniversityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 院校控制器
 */
@Tag(name = "院校管理", description = "院校查询、详情、对比")
@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @Operation(summary = "分页查询院校")
    @GetMapping("/list")
    public Result<Page<UniversityVO>> pageList(UniversityQueryDTO dto) {
        Page<UniversityVO> page = universityService.pageList(dto);
        return Result.success(page);
    }

    @Operation(summary = "获取院校详情")
    @GetMapping("/{id}")
    public Result<UniversityDetailVO> getDetail(@PathVariable Long id) {
        UniversityDetailVO detail = universityService.getDetail(id);
        return Result.success(detail);
    }

    @Operation(summary = "获取院校历年分数线")
    @GetMapping("/{id}/scores")
    public Result<Object> getScores(
            @PathVariable Long id,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String subjectType) {
        Object scores = universityService.getScores(id, province, subjectType);
        return Result.success(scores);
    }

    @Operation(summary = "获取院校开设专业")
    @GetMapping("/{id}/majors")
    public Result<Object> getMajors(@PathVariable Long id) {
        Object majors = universityService.getMajors(id);
        return Result.success(majors);
    }

    @Operation(summary = "院校对比")
    @PostMapping("/compare")
    public Result<Object> compare(@RequestBody Long[] ids) {
        Object result = universityService.compare(ids);
        return Result.success(result);
    }
}