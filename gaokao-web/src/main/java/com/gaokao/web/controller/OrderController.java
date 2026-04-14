package com.gaokao.web.controller;

import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.Result;
import com.gaokao.common.result.ResultCode;
import com.gaokao.order.dto.CreateOrderDTO;
import com.gaokao.order.dto.InitiatePaymentDTO;
import com.gaokao.order.dto.PaymentCallback;
import com.gaokao.order.dto.PaymentResponse;
import com.gaokao.order.entity.Order;
import com.gaokao.order.entity.OrderStatus;
import com.gaokao.order.service.OrderService;
import com.gaokao.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单控制器
 *
 * API设计：
 * - POST /api/order/create        - 创建订单
 * - POST /api/order/pay           - 发起支付
 * - POST /api/order/callback      - 支付回调（第三方支付调用）
 * - POST /api/order/cancel        - 取消订单
 * - GET  /api/order/list          - 获取订单列表
 * - GET  /api/order/detail/{id}   - 获取订单详情
 *
 * 技术亮点：
 * - 订单状态机控制状态流转
 * - 支付回调幂等性处理
 * - 订单超时自动取消
 */
@Slf4j
@Tag(name = "订单管理", description = "订单创建、支付、取消、查询")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建会员订单")
    @PostMapping("/create")
    public Result<OrderVO> createOrder(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateOrderDTO dto) {

        log.info("创建订单：userId={}, productId={}", userId, dto.getProductId());

        // 创建会员购买订单
        Order order = orderService.createMembershipOrder(userId, dto.getProductId());

        log.info("订单创建完成：数据库返回ID={}, orderNo={}", order.getId(), order.getOrderNo());

        OrderVO vo = convertToVO(order);
        log.info("返回前端VO.id={}", vo.getId());
        return Result.success(vo);
    }

    @Operation(summary = "发起支付")
    @PostMapping("/pay")
    public Result<OrderVO> initiatePayment(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody InitiatePaymentDTO dto) {

        log.info("发起支付：userId={}, orderId={}, method={}",
                userId, dto.getOrderId(), dto.getPaymentMethod());

        // 获取订单ID
        Long orderId = dto.getOrderId();
        if (orderId == null && dto.getOrderNo() != null) {
            Order order = orderService.getOrderByOrderNo(dto.getOrderNo());
            if (order == null) {
                throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
            }
            orderId = order.getId();
        }

        // 验证订单归属
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        // 发起支付
        PaymentResponse response = orderService.initiatePayment(orderId, dto.getPaymentMethod());

        // 获取更新后的订单
        Order updatedOrder = orderService.getOrderById(orderId);
        OrderVO vo = convertToVO(updatedOrder);
        vo.setPayUrl(response.getPayUrl());  // 返回支付链接

        return Result.success(vo);
    }

    @Operation(summary = "支付回调", description = "第三方支付平台的回调接口")
    @PostMapping("/callback")
    public Result<String> handleCallback(@RequestBody PaymentCallback callback) {

        log.info("支付回调：orderNo={}, status={}", callback.getOrderNo(), callback.getStatus());

        boolean success = orderService.handlePaymentCallback(callback);

        if (success) {
            return Result.success("支付成功");
        } else {
            return Result.error("支付失败");
        }
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel/{orderId}")
    public Result<Void> cancelOrder(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long orderId,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {

        log.info("取消订单：userId={}, orderId={}", userId, orderId);

        // 验证订单归属
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        orderService.cancelOrder(orderId, reason != null ? reason : "用户主动取消");

        return Result.success();
    }

    @Operation(summary = "获取订单列表")
    @GetMapping("/list")
    public Result<List<OrderVO>> getOrderList(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "订单状态") @RequestParam(required = false) String status) {

        List<Order> orders = orderService.getUserOrders(userId, status);

        List<OrderVO> voList = new ArrayList<>();
        for (Order order : orders) {
            voList.add(convertToVO(order));
        }

        return Result.success(voList);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/detail/{orderId}")
    public Result<OrderVO> getOrderDetail(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long orderId) {

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看此订单");
        }

        return Result.success(convertToVO(order));
    }

    @Operation(summary = "查询订单支付状态")
    @GetMapping("/status/{orderNo}")
    public Result<OrderVO> queryOrderStatus(
            @RequestAttribute("userId") Long userId,
            @PathVariable String orderNo) {

        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看此订单");
        }

        return Result.success(convertToVO(order));
    }

    /**
     * 转换为VO
     */
    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setProductName(order.getProductName());
        vo.setAmount(order.getAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusName(OrderStatus.fromCode(order.getStatus()).getName());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setPaymentTime(order.getPaymentTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}