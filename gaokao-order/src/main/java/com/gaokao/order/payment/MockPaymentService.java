package com.gaokao.order.payment;

import com.gaokao.order.dto.PaymentCallback;
import com.gaokao.order.dto.PaymentRequest;
import com.gaokao.order.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Mock支付服务实现
 *
 * 设计说明：
 * - 用于开发测试环境，模拟支付流程
 * - 不依赖真实支付平台，便于快速验证业务逻辑
 * - 支持模拟支付成功、失败、超时等场景
 *
 * 模拟流程：
 * 1. 创建支付：生成模拟支付链接和流水号
 * 2. 模拟回调：通过API手动触发回调，模拟支付结果
 * 3. 验证状态：查询模拟的支付状态
 *
 * 简历价值：
 * - 展示支付流程设计思路（架构层面）
 * - Mock实现便于单元测试和集成测试
 * - 真实支付可通过适配器无缝替换
 */
@Slf4j
@Service("mockPaymentService")
public class MockPaymentService implements PaymentService {

    private static final String CHANNEL_NAME = "Mock支付";
    private static final String CHANNEL_CODE = "MOCK";

    /**
     * 模拟支付页面URL（用于测试）
     */
    private static final String MOCK_PAY_URL = "/mock/pay?orderNo=%s&paymentNo=%s";

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    /**
     * 创建模拟支付订单
     *
     * 流程：
     * 1. 生成模拟支付流水号
     * 2. 返回模拟支付页面链接
     * 3. 用户访问链接后可手动选择支付结果
     */
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Mock支付创建订单：orderNo={}, amount={}", request.getOrderNo(), request.getAmount());

        // 生成模拟支付流水号
        String paymentNo = generateMockPaymentNo();

        // 构建模拟支付链接
        String payUrl = String.format(MOCK_PAY_URL, request.getOrderNo(), paymentNo);

        // 返回待支付状态
        PaymentResponse response = PaymentResponse.pending(paymentNo, payUrl);
        response.setAmount(request.getAmount());

        log.info("Mock支付订单创建成功：paymentNo={}, payUrl={}", paymentNo, payUrl);
        return response;
    }

    /**
     * 处理模拟支付回调
     *
     * 设计要点：
     * - 支付回调幂等性处理（通过订单号防重）
     * - 验证回调签名（Mock模式下简化处理）
     * - 更新订单状态
     */
    @Override
    public boolean handleCallback(PaymentCallback callback) {
        log.info("Mock支付回调处理：orderNo={}, status={}", callback.getOrderNo(), callback.getStatus());

        // 验证回调签名（Mock模式下默认通过）
        if (!verifySignature(callback)) {
            log.warn("Mock支付回调签名验证失败：orderNo={}", callback.getOrderNo());
            return false;
        }

        // 检查支付状态
        if ("SUCCESS".equals(callback.getStatus())) {
            log.info("Mock支付成功：orderNo={}, paymentNo={}", callback.getOrderNo(), callback.getPaymentNo());
            return true;
        } else {
            log.warn("Mock支付失败：orderNo={}, error={}", callback.getOrderNo(), callback.getErrorMessage());
            return false;
        }
    }

    /**
     * 查询模拟支付状态
     */
    @Override
    public PaymentResponse queryPaymentStatus(String orderNo) {
        log.info("Mock支付查询状态：orderNo={}", orderNo);

        // Mock模式下返回一个模拟状态
        // 实际使用时可通过Redis或内存存储支付状态
        PaymentResponse response = new PaymentResponse();
        response.setPaymentNo("MOCK_" + orderNo);
        response.setStatus("PENDING");
        response.setSuccess(true);

        return response;
    }

    /**
     * 关闭模拟支付订单
     */
    @Override
    public boolean closePayment(String orderNo) {
        log.info("Mock支付关闭订单：orderNo={}", orderNo);
        return true;
    }

    /**
     * 模拟退款
     */
    @Override
    public PaymentResponse refund(String orderNo, BigDecimal refundAmount, String reason) {
        log.info("Mock支付退款：orderNo={}, amount={}, reason={}", orderNo, refundAmount, reason);

        String refundNo = "REFUND_" + generateMockPaymentNo();
        return PaymentResponse.success(refundNo);
    }

    /**
     * 验证回调签名（Mock模式下默认通过）
     */
    @Override
    public boolean verifySignature(PaymentCallback callback) {
        // Mock模式下不验证签名，直接返回true
        // 实际支付需要验证签名防止伪造回调
        return callback.getSign() != null;
    }

    /**
     * 查询订单支付流水号
     */
    @Override
    public String getPaymentNo(String orderNo) {
        return "MOCK_PAY_" + orderNo;
    }

    /**
     * 生成模拟支付流水号
     */
    private String generateMockPaymentNo() {
        return "MOCK_PAY_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}