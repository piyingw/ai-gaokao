package com.gaokao.order.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付策略工厂
 *
 * 设计说明：
 * - 管理不同支付渠道的PaymentService实例
 * - 根据支付渠道代码获取对应的支付服务
 * - 支持动态注册新的支付渠道
 *
 * 使用示例：
 * PaymentService paymentService = paymentFactory.getPaymentService("MOCK");
 * PaymentResponse response = paymentService.createPayment(request);
 *
 * 技术亮点：
 * - 策略工厂模式，支持多渠道切换
 * - Bean动态注入，自动注册Spring管理的PaymentService
 */
@Slf4j
@Component
public class PaymentFactory {

    /**
     * 支付服务映射表
     */
    private final Map<String, PaymentService> paymentServices = new ConcurrentHashMap<>();

    /**
     * 构造函数自动注入所有PaymentService实现
     */
    @Autowired
    public PaymentFactory(Map<String, PaymentService> paymentServiceMap) {
        // 注册所有PaymentService实现
        paymentServiceMap.forEach((beanName, service) -> {
            String channelCode = service.getChannelCode();
            paymentServices.put(channelCode, service);
            log.info("注册支付渠道：{} -> {}", channelCode, beanName);
        });

        // 确保Mock支付始终可用（用于测试）
        if (!paymentServices.containsKey("MOCK")) {
            log.warn("Mock支付服务未注册，测试环境可能无法正常工作");
        }
    }

    /**
     * 根据渠道代码获取支付服务
     *
     * @param channelCode 支付渠道代码（MOCK/WECHAT/ALIPAY）
     * @return 支付服务实例
     * @throws IllegalArgumentException 如果渠道不存在
     */
    public PaymentService getPaymentService(String channelCode) {
        PaymentService service = paymentServices.get(channelCode);
        if (service == null) {
            log.error("未知的支付渠道：{}", channelCode);
            throw new IllegalArgumentException("未知的支付渠道：" + channelCode);
        }
        return service;
    }

    /**
     * 获取默认支付服务（Mock支付）
     *
     * @return Mock支付服务
     */
    public PaymentService getDefaultPaymentService() {
        return getPaymentService("MOCK");
    }

    /**
     * 注册新的支付服务
     *
     * @param channelCode 渠道代码
     * @param paymentService 支付服务
     */
    public void registerPaymentService(String channelCode, PaymentService paymentService) {
        paymentServices.put(channelCode, paymentService);
        log.info("动态注册支付渠道：{}", channelCode);
    }

    /**
     * 获取所有可用的支付渠道
     *
     * @return 支付渠道列表
     */
    public java.util.List<String> getAvailableChannels() {
        return new java.util.ArrayList<>(paymentServices.keySet());
    }

    /**
     * 检查支付渠道是否可用
     *
     * @param channelCode 渠道代码
     * @return 是否可用
     */
    public boolean isChannelAvailable(String channelCode) {
        return paymentServices.containsKey(channelCode);
    }
}