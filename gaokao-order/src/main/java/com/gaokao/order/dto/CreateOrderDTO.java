package com.gaokao.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {

    /**
     * 商品ID
     * 1=普通会员年卡, 2=VIP会员年卡, 3=普通会员月卡, 4=VIP会员月卡
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 订单类型：MEMBERSHIP/SERVICE
     */
    private String orderType = "MEMBERSHIP";

    /**
     * 优惠券ID（可选）
     */
    private Long couponId;
}