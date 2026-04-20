package com.gaokao.promotion.event;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券领取事件
 *
 * 用于异步处理优惠券秒杀请求
 *
 * 设计说明：
 * - 用户发起领取请求后，先进行资格预检
 * - 预检通过后，发送事件到MQ异步处理
 * - 用户可通过订单号查询领取结果
 */
@Data
public class CouponClaimEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID（用于幂等性校验）
     */
    private String eventId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券模板ID
     */
    private Long templateId;

    /**
     * 请求时间
     */
    private LocalDateTime requestTime;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件类型常量
     */
    public static final String TYPE_CLAIM_REQUEST = "COUPON_CLAIM_REQUEST";
    public static final String TYPE_CLAIM_SUCCESS = "COUPON_CLAIM_SUCCESS";
    public static final String TYPE_CLAIM_FAILED = "COUPON_CLAIM_FAILED";

    /**
     * 预检通过的库存扣减凭证
     */
    private String stockToken;

    /**
     * 创建领取请求事件
     */
    public static CouponClaimEvent createClaimRequest(Long userId, Long templateId, String stockToken) {
        CouponClaimEvent event = new CouponClaimEvent();
        event.setEventId(generateEventId(userId, templateId));
        event.setUserId(userId);
        event.setTemplateId(templateId);
        event.setRequestTime(LocalDateTime.now());
        event.setEventType(TYPE_CLAIM_REQUEST);
        event.setStockToken(stockToken);
        return event;
    }

    /**
     * 生成事件ID
     */
    public static String generateEventId(Long userId, Long templateId) {
        return "CLAIM_" + userId + "_" + templateId + "_" + System.currentTimeMillis();
    }
}