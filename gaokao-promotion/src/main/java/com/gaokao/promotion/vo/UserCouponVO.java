package com.gaokao.promotion.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券VO
 */
@Data
public class UserCouponVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 优惠券编码
     */
    private String couponCode;

    /**
     * 优惠券名称
     */
    private String couponName;

    /**
     * 优惠券类型
     */
    private String couponType;

    /**
     * 优惠券类型名称
     */
    private String couponTypeName;

    /**
     * 优惠券值
     */
    private BigDecimal couponValue;

    /**
     * 最低消费金额
     */
    private BigDecimal minAmount;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 有效期开始时间
     */
    private LocalDateTime startTime;

    /**
     * 有效期结束时间
     */
    private LocalDateTime endTime;

    /**
     * 是否可用
     */
    private Boolean available;

    /**
     * 不可用原因
     */
    private String unavailableReason;
}