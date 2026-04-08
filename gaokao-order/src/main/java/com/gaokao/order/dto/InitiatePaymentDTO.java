package com.gaokao.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起支付DTO
 */
@Data
public class InitiatePaymentDTO {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付方式：MOCK/WECHAT/ALIPAY
     */
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;
}