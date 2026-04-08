package com.gaokao.order.service;

import com.gaokao.order.dto.PaymentCallback;
import com.gaokao.order.dto.PaymentRequest;
import com.gaokao.order.dto.PaymentResponse;
import com.gaokao.order.entity.Order;
import com.gaokao.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建会员购买订单
     *
     * @param userId 用户ID
     * @param productId 商品ID（会员等级ID）
     * @return 订单信息
     */
    Order createMembershipOrder(Long userId, Long productId);

    /**
     * 创建支付请求（发起支付）
     *
     * @param orderId 订单ID
     * @param paymentMethod 支付方式
     * @return 支付响应
     */
    PaymentResponse initiatePayment(Long orderId, String paymentMethod);

    /**
     * 处理支付回调
     *
     * @param callback 回调数据
     * @return 处理结果
     */
    boolean handlePaymentCallback(PaymentCallback callback);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @param reason 取消原因
     * @return 是否成功
     */
    boolean cancelOrder(Long orderId, String reason);

    /**
     * 申请退款
     *
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 是否成功
     */
    boolean refundOrder(Long orderId, String reason);

    /**
     * 完成订单（业务处理完成）
     *
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean completeOrder(Long orderId);

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    Order getOrderById(Long orderId);

    /**
     * 根据订单号获取订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    Order getOrderByOrderNo(String orderNo);

    /**
     * 获取用户订单列表
     *
     * @param userId 用户ID
     * @param status 订单状态（可选）
     * @return 订单列表
     */
    List<Order> getUserOrders(Long userId, String status);

    /**
     * 处理超时订单
     * 定时任务调用，自动取消超时未支付的订单
     */
    void processTimeoutOrders();

    /**
     * 校验订单状态是否可以变更
     *
     * @param orderId 订单ID
     * @param targetStatus 目标状态
     * @return 是否可以变更
     */
    boolean canChangeStatus(Long orderId, OrderStatus targetStatus);

    /**
     * 生成订单号
     *
     * @return 订单号
     */
    String generateOrderNo();
}