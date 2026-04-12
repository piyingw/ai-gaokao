package com.gaokao.order.message;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 消息生产者
 *
 * 设计说明：
 * - 封装消息发送逻辑
 * - 支持同步发送和异步发送
 * - 支持延迟消息（用于订单超时）
 *
 * 技术选型：
 * - 使用RocketMQ实现异步消息
 * - 延迟消息用于订单超时自动取消
 *
 * 简历价值：
 * - 展示消息队列设计思路
 * - 展示异步消息处理架构
 * - 延迟消息实现精确超时控制
 *
 * 开发环境：
 * - 如果没有RocketMQ配置，消息只记录日志
 */
@Slf4j
@Component
public class MessageProducer {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 订单主题
     */
    private static final String TOPIC_ORDER = "gaokao-order";

    /**
     * 会员主题
     */
    private static final String TOPIC_MEMBER = "gaokao-member";

    /**
     * 通知主题
     */
    private static final String TOPIC_NOTIFICATION = "gaokao-notification";

    /**
     * 发送订单消息
     *
     * @param message 订单消息
     */
    public void sendOrderMessage(OrderMessage message) {
        log.info("发送订单消息：type={}, orderId={}", message.getMessageType(), message.getOrderId());

        if (rocketMQTemplate == null) {
            log.warn("RocketMQ未配置，订单消息仅记录日志：orderId={}", message.getOrderId());
            return;
        }

        try {
            rocketMQTemplate.convertAndSend(TOPIC_ORDER, message);
            log.info("订单消息发送成功：topic={}, orderId={}", TOPIC_ORDER, message.getOrderId());

        } catch (Exception e) {
            log.error("发送订单消息失败", e);
        }
    }

    /**
     * 发送延迟消息（订单超时处理）
     *
     * RocketMQ延迟级别：
     * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     * 级别16对应30分钟
     *
     * @param message 订单消息
     * @param delayLevel 延迟级别
     */
    public void sendDelayMessage(OrderMessage message, int delayLevel) {
        log.info("发送延迟消息：type={}, orderId={}, delayLevel={}",
                message.getMessageType(), message.getOrderId(), delayLevel);

        if (rocketMQTemplate == null) {
            log.warn("RocketMQ未配置，延迟消息仅记录日志：orderId={}", message.getOrderId());
            return;
        }

        try {
            org.springframework.messaging.Message<OrderMessage> msg =
                    MessageBuilder.withPayload(message).build();
            rocketMQTemplate.syncSend(TOPIC_ORDER, msg, 3000, delayLevel);
            log.info("延迟消息发送成功：topic={}, delayLevel={}, orderId={}", TOPIC_ORDER, delayLevel, message.getOrderId());

        } catch (Exception e) {
            log.error("发送延迟消息失败", e);
        }
    }

    /**
     * 发送会员开通通知
     *
     * @param userId 用户ID
     * @param level 会员等级
     */
    public void sendMemberNotification(Long userId, String level) {
        log.info("发送会员开通通知：userId={}, level={}", userId, level);

        if (rocketMQTemplate == null) {
            log.warn("RocketMQ未配置，会员通知仅记录日志：userId={}", userId);
            return;
        }

        try {
            MemberNotification notification = new MemberNotification();
            notification.setUserId(userId);
            notification.setLevel(level);
            notification.setMessageTime(java.time.LocalDateTime.now());

            rocketMQTemplate.convertAndSend(TOPIC_MEMBER, notification);
            log.info("会员通知发送成功：topic={}, userId={}", TOPIC_MEMBER, userId);

        } catch (Exception e) {
            log.error("发送会员通知失败", e);
        }
    }

    /**
     * 发送系统通知
     *
     * @param userId 用户ID
     * @param title 标题
     * @param content 内容
     */
    public void sendSystemNotification(Long userId, String title, String content) {
        log.info("发送系统通知：userId={}, title={}", userId, title);

        if (rocketMQTemplate == null) {
            log.warn("RocketMQ未配置，系统通知仅记录日志：userId={}", userId);
            return;
        }

        try {
            SystemNotification notification = new SystemNotification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setMessageTime(java.time.LocalDateTime.now());

            rocketMQTemplate.convertAndSend(TOPIC_NOTIFICATION, notification);
            log.info("系统通知发送成功：topic={}, userId={}", TOPIC_NOTIFICATION, userId);

        } catch (Exception e) {
            log.error("发送系统通知失败", e);
        }
    }

    /**
     * 会员通知消息
     */
    @lombok.Data
    public static class MemberNotification {
        private Long userId;
        private String level;
        private java.time.LocalDateTime messageTime;
    }

    /**
     * 系统通知消息
     */
    @lombok.Data
    public static class SystemNotification {
        private Long userId;
        private String title;
        private String content;
        private java.time.LocalDateTime messageTime;
    }
}