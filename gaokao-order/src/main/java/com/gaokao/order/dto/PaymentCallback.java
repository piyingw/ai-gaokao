package com.gaokao.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调DTO
 *
 * 用于接收支付平台的回调数据
 */
@Data
public class PaymentCallback {

    /**
     * 订单号（商户订单号）
     */
    private String orderNo;

    /**
     * 支付流水号（第三方）
     */
    private String paymentNo;

    /**
     * 支付状态：SUCCESS/FAILED
     */
    private String status;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付时间
     */
    private String payTime;

    /**
     * 回调签名（用于验证回调真实性）
     */
    private String sign;

    /**
     * 回调原始数据（JSON格式）
     */
    private String rawData;

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 错误信息（支付失败时）
     */
    private String errorMessage;

    /**
     * 错误代码
     */
    private String errorCode;
}