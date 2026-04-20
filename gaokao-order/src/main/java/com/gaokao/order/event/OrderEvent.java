package com.gaokao.order.event;

import com.gaokao.order.message.OrderMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 订单事件
 *
 * 用于在消息消费者和业务处理器之间解耦
 */
@Getter
public class OrderEvent extends ApplicationEvent {

    private final OrderMessage orderMessage;

    public OrderEvent(Object source, OrderMessage orderMessage) {
        super(source);
        this.orderMessage = orderMessage;
    }

    /**
     * 订单超时事件
     */
    public static class OrderTimeoutEvent extends OrderEvent {
        public OrderTimeoutEvent(Object source, OrderMessage message) {
            super(source, message);
        }
    }

    /**
     * 订单创建事件
     */
    public static class OrderCreateEvent extends OrderEvent {
        public OrderCreateEvent(Object source, OrderMessage message) {
            super(source, message);
        }
    }

    /**
     * 支付成功事件
     */
    public static class PaySuccessEvent extends OrderEvent {
        public PaySuccessEvent(Object source, OrderMessage message) {
            super(source, message);
        }
    }

    /**
     * 订单取消事件
     */
    public static class OrderCancelEvent extends OrderEvent {
        public OrderCancelEvent(Object source, OrderMessage message) {
            super(source, message);
        }
    }
}