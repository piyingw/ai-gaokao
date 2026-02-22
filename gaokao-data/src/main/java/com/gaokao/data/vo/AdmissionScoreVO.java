package com.gaokao.data.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 历年分数线响应 VO
 */
@Data
public class AdmissionScoreVO {

    /**
     * 记录 ID
     */
    private Long id;

    /**
     * 院校 ID
     */
    private Long universityId;

    /**
     * 院校名称
     */
    private String universityName;

    /**
     * 院校代码
     */
    private String universityCode;

    /**
     * 院校层次
     */
    private String universityLevel;

    /**
     * 院校类型
     */
    private String universityType;

    /**
     * 院校所在省份
     */
    private String universityProvince;

    /**
     * 院校所在城市
     */
    private String universityCity;

    /**
     * 专业 ID
     */
    private Long majorId;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 招生省份
     */
    private String province;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 批次
     */
    private String batch;

    /**
     * 科类
     */
    private String subjectType;

    /**
     * 最低分
     */
    private Integer minScore;

    /**
     * 平均分
     */
    private BigDecimal avgScore;

    /**
     * 最高分
     */
    private Integer maxScore;

    /**
     * 最低位次
     */
    private Integer minRank;

    /**
     * 最高位次
     */
    private Integer maxRank;

    /**
     * 招生人数
     */
    private Integer enrollment;

    /**
     * 录取人数
     */
    private Integer admitted;

    /**
     * 录取率
     */
    private BigDecimal admissionRate;
}