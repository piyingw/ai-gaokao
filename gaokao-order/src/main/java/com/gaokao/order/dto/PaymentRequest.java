package com.gaokao.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付请求DTO
 */
@Data
public class PaymentRequest {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 商品名称（用于支付页面展示）
     */
    private String productName;

    /**
     * 支付描述
     */
    private String description;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 支付超时时间（分钟）
     */
    private Integer expireMinutes;

    /**
     * 支付方式
     */
    private String paymentMethod;
}