package com.gaokao.data.dto;

import lombok.Data;

/**
 * 院校查询 DTO
 */
@Data
public class UniversityQueryDTO {

    /**
     * 当前页
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 院校名称（模糊搜索）
     */
    private String name;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 院校层次（985/211/双一流/普通）
     */
    private String level;

    /**
     * 院校类型（综合/理工/师范等）
     */
    private String type;

    /**
     * 办学性质（公办/民办）
     */
    private String nature;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式（asc/desc）
     */
    private String sortOrder;
}