package com.gaokao.data.dto;

import lombok.Data;

import java.util.List;

/**
 * 历年分数线查询 DTO
 */
@Data
public class AdmissionScoreQueryDTO {

    /**
     * 当前页
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;

    /**
     * 院校 ID
     */
    private Long universityId;

    /**
     * 院校名称（模糊搜索）
     */
    private String universityName;

    /**
     * 专业 ID
     */
    private Long majorId;

    /**
     * 省份
     */
    private String province;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 年份列表（查询多年）
     */
    private List<Integer> years;

    /**
     * 批次（提前批/本科批/专科批）
     */
    private String batch;

    /**
     * 科类（物理类/历史类/理科/文科）
     */
    private String subjectType;

    /**
     * 最低分下限
     */
    private Integer minScoreFrom;

    /**
     * 最低分上限
     */
    private Integer minScoreTo;

    /**
     * 最低位次上限
     */
    private Integer minRankTo;

    /**
     * 院校层次（985/211/双一流/普通）
     */
    private String universityLevel;

    /**
     * 排序字段（year/minScore/minRank）
     */
    private String sortField;

    /**
     * 排序方式（asc/desc）
     */
    private String sortOrder = "desc";
}