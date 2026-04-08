package com.gaokao.order.statemachine;

import lombok.Getter;

/**
 * 订单状态变更事件
 *
 * 用于状态变更通知，可与消息队列结合
 */
@Getter
public class OrderStateEvent {

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
     * 原状态
     */
    private String fromStatus;

    /**
     * 新状态
     */
    private String toStatus;

    /**
     * 触发时间
     */
    private java.time.LocalDateTime eventTime;

    /**
     * 事件来源（SYSTEM/USER/CALLBACK）
     */
    private String source;

    /**
     * 备注
     */
    private String remark;

    public OrderStateEvent(Long orderId, String orderNo, Long userId,
                          String fromStatus, String toStatus, String source) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.eventTime = java.time.LocalDateTime.now();
        this.source = source;
    }

    /**
     * 创建状态变更事件
     */
    public static OrderStateEvent of(Long orderId, String orderNo, Long userId,
                                     String fromStatus, String toStatus, String source) {
        return new OrderStateEvent(orderId, orderNo, userId, fromStatus, toStatus, source);
    }
}