package com.gaokao.order.message;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单消息实体
 *
 * 用于订单相关的异步消息处理
 */
@Data
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 订单状态
     */
    private String orderStatus;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息时间
     */
    private LocalDateTime messageTime;

    /**
     * 延迟级别（用于延迟消息）
     * RocketMQ延迟级别：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     */
    private Integer delayLevel;

    /**
     * 消息类型常量
     */
    public static final String TYPE_ORDER_CREATE = "ORDER_CREATE";
    public static final String TYPE_ORDER_PAY_SUCCESS = "ORDER_PAY_SUCCESS";
    public static final String TYPE_ORDER_TIMEOUT = "ORDER_TIMEOUT";
    public static final String TYPE_ORDER_CANCEL = "ORDER_CANCEL";

    /**
     * 创建订单创建消息
     */
    public static OrderMessage createOrderMessage(Long orderId, String orderNo, Long userId) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setOrderNo(orderNo);
        message.setUserId(userId);
        message.setMessageType(TYPE_ORDER_CREATE);
        message.setMessageTime(LocalDateTime.now());
        message.setDelayLevel(16); // 30分钟延迟
        return message;
    }

    /**
     * 创建支付成功消息
     */
    public static OrderMessage paySuccessMessage(Long orderId, String orderNo, Long userId) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setOrderNo(orderNo);
        message.setUserId(userId);
        message.setMessageType(TYPE_ORDER_PAY_SUCCESS);
        message.setMessageTime(LocalDateTime.now());
        return message;
    }

    /**
     * 创建订单超时消息
     */
    public static OrderMessage timeoutMessage(Long orderId, String orderNo, Long userId) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setOrderNo(orderNo);
        message.setUserId(userId);
        message.setMessageType(TYPE_ORDER_TIMEOUT);
        message.setMessageTime(LocalDateTime.now());
        return message;
    }
}