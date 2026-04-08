package com.gaokao.promotion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体
 *
 * 设计说明：
 * - 用户领取的优惠券实例
 * - coupon_code为唯一标识，用于使用时校验
 * - status管理优惠券状态：UNUSED/USED/EXPIRED
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券模板ID
     */
    private Long templateId;

    /**
     * 优惠券编码（唯一）
     */
    private String couponCode;

    /**
     * 优惠券名称（冗余字段，便于查询）
     */
    private String couponName;

    /**
     * 优惠券类型
     */
    private String couponType;

    /**
     * 优惠券值
     */
    private BigDecimal couponValue;

    /**
     * 最低消费金额
     */
    private BigDecimal minAmount;

    /**
     * 状态：UNUSED-未使用 / USED-已使用 / EXPIRED-已过期
     */
    private String status;

    /**
     * 使用时间
     */
    private LocalDateTime useTime;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 有效期开始时间
     */
    private LocalDateTime startTime;

    /**
     * 有效期结束时间
     */
    private LocalDateTime endTime;

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