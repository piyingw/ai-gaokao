package com.gaokao.promotion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体
 *
 * 设计说明：
 * - 优惠券模板定义优惠券的基本属性
 * - total_count表示总发行量，used_count表示已领取数量
 * - 通过Redis原子操作实现库存扣减
 */
@Data
@TableName("coupon_template")
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券类型：DISCOUNT（折扣券）/ FULL_REDUCTION（满减券）
     */
    private String type;

    /**
     * 优惠券值（折扣券为折扣率如0.9，满减券为减免金额）
     */
    private BigDecimal value;

    /**
     * 最低消费金额（满减门槛）
     */
    private BigDecimal minAmount;

    /**
     * 总发行量
     */
    private Integer totalCount;

    /**
     * 已领取数量
     */
    private Integer usedCount;

    /**
     * 每人限领数量
     */
    private Integer limitPerUser;

    /**
     * 有效期开始时间
     */
    private LocalDateTime startTime;

    /**
     * 有效期结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 描述
     */
    private String description;

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