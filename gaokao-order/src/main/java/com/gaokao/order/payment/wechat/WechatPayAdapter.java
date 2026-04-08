package com.gaokao.order.payment.wechat;

import com.gaokao.order.dto.PaymentCallback;
import com.gaokao.order.dto.PaymentRequest;
import com.gaokao.order.dto.PaymentResponse;
import com.gaokao.order.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 微信支付适配器
 *
 * 设计说明：
 * - 预留扩展，当有商户资质时可对接真实微信支付
 * - 实现PaymentService接口，与Mock支付无缝切换
 * - 需要集成微信支付SDK（com.github.wechatpay）
 *
 * 实现要点：
 * 1. 配置商户号、API密钥、证书
 * 2. 创建支付：调用微信支付统一下单API
 * 3. 处理回调：验证签名，解析回调XML/JSON
 * 4. 查询状态：调用微信支付查询API
 * 5. 退款：调用微信支付退款API
 *
 * 简历价值：
 * - 展示支付接口抽象设计能力
 * - 展示适配器模式应用
 * - 支付模块架构设计层面的理解
 */
@Slf4j
public class WechatPayAdapter implements PaymentService {

    // TODO: 当有商户资质时实现真实微信支付对接
    // 需要配置：
    // - appId: 微信AppID
    // - mchId: 商户号
    // - apiV3Key: API V3密钥
    // - certPath: 商户证书路径

    @Override
    public String getChannelName() {
        return "微信支付";
    }

    @Override
    public String getChannelCode() {
        return "WECHAT";
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("微信支付创建订单（预留）：orderNo={}", request.getOrderNo());
        // TODO: 实现微信支付统一下单
        throw new UnsupportedOperationException("微信支付暂未开通，请联系管理员配置商户资质");
    }

    @Override
    public boolean handleCallback(PaymentCallback callback) {
        log.info("微信支付回调处理（预留）：orderNo={}", callback.getOrderNo());
        // TODO: 实现微信支付回调处理
        throw new UnsupportedOperationException("微信支付暂未开通");
    }

    @Override
    public PaymentResponse queryPaymentStatus(String orderNo) {
        log.info("微信支付查询状态（预留）：orderNo={}", orderNo);
        // TODO: 实现微信支付状态查询
        throw new UnsupportedOperationException("微信支付暂未开通");
    }

    @Override
    public boolean closePayment(String orderNo) {
        log.info("微信支付关闭订单（预留）：orderNo={}", orderNo);
        // TODO: 实现微信支付订单关闭
        throw new UnsupportedOperationException("微信支付暂未开通");
    }

    @Override
    public PaymentResponse refund(String orderNo, BigDecimal refundAmount, String reason) {
        log.info("微信支付退款（预留）：orderNo={}", orderNo);
        // TODO: 实现微信支付退款
        throw new UnsupportedOperationException("微信支付暂未开通");
    }

    @Override
    public boolean verifySignature(PaymentCallback callback) {
        // TODO: 实现微信支付签名验证
        // 使用微信支付SDK验证签名
        return false;
    }

    @Override
    public String getPaymentNo(String orderNo) {
        // TODO: 实现微信支付流水号查询
        return null;
    }
}