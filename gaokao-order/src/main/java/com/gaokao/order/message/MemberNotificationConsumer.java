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
 * 会员通知消息消费者
 *
 * 使用 RocketMQ 4.x 原生 API 实现 MessageListenerConcurrently
 */
@Slf4j
@Component
public class MemberNotificationConsumer implements MessageListenerConcurrently {

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                String json = new String(msg.getBody());
                MessageProducer.MemberNotification notification = JSON.parseObject(json, MessageProducer.MemberNotification.class);

                log.info("消费会员通知：userId={}, level={}", notification.getUserId(), notification.getLevel());

                // TODO: 发送短信/邮件/站内消息通知用户会员开通成功

                log.info("会员通知处理完成：userId={}", notification.getUserId());

            } catch (Exception e) {
                log.error("会员通知处理失败", e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}