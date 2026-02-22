package com.gaokao.data.vo;

import lombok.Data;

/**
 * 院校列表 VO
 */
@Data
public class UniversityVO {

    private Long id;

    private String name;

    private String code;

    private String province;

    private String city;

    private String level;

    private String type;

    private String nature;

    private Integer ranking;

    private String tags;

    /**
     * 去年最低分（用于列表展示）
     */
    private Integer lastYearMinScore;

    /**
     * 去年最低位次
     */
    private Integer lastYearMinRank;
}