package com.gaokao.order.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 会员通知消息消费者
 *
 * 处理会员开通、升级等通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "gaokao-member",
        consumerGroup = "gaokao-member-consumer"
)
public class MemberNotificationConsumer implements RocketMQListener<MessageProducer.MemberNotification> {

    @Override
    public void onMessage(MessageProducer.MemberNotification notification) {
        log.info("消费会员通知：userId={}, level={}", notification.getUserId(), notification.getLevel());

        try {
            // TODO: 发送短信/邮件/站内消息通知用户会员开通成功
            // 可以集成短信服务、邮件服务、WebSocket推送等

            log.info("会员通知处理完成：userId={}", notification.getUserId());

        } catch (Exception e) {
            log.error("会员通知处理失败", e);
            throw new RuntimeException("会员通知处理失败", e);
        }
    }
}