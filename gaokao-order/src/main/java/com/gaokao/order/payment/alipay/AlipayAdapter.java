package com.gaokao.order.payment.alipay;

import com.gaokao.order.dto.PaymentCallback;
import com.gaokao.order.dto.PaymentRequest;
import com.gaokao.order.dto.PaymentResponse;
import com.gaokao.order.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 支付宝支付适配器
 *
 * 设计说明：
 * - 预留扩展，当有商户资质时可对接真实支付宝支付
 * - 实现PaymentService接口，与Mock支付无缝切换
 * - 需要集成支付宝SDK（alipay-sdk-java）
 *
 * 实现要点：
 * 1. 配置应用ID、商户私钥、支付宝公钥
 * 2. 创建支付：调用支付宝当面付/网页支付API
 * 3. 处理回调：验证签名，解析回调数据
 * 4. 查询状态：调用支付宝查询API
 * 5. 退款：调用支付宝退款API
 *
 * 简历价值：
 * - 展示支付接口抽象设计能力
 * - 展示适配器模式应用
 * - 支付模块架构设计层面的理解
 */
@Slf4j
public class AlipayAdapter implements PaymentService {

    // TODO: 当有商户资质时实现真实支付宝支付对接
    // 需要配置：
    // - appId: 支付宝应用ID
    // - privateKey: 应用私钥
    // - alipayPublicKey: 支付宝公钥
    // - gatewayUrl: 支付宝网关地址

    @Override
    public String getChannelName() {
        return "支付宝支付";
    }

    @Override
    public String getChannelCode() {
        return "ALIPAY";
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("支付宝支付创建订单（预留）：orderNo={}", request.getOrderNo());
        // TODO: 实现支付宝支付创建
        throw new UnsupportedOperationException("支付宝支付暂未开通，请联系管理员配置商户资质");
    }

    @Override
    public boolean handleCallback(PaymentCallback callback) {
        log.info("支付宝支付回调处理（预留）：orderNo={}", callback.getOrderNo());
        // TODO: 实现支付宝支付回调处理
        throw new UnsupportedOperationException("支付宝支付暂未开通");
    }

    @Override
    public PaymentResponse queryPaymentStatus(String orderNo) {
        log.info("支付宝支付查询状态（预留）：orderNo={}", orderNo);
        // TODO: 实现支付宝支付状态查询
        throw new UnsupportedOperationException("支付宝支付暂未开通");
    }

    @Override
    public boolean closePayment(String orderNo) {
        log.info("支付宝支付关闭订单（预留）：orderNo={}", orderNo);
        // TODO: 实现支付宝支付订单关闭
        throw new UnsupportedOperationException("支付宝支付暂未开通");
    }

    @Override
    public PaymentResponse refund(String orderNo, BigDecimal refundAmount, String reason) {
        log.info("支付宝支付退款（预留）：orderNo={}", orderNo);
        // TODO: 实现支付宝支付退款
        throw new UnsupportedOperationException("支付宝支付暂未开通");
    }

    @Override
    public boolean verifySignature(PaymentCallback callback) {
        // TODO: 实现支付宝支付签名验证
        // 使用支付宝SDK验证签名
        return false;
    }

    @Override
    public String getPaymentNo(String orderNo) {
        // TODO: 实现支付宝支付流水号查询
        return null;
    }
}