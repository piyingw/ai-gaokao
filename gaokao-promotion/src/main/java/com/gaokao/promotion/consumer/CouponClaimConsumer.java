package com.gaokao.promotion.consumer;

import com.alibaba.fastjson2.JSON;
import com.gaokao.promotion.event.CouponClaimEvent;
import com.gaokao.promotion.service.CouponAsyncClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 优惠券领取事件消费者
 *
 * 处理异步优惠券领取请求
 *
 * 流程：
 * 1. 解析领取事件
 * 2. 验证库存凭证
 * 3. 创建用户优惠券
 * 4. 更新领取结果
 *
 * 失败处理：
 * - 消费失败时返回RECONSUME_LATER，触发重试
 * - 重试次数超限后进入死信队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponClaimConsumer implements MessageListenerConcurrently {

    private final CouponAsyncClaimService couponAsyncClaimService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                String json = new String(msg.getBody());
                CouponClaimEvent event = JSON.parseObject(json, CouponClaimEvent.class);

                log.info("消费优惠券领取事件：eventId={}, type={}, userId={}",
                        event.getEventId(), event.getEventType(), event.getUserId());

                // 处理领取请求
                boolean success = couponAsyncClaimService.processClaimRequest(event);

                if (success) {
                    log.info("优惠券领取处理成功：eventId={}", event.getEventId());
                } else {
                    log.warn("优惠券领取处理失败：eventId={}", event.getEventId());
                    // 处理失败，触发重试
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }

            } catch (Exception e) {
                log.error("优惠券领取事件消费失败", e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}