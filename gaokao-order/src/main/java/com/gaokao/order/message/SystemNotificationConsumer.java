package com.gaokao.order.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 系统通知消息消费者
 *
 * 处理系统通知推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "gaokao-notification",
        consumerGroup = "gaokao-notification-consumer"
)
public class SystemNotificationConsumer implements RocketMQListener<MessageProducer.SystemNotification> {

    @Override
    public void onMessage(MessageProducer.SystemNotification notification) {
        log.info("消费系统通知：userId={}, title={}", notification.getUserId(), notification.getTitle());

        try {
            // TODO: 保存站内消息到数据库
            // TODO: WebSocket实时推送
            // TODO: 短信/邮件通知

            log.info("系统通知处理完成：userId={}, title={}", notification.getUserId(), notification.getTitle());

        } catch (Exception e) {
            log.error("系统通知处理失败", e);
            throw new RuntimeException("系统通知处理失败", e);
        }
    }
}