package com.gaokao.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 专业实体
 */
@Data
@TableName("major")
public class Major implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 专业 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 专业名称
     */
    private String name;

    /**
     * 专业代码
     */
    private String code;

    /**
     * 学科门类（工学/理学/文学等）
     */
    private String category;

    /**
     * 专业类（计算机类/电子信息类等）
     */
    private String subCategory;

    /**
     * 学位类型（工学学士/理学学士等）
     */
    private String degreeType;

    /**
     * 学制（年）
     */
    private Integer duration;

    /**
     * 专业简介
     */
    private String intro;

    /**
     * 主要课程（JSON 数组）
     */
    private String courses;

    /**
     * 就业方向（JSON 数组）
     */
    private String employment;

    /**
     * 就业前景评分（1-5）
     */
    private Integer employmentRating;

    /**
     * 平均薪资（元/月）
     */
    private Integer avgSalary;

    /**
     * 男女比例
     */
    private String genderRatio;

    /**
     * 选科要求（JSON）
     */
    private String subjectRequirement;

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

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}