package com.gaokao.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员实体
 *
 * 设计说明：
 * - 会员等级分为三级：FREE(免费)、NORMAL(普通会员)、VIP(高级会员)
 * - 每个用户对应一条会员记录，level初始为FREE
 * - 通过start_time和end_time管理会员有效期
 * - status用于会员状态的快速判断（活跃/过期/冻结）
 */
@Data
@TableName("member")
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会员 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 ID（关联 sys_user 表）
     */
    private Long userId;

    /**
     * 会员等级：FREE/NORMAL/VIP
     */
    private String level;

    /**
     * 会员开始时间（购买/升级时间）
     */
    private LocalDateTime startTime;

    /**
     * 会员结束时间（过期时间）
     */
    private LocalDateTime endTime;

    /**
     * 会员状态：0-已过期 1-正常 2-冻结
     */
    private Integer status;

    /**
     * 累计消费金额（用于统计分析）
     */
    private BigDecimal totalSpent;

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
     * 删除标志 0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}