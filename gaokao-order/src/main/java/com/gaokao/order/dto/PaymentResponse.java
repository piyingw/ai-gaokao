package com.gaokao.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付响应DTO
 */
@Data
public class PaymentResponse {

    /**
     * 支付是否成功
     */
    private boolean success;

    /**
     * 支付流水号（第三方）
     */
    private String paymentNo;

    /**
     * 支付链接（H5支付跳转链接）
     */
    private String payUrl;

    /**
     * 支付二维码内容（扫码支付）
     */
    private String qrCode;

    /**
     * 支付状态：SUCCESS/FAILED/PENDING
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
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误代码
     */
    private String errorCode;

    public static PaymentResponse success(String paymentNo) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(true);
        response.setPaymentNo(paymentNo);
        response.setStatus("SUCCESS");
        return response;
    }

    public static PaymentResponse pending(String paymentNo, String payUrl) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(true);
        response.setPaymentNo(paymentNo);
        response.setPayUrl(payUrl);
        response.setStatus("PENDING");
        return response;
    }

    public static PaymentResponse fail(String errorCode, String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setStatus("FAILED");
        return response;
    }
}