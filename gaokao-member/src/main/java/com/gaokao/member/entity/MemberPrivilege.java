package com.gaokao.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员权益配置实体
 *
 * 设计说明：
 * - 不同会员等级对应不同的权益配置
 * - 权益以code形式标识，便于动态扩展
 * - limit_count表示每日使用次数限制，-1表示无限制
 */
@Data
@TableName("member_privilege")
public class MemberPrivilege implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权益配置 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会员等级：FREE/NORMAL/VIP
     */
    private String level;

    /**
     * 权益代码（唯一标识）
     * 例如：AI_CHAT, UNIVERSITY_QUERY, RECOMMEND, ONE_CLICK_GENERATE
     */
    private String privilegeCode;

    /**
     * 权益名称
     */
    private String privilegeName;

    /**
     * 每日使用次数限制
     * -1 表示无限制，正数表示每日最大次数
     */
    private Integer limitCount;

    /**
     * 权益描述
     */
    private String description;

    /**
     * 权益状态：0-禁用 1-启用
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
}