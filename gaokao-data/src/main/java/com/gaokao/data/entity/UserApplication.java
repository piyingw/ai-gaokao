package com.gaokao.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户志愿表实体
 */
@Data
@TableName("user_application")
public class UserApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 志愿 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 志愿方案名称
     */
    private String name;

    /**
     * 高考分数
     */
    private Integer score;

    /**
     * 省份
     */
    private String province;

    /**
     * 科类（物理类/历史类）
     */
    private String subjectType;

    /**
     * 志愿列表（JSON）
     */
    private String applications;

    /**
     * 状态（0-草稿 1-已提交）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

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