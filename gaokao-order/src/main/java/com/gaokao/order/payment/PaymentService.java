package com.gaokao.order.payment;

import com.gaokao.order.dto.PaymentRequest;
import com.gaokao.order.dto.PaymentResponse;
import com.gaokao.order.dto.PaymentCallback;

/**
 * 支付服务接口（抽象设计）
 *
 * 设计说明：
 * - 统一支付接口，支持多支付渠道扩展
 * - 采用策略模式，不同支付方式实现不同适配器
 * - 支付流程：创建支付 → 等待回调 → 处理回调 → 更新状态
 *
 * 支付渠道适配器：
 * - MockPaymentService：Mock支付（开发测试）
 * - WechatPayAdapter：微信支付适配器（预留扩展）
 * - AlipayAdapter：支付宝支付适配器（预留扩展）
 *
 * 简历价值：
 * - 展示接口抽象设计能力
 * - 展示多渠道支付架构设计思路
 * - 支付回调幂等性处理设计
 */
public interface PaymentService {

    /**
     * 获取支付渠道名称
     */
    String getChannelName();

    /**
     * 获取支付渠道代码
     */
    String getChannelCode();

    /**
     * 创建支付订单
     *
     * @param request 支付请求
     * @return 支付响应（包含支付链接/二维码等）
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 处理支付回调
     *
     * @param callback 回调数据
     * @return 处理结果
     */
    boolean handleCallback(PaymentCallback callback);

    /**
     * 查询支付状态
     *
     * @param orderNo 订单号
     * @return 支付状态
     */
    PaymentResponse queryPaymentStatus(String orderNo);

    /**
     * 关闭支付订单
     *
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean closePayment(String orderNo);

    /**
     * 申请退款
     *
     * @param orderNo 订单号
     * @param refundAmount 退款金额
     * @param reason 退款原因
     * @return 退款结果
     */
    PaymentResponse refund(String orderNo, java.math.BigDecimal refundAmount, String reason);

    /**
     * 验证回调签名（防止伪造回调）
     *
     * @param callback 回调数据
     * @return 签名是否有效
     */
    boolean verifySignature(PaymentCallback callback);

    /**
     * 查询订单支付流水号
     *
     * @param orderNo 订单号
     * @return 支付流水号
     */
    String getPaymentNo(String orderNo);
}