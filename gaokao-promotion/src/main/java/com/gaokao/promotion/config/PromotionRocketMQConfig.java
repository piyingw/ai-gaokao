package com.gaokao.promotion.config;

import com.gaokao.promotion.consumer.CouponClaimConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类 - 促销模块
 *
 * 注意：Producer 由 gaokao-order 模块的 RocketMQConfig 创建，这里只配置 Consumer
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rocketmq.name-server")
@RequiredArgsConstructor
public class PromotionRocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    private final CouponClaimConsumer couponClaimConsumer;

    /**
     * 创建优惠券领取消费者
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "rocketmq.consumer.coupon.enabled", havingValue = "true", matchIfMissing = true)
    public DefaultMQPushConsumer couponClaimPushConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("gaokao-coupon-consumer");
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        try {
            consumer.subscribe("gaokao-coupon", "*");
            consumer.registerMessageListener(couponClaimConsumer);
            consumer.start();
            log.info("Coupon Claim Consumer 启动成功: namesrv={}, group=gaokao-coupon-consumer", nameServer);
        } catch (Exception e) {
            log.error("Coupon Claim Consumer 启动失败", e);
        }

        return consumer;
    }
}