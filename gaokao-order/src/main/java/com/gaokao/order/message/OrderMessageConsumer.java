package com.gaokao.order.message;

import com.alibaba.fastjson2.JSON;
import com.gaokao.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单消息消费者
 *
 * 使用 RocketMQ 4.x 原生 API 实现 MessageListenerConcurrently
 *
 * 架构改进：通过 Spring 事件机制解耦，消费者只负责消息解析和发布事件，
 * 业务逻辑由 OrderEventListener 处理，避免循环依赖
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer implements MessageListenerConcurrently {

    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String MESSAGE_IDEMPOTENT_KEY = "gaokao:mq:consumed:";
    private static final long IDEMPOTENT_EXPIRE = 24 * 60 * 60;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                String json = new String(msg.getBody());
                OrderMessage message = JSON.parseObject(json, OrderMessage.class);

                log.info("消费订单消息：type={}, orderId={}", message.getMessageType(), message.getOrderId());

                // 幂等性校验
                String messageId = buildMessageId(message);
                String idempotentKey = MESSAGE_IDEMPOTENT_KEY + messageId;

                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", IDEMPOTENT_EXPIRE, TimeUnit.SECONDS);
                if (Boolean.FALSE.equals(isNew)) {
                    log.warn("消息已消费，跳过：messageId={}", messageId);
                    continue;
                }

                // 发布事件，由事件监听器处理业务逻辑
                publishEvent(message);

            } catch (Exception e) {
                log.error("订单消息处理失败", e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private String buildMessageId(OrderMessage message) {
        return message.getMessageType() + ":" + message.getOrderId() + ":" + message.getMessageTime();
    }

    /**
     * 根据消息类型发布对应事件
     */
    private void publishEvent(OrderMessage message) {
        switch (message.getMessageType()) {
            case OrderMessage.TYPE_ORDER_CREATE:
                eventPublisher.publishEvent(new OrderEvent.OrderCreateEvent(this, message));
                break;
            case OrderMessage.TYPE_ORDER_PAY_SUCCESS:
                eventPublisher.publishEvent(new OrderEvent.PaySuccessEvent(this, message));
                break;
            case OrderMessage.TYPE_ORDER_TIMEOUT:
                eventPublisher.publishEvent(new OrderEvent.OrderTimeoutEvent(this, message));
                break;
            case OrderMessage.TYPE_ORDER_CANCEL:
                eventPublisher.publishEvent(new OrderEvent.OrderCancelEvent(this, message));
                break;
            default:
                log.warn("未知的消息类型：{}", message.getMessageType());
        }
    }
}