package com.gaokao.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 院校实体
 */
@Data
@TableName("university")
public class University implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 院校 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 院校名称
     */
    private String name;

    /**
     * 院校代码
     */
    private String code;

    /**
     * 所在省份
     */
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 院校层次（985/211/双一流/普通）
     */
    private String level;

    /**
     * 院校类型（综合/理工/师范/医药等）
     */
    private String type;

    /**
     * 办学性质（公办/民办/中外合作）
     */
    private String nature;

    /**
     * 综合排名
     */
    private Integer ranking;

    /**
     * 院校简介
     */
    private String intro;

    /**
     * 特色专业（JSON 数组）
     */
    private String features;

    /**
     * 招生网地址
     */
    private String admissionUrl;

    /**
     * 官网地址
     */
    private String officialUrl;

    /**
     * 院校标签（JSON 数组）
     */
    private String tags;

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