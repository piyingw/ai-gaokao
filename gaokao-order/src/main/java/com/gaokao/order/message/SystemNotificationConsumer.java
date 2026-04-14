package com.gaokao.order.message;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统通知消息消费者
 *
 * 使用 RocketMQ 4.x 原生 API 实现 MessageListenerConcurrently
 */
@Slf4j
@Component
public class SystemNotificationConsumer implements MessageListenerConcurrently {

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                String json = new String(msg.getBody());
                MessageProducer.SystemNotification notification = JSON.parseObject(json, MessageProducer.SystemNotification.class);

                log.info("消费系统通知：userId={}, title={}", notification.getUserId(), notification.getTitle());

                // TODO: 保存站内消息到数据库
                // TODO: WebSocket实时推送
                // TODO: 短信/邮件通知

                log.info("系统通知处理完成：userId={}, title={}", notification.getUserId(), notification.getTitle());

            } catch (Exception e) {
                log.error("系统通知处理失败", e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}