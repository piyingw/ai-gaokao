package com.gaokao.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 历年分数线实体
 */
@Data
@TableName("admission_score")
public class AdmissionScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 院校 ID
     */
    private Long universityId;

    /**
     * 专业 ID（可为空，表示院校分数线）
     */
    private Long majorId;

    /**
     * 招生省份
     */
    private String province;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 批次（提前批/本科批/专科批等）
     */
    private String batch;

    /**
     * 科类（物理类/历史类/理科/文科）
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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}