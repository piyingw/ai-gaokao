package com.gaokao.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 政策文档实体
 */
@Data
@TableName("policy_document")
public class PolicyDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档类型（政策文件/招生简章/常见问题等）
     */
    private String type;

    /**
     * 适用省份
     */
    private String province;

    /**
     * 适用年份
     */
    private Integer year;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 关键词（JSON数组）
     */
    private String keywords;

    /**
     * 来源
     */
    private String source;

    /**
     * 来源URL
     */
    private String sourceUrl;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 向量ID（PGVector）
     */
    private String vectorId;

    /**
     * 状态（0-禁用 1-启用）
     */
    private Integer status;

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