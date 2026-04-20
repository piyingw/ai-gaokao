package com.gaokao.order.config;

import com.gaokao.order.message.MemberNotificationConsumer;
import com.gaokao.order.message.OrderMessageConsumer;
import com.gaokao.order.message.SystemNotificationConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 *
 * 使用 RocketMQ 4.x 原生客户端，替代不兼容 Spring Boot 3 的 starter
 *
 * 架构改进：通过事件机制解耦，不再需要 @Lazy 注解
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rocketmq.name-server")
@RequiredArgsConstructor
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group:gaokao-producer-group}")
    private String producerGroup;

    private final OrderMessageConsumer orderMessageConsumer;
    private final MemberNotificationConsumer memberNotificationConsumer;
    private final SystemNotificationConsumer systemNotificationConsumer;

    /**
     * 创建消息生产者
     */
    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setRetryTimesWhenSendFailed(3);
        producer.setSendMsgTimeout(10000);  // 增加超时时间到10秒，避免首次连接超时

        try {
            producer.start();
            log.info("RocketMQ Producer 启动成功: namesrv={}, group={}", nameServer, producerGroup);
        } catch (Exception e) {
            log.warn("RocketMQ Producer 启动失败，消息发送功能不可用: {}", e.getMessage());
        }

        return producer;
    }

    /**
     * 创建订单消息消费者
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "rocketmq.consumer.order.enabled", havingValue = "true")
    public DefaultMQPushConsumer orderPushConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("gaokao-order-consumer");
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        try {
            consumer.subscribe("gaokao-order", "*");
            consumer.registerMessageListener(orderMessageConsumer);
            consumer.start();
            log.info("RocketMQ Order Consumer 启动成功: namesrv={}, group=gaokao-order-consumer", nameServer);
        } catch (Exception e) {
            log.error("RocketMQ Order Consumer 启动失败", e);
        }

        return consumer;
    }

    /**
     * 创建会员通知消费者
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "rocketmq.consumer.member.enabled", havingValue = "true", matchIfMissing = true)
    public DefaultMQPushConsumer memberPushConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("gaokao-member-consumer");
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        try {
            consumer.subscribe("gaokao-member", "*");
            consumer.registerMessageListener(memberNotificationConsumer);
            consumer.start();
            log.info("RocketMQ Member Consumer 启动成功: namesrv={}, group=gaokao-member-consumer", nameServer);
        } catch (Exception e) {
            log.error("RocketMQ Member Consumer 启动失败", e);
        }

        return consumer;
    }

    /**
     * 创建系统通知消费者
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "rocketmq.consumer.notification.enabled", havingValue = "true", matchIfMissing = true)
    public DefaultMQPushConsumer notificationPushConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("gaokao-notification-consumer");
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        try {
            consumer.subscribe("gaokao-notification", "*");
            consumer.registerMessageListener(systemNotificationConsumer);
            consumer.start();
            log.info("RocketMQ Notification Consumer 启动成功: namesrv={}, group=gaokao-notification-consumer", nameServer);
        } catch (Exception e) {
            log.error("RocketMQ Notification Consumer 启动失败", e);
        }

        return consumer;
    }
}