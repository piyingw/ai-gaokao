package com.gaokao.data.vo;

import lombok.Data;

/**
 * 院校详情 VO
 */
@Data
public class UniversityDetailVO {

    private Long id;

    private String name;

    private String code;

    private String province;

    private String city;

    private String level;

    private String type;

    private String nature;

    private Integer ranking;

    private String intro;

    private String features;

    private String admissionUrl;

    private String officialUrl;

    private String tags;

    /**
     * 历年分数线统计
     */
    private Object scoreStats;

    /**
     * 开设专业列表
     */
    private Object majors;
}