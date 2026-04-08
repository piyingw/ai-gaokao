package com.gaokao.order.message;

import com.gaokao.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单消息消费者
 *
 * 设计说明：
 * - 消费订单相关消息
 * - 处理订单超时、支付成功等事件
 * - 实现消息幂等性处理
 *
 * 技术亮点：
 * - 使用@RocketMQMessageListener注解实现消息监听
 * - 消费者组保证集群消费
 * - Redis实现消息幂等性
 *
 * 简历价值：
 * - 展示消息消费设计思路
 * - 展示消息幂等性处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "gaokao-order",
        consumerGroup = "gaokao-order-consumer",
        selectorType = SelectorType.TAG,
        selectorExpression = "*"
)
public class OrderMessageConsumer implements RocketMQListener<OrderMessage> {

    private final OrderService orderService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 消息幂等性Key前缀
     */
    private static final String MESSAGE_IDEMPOTENT_KEY = "gaokao:mq:consumed:";

    /**
     * 幂等性过期时间（24小时）
     */
    private static final long IDEMPOTENT_EXPIRE = 24 * 60 * 60;

    @Override
    public void onMessage(OrderMessage message) {
        log.info("消费订单消息：type={}, orderId={}", message.getMessageType(), message.getOrderId());

        // 幂等性校验：使用消息ID防止重复消费
        String messageId = buildMessageId(message);
        String idempotentKey = MESSAGE_IDEMPOTENT_KEY + messageId;

        // 检查是否已消费
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", IDEMPOTENT_EXPIRE, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("消息已消费，跳过：messageId={}", messageId);
            return;
        }

        try {
            switch (message.getMessageType()) {
                case OrderMessage.TYPE_ORDER_CREATE:
                    handleOrderCreate(message);
                    break;
                case OrderMessage.TYPE_ORDER_PAY_SUCCESS:
                    handlePaySuccess(message);
                    break;
                case OrderMessage.TYPE_ORDER_TIMEOUT:
                    handleOrderTimeout(message);
                    break;
                case OrderMessage.TYPE_ORDER_CANCEL:
                    handleOrderCancel(message);
                    break;
                default:
                    log.warn("未知的消息类型：{}", message.getMessageType());
            }
        } catch (Exception e) {
            log.error("订单消息处理失败", e);
            // 处理失败，删除幂等性标记，允许重试
            redisTemplate.delete(idempotentKey);
            throw new RuntimeException("消息处理失败，触发重试", e);
        }
    }

    /**
     * 构建消息唯一ID
     */
    private String buildMessageId(OrderMessage message) {
        return message.getMessageType() + ":" + message.getOrderId() + ":" + message.getMessageTime();
    }

    /**
     * 处理订单创建消息
     * 可以用于发送通知、记录日志等
     */
    private void handleOrderCreate(OrderMessage message) {
        log.info("处理订单创建：orderNo={}", message.getOrderNo());
        // TODO: 发送订单创建通知
    }

    /**
     * 处理支付成功消息
     * 可以用于发送通知、记录统计等
     */
    private void handlePaySuccess(OrderMessage message) {
        log.info("处理支付成功：orderNo={}", message.getOrderNo());
        // TODO: 发送支付成功通知、更新统计数据
    }

    /**
     * 处理订单超时消息
     * 取消超时订单
     */
    private void handleOrderTimeout(OrderMessage message) {
        log.info("处理订单超时：orderNo={}", message.getOrderNo());

        // 取消超时订单
        orderService.cancelOrder(message.getOrderId(), "订单超时自动取消");
    }

    /**
     * 处理订单取消消息
     * 可以用于退还优惠券、发送通知等
     */
    private void handleOrderCancel(OrderMessage message) {
        log.info("处理订单取消：orderNo={}", message.getOrderNo());
        // TODO: 退还优惠券、发送取消通知
    }
}