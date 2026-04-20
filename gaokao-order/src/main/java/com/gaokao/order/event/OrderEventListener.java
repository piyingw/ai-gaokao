package com.gaokao.order.event;

import com.gaokao.order.message.OrderMessage;
import com.gaokao.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件监听器
 *
 * 监听订单相关事件并执行业务逻辑
 * 通过事件机制解耦消息消费者和业务服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderService orderService;

    /**
     * 处理订单超时事件
     */
    @EventListener
    public void onOrderTimeout(OrderEvent.OrderTimeoutEvent event) {
        OrderMessage message = event.getOrderMessage();
        log.info("监听订单超时事件：orderNo={}", message.getOrderNo());
        orderService.cancelOrder(message.getOrderId(), "订单超时自动取消");
    }

    /**
     * 处理订单创建事件
     */
    @EventListener
    public void onOrderCreate(OrderEvent.OrderCreateEvent event) {
        OrderMessage message = event.getOrderMessage();
        log.info("监听订单创建事件：orderNo={}", message.getOrderNo());
        // TODO: 发送订单创建通知
    }

    /**
     * 处理支付成功事件
     */
    @EventListener
    public void onPaySuccess(OrderEvent.PaySuccessEvent event) {
        OrderMessage message = event.getOrderMessage();
        log.info("监听支付成功事件：orderNo={}", message.getOrderNo());
        // TODO: 发送支付成功通知、更新统计数据
    }

    /**
     * 处理订单取消事件
     */
    @EventListener
    public void onOrderCancel(OrderEvent.OrderCancelEvent event) {
        OrderMessage message = event.getOrderMessage();
        log.info("监听订单取消事件：orderNo={}", message.getOrderNo());
        // TODO: 退还优惠券、发送取消通知
    }
}