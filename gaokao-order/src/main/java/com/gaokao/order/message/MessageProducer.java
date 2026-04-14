package com.gaokao.order.message;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 消息生产者
 *
 * 使用 RocketMQ 4.x 原生客户端 API
 */
@Slf4j
@Component
public class MessageProducer {

    @Autowired(required = false)
    private DefaultMQProducer producer;

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
     */
    public void sendOrderMessage(OrderMessage message) {
        log.info("发送订单消息：type={}, orderId={}", message.getMessageType(), message.getOrderId());

        if (producer == null) {
            log.warn("RocketMQ未配置，订单消息仅记录日志：orderId={}", message.getOrderId());
            return;
        }

        try {
            String json = JSON.toJSONString(message);
            Message msg = new Message(TOPIC_ORDER, message.getMessageType(), json.getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(msg);
            log.info("订单消息发送成功：topic={}, msgId={}, orderId={}", TOPIC_ORDER, result.getMsgId(), message.getOrderId());

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
     */
    public void sendDelayMessage(OrderMessage message, int delayLevel) {
        log.info("发送延迟消息：type={}, orderId={}, delayLevel={}",
                message.getMessageType(), message.getOrderId(), delayLevel);

        if (producer == null) {
            log.warn("RocketMQ未配置，延迟消息仅记录日志：orderId={}", message.getOrderId());
            return;
        }

        try {
            String json = JSON.toJSONString(message);
            Message msg = new Message(TOPIC_ORDER, message.getMessageType(), json.getBytes(StandardCharsets.UTF_8));
            msg.setDelayTimeLevel(delayLevel);

            // 使用异步发送，避免阻塞主线程导致接口响应慢
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult result) {
                    log.info("延迟消息发送成功：topic={}, delayLevel={}, msgId={}, orderId={}",
                            TOPIC_ORDER, delayLevel, result.getMsgId(), message.getOrderId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("延迟消息发送失败：orderId={}, error={}", message.getOrderId(), e.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("发送延迟消息失败", e);
        }
    }

    /**
     * 发送会员开通通知
     */
    public void sendMemberNotification(Long userId, String level) {
        log.info("发送会员开通通知：userId={}, level={}", userId, level);

        if (producer == null) {
            log.warn("RocketMQ未配置，会员通知仅记录日志：userId={}", userId);
            return;
        }

        try {
            MemberNotification notification = new MemberNotification();
            notification.setUserId(userId);
            notification.setLevel(level);
            notification.setMessageTime(java.time.LocalDateTime.now());

            String json = JSON.toJSONString(notification);
            Message msg = new Message(TOPIC_MEMBER, json.getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(msg);
            log.info("会员通知发送成功：topic={}, msgId={}, userId={}", TOPIC_MEMBER, result.getMsgId(), userId);

        } catch (Exception e) {
            log.error("发送会员通知失败", e);
        }
    }

    /**
     * 发送系统通知
     */
    public void sendSystemNotification(Long userId, String title, String content) {
        log.info("发送系统通知：userId={}, title={}", userId, title);

        if (producer == null) {
            log.warn("RocketMQ未配置，系统通知仅记录日志：userId={}", userId);
            return;
        }

        try {
            SystemNotification notification = new SystemNotification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setMessageTime(java.time.LocalDateTime.now());

            String json = JSON.toJSONString(notification);
            Message msg = new Message(TOPIC_NOTIFICATION, json.getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(msg);
            log.info("系统通知发送成功：topic={}, msgId={}, userId={}", TOPIC_NOTIFICATION, result.getMsgId(), userId);

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