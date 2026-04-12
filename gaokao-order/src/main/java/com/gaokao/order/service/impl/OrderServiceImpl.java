package com.gaokao.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.member.entity.MemberLevel;
import com.gaokao.member.service.MemberService;
import com.gaokao.order.dto.PaymentCallback;
import com.gaokao.order.dto.PaymentRequest;
import com.gaokao.order.dto.PaymentResponse;
import com.gaokao.order.entity.*;
import com.gaokao.order.mapper.OrderMapper;
import com.gaokao.order.mapper.PaymentRecordMapper;
import com.gaokao.order.message.MessageProducer;
import com.gaokao.order.message.OrderMessage;
import com.gaokao.order.payment.PaymentFactory;
import com.gaokao.order.payment.PaymentService;
import com.gaokao.order.service.OrderService;
import com.gaokao.order.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类
 *
 * 技术亮点：
 * 1. 订单状态机控制状态流转合法性
 * 2. 支付回调幂等性处理（Redis防重）
 * 3. 订单创建与会员开通的事务一致性
 * 4. 订单超时自动取消（RocketMQ延迟队列）
 *
 * 幂等性设计：
 * - 支付回调处理前先检查订单状态
 * - 使用Redis记录已处理的回调（防重复回调）
 * - 数据库乐观锁防止并发更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final PaymentFactory paymentFactory;
    private final MemberService memberService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageProducer messageProducer;

    /**
     * 支付回调幂等性Key前缀
     */
    private static final String CALLBACK_IDEMPOTENT_KEY = "gaokao:order:callback:";

    /**
     * 支付回调幂等性过期时间（24小时）
     */
    private static final long CALLBACK_EXPIRE = 24 * 60 * 60;

    /**
     * 订单超时时间（30分钟）
     */
    private static final int ORDER_TIMEOUT_MINUTES = 30;

    @Override
    @Transactional
    public Order createMembershipOrder(Long userId, Long productId) {
        log.info("创建会员订单：userId={}, productId={}", userId, productId);

        // 1. 获取商品信息（这里简化处理，实际需要查询商品表）
        // 商品ID对应会员等级：1=NORMAL年卡, 2=VIP年卡, 3=NORMAL月卡, 4=VIP月卡
        MemberLevel level;
        int durationDays;
        BigDecimal amount;
        String productName;

        if (productId == 1L) {
            level = MemberLevel.NORMAL;
            durationDays = 365;
            amount = new BigDecimal("98.00");
            productName = "普通会员（年卡）";
        } else if (productId == 2L) {
            level = MemberLevel.VIP;
            durationDays = 365;
            amount = new BigDecimal("298.00");
            productName = "VIP会员（年卡）";
        } else if (productId == 3L) {
            level = MemberLevel.NORMAL;
            durationDays = 30;
            amount = new BigDecimal("19.00");
            productName = "普通会员（月卡）";
        } else if (productId == 4L) {
            level = MemberLevel.VIP;
            durationDays = 30;
            amount = new BigDecimal("49.00");
            productName = "VIP会员（月卡）";
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的商品ID");
        }

        // 2. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOrderType(OrderType.MEMBERSHIP.getCode());
        order.setProductId(productId);
        order.setProductName(productName);
        order.setAmount(amount);
        order.setPayAmount(amount);  // 暂不处理优惠券
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_TIMEOUT_MINUTES));

        orderMapper.insert(order);

        // 发送延迟消息，用于订单超时自动取消
        // RocketMQ延迟级别16 = 30分钟
        OrderMessage delayMessage = OrderMessage.createOrderMessage(order.getId(), order.getOrderNo(), userId);
        delayMessage.setMessageType(OrderMessage.TYPE_ORDER_TIMEOUT);
        messageProducer.sendDelayMessage(delayMessage, 16);

        log.info("订单创建成功：orderNo={}, amount={}", order.getOrderNo(), order.getAmount());
        return order;
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long orderId, String paymentMethod) {
        log.info("发起支付：orderId={}, method={}", orderId, paymentMethod);

        // 1. 获取订单
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 2. 校验订单状态
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (!OrderStateMachine.canTransition(currentStatus, OrderStatus.PAYING)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许支付");
        }

        // 3. 校验订单是否超时
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单已超时，请重新下单");
        }

        // 4. 更新订单状态为支付中
        order.setStatus(OrderStatus.PAYING.getCode());
        order.setPaymentMethod(paymentMethod);
        orderMapper.updateById(order);

        // 5. 构建支付请求
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setOrderNo(order.getOrderNo());
        request.setUserId(order.getUserId());
        request.setAmount(order.getPayAmount());
        request.setProductName(order.getProductName());
        request.setDescription("高考志愿填报系统会员购买");
        request.setExpireMinutes(ORDER_TIMEOUT_MINUTES);

        // 6. 调用支付服务
        PaymentService paymentService = paymentFactory.getPaymentService(paymentMethod);
        PaymentResponse response = paymentService.createPayment(request);

        // 7. 创建支付记录
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(orderId);
        record.setPaymentNo(response.getPaymentNo());
        record.setPaymentMethod(paymentMethod);
        record.setAmount(order.getPayAmount());
        record.setStatus("PROCESSING");

        paymentRecordMapper.insert(record);

        log.info("支付发起成功：orderNo={}, paymentNo={}", order.getOrderNo(), response.getPaymentNo());

        // 8. Mock支付直接完成支付（无需等待回调）
        if ("MOCK".equals(paymentMethod)) {
            PaymentCallback mockCallback = new PaymentCallback();
            mockCallback.setOrderNo(order.getOrderNo());
            mockCallback.setPaymentNo(response.getPaymentNo());
            mockCallback.setStatus("SUCCESS");
            mockCallback.setChannel("MOCK");
            mockCallback.setSign("mock_sign");

            handlePaymentCallback(mockCallback);

            // 返回成功状态
            response.setStatus("SUCCESS");
            response.setSuccess(true);
        }

        return response;
    }

    @Override
    @Transactional
    public boolean handlePaymentCallback(PaymentCallback callback) {
        log.info("处理支付回调：orderNo={}, status={}", callback.getOrderNo(), callback.getStatus());

        String orderNo = callback.getOrderNo();

        // 1. 幂等性校验（防止重复处理）
        String idempotentKey = CALLBACK_IDEMPOTENT_KEY + orderNo + ":" + callback.getPaymentNo();
        if (redisTemplate.opsForValue().get(idempotentKey) != null) {
            log.warn("支付回调已处理，忽略重复回调：orderNo={}", orderNo);
            return true;  // 已处理过，返回成功
        }

        // 2. 获取订单
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            log.error("订单不存在：orderNo={}", orderNo);
            return false;
        }

        // 3. 校验订单状态
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (currentStatus == OrderStatus.PAID || currentStatus == OrderStatus.COMPLETED) {
            log.warn("订单已支付完成，忽略回调：orderNo={}, status={}", orderNo, currentStatus);
            return true;
        }

        // 4. 获取支付服务并验证签名
        PaymentService paymentService = paymentFactory.getPaymentService(callback.getChannel());
        if (!paymentService.verifySignature(callback)) {
            log.error("支付回调签名验证失败：orderNo={}", orderNo);
            return false;
        }

        // 5. 处理支付结果
        boolean success = paymentService.handleCallback(callback);

        if (success) {
            // 6. 更新订单状态为已支付
            OrderStatus newStatus = OrderStateMachine.transition(currentStatus, OrderStatus.PAID);
            order.setStatus(newStatus.getCode());
            order.setPaymentTime(LocalDateTime.now());
            orderMapper.updateById(order);

            // 7. 开通会员（事务一致性）
            if (OrderType.MEMBERSHIP.getCode().equals(order.getOrderType())) {
                MemberLevel level = MemberLevel.fromCode(getLevelByProductId(order.getProductId()));
                int durationDays = getDurationByProductId(order.getProductId());
                memberService.upgradeMember(order.getUserId(), level, durationDays);
            }

            // 8. 标记回调已处理（幂等性）
            redisTemplate.opsForValue().set(idempotentKey, "processed", CALLBACK_EXPIRE, TimeUnit.SECONDS);

            log.info("支付成功，会员开通：orderNo={}, userId={}", orderNo, order.getUserId());

            // 9. 发送支付成功消息（异步通知）
            OrderMessage paySuccessMsg = OrderMessage.paySuccessMessage(order.getId(), orderNo, order.getUserId());
            messageProducer.sendOrderMessage(paySuccessMsg);

            // 10. 发送会员开通通知
            if (OrderType.MEMBERSHIP.getCode().equals(order.getOrderType())) {
                MemberLevel level = MemberLevel.fromCode(getLevelByProductId(order.getProductId()));
                messageProducer.sendMemberNotification(order.getUserId(), level.getCode());
            }

            // 11. 完成订单
            completeOrder(order.getId());

        } else {
            // 支付失败，回退状态
            order.setStatus(OrderStatus.PENDING.getCode());
            orderMapper.updateById(order);
            log.warn("支付失败：orderNo={}, error={}", orderNo, callback.getErrorMessage());
        }

        // 10. 更新支付记录
        LambdaUpdateWrapper<PaymentRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PaymentRecord::getOrderId, order.getId())
                     .set(PaymentRecord::getStatus, success ? "SUCCESS" : "FAILED")
                     .set(PaymentRecord::getCallbackTime, LocalDateTime.now())
                     .set(PaymentRecord::getCallbackData, callback.getRawData())
                     .set(success ? null : PaymentRecord::getErrorMessage, callback.getErrorMessage());
        paymentRecordMapper.update(null, updateWrapper);

        return success;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long orderId, String reason) {
        log.info("取消订单：orderId={}, reason={}", orderId, reason);

        Order order = getOrderById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (!OrderStateMachine.canCancel(currentStatus)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许取消");
        }

        // 更新状态
        OrderStatus newStatus = OrderStateMachine.transition(currentStatus, OrderStatus.CANCELLED);
        order.setStatus(newStatus.getCode());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        log.info("订单取消成功：orderId={}", orderId);
        return true;
    }

    @Override
    @Transactional
    public boolean refundOrder(Long orderId, String reason) {
        log.info("申请退款：orderId={}, reason={}", orderId, reason);

        Order order = getOrderById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (!OrderStateMachine.canRefund(currentStatus)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许退款");
        }

        // 获取支付服务并发起退款
        PaymentService paymentService = paymentFactory.getPaymentService(order.getPaymentMethod());
        PaymentResponse response = paymentService.refund(order.getOrderNo(), order.getPayAmount(), reason);

        if (response.isSuccess()) {
            // 更新状态
            OrderStatus newStatus = OrderStateMachine.transition(currentStatus, OrderStatus.REFUNDED);
            order.setStatus(newStatus.getCode());
            order.setCancelReason(reason);
            orderMapper.updateById(order);

            log.info("退款成功：orderId={}", orderId);
            return true;
        } else {
            log.error("退款失败：orderId={}, error={}", orderId, response.getErrorMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean completeOrder(Long orderId) {
        log.info("完成订单：orderId={}", orderId);

        Order order = getOrderById(orderId);
        if (order == null) {
            return false;
        }

        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        OrderStatus newStatus = OrderStateMachine.transition(currentStatus, OrderStatus.COMPLETED);
        order.setStatus(newStatus.getCode());
        orderMapper.updateById(order);

        log.info("订单完成：orderId={}", orderId);
        return true;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getDeleted, 0)
        );
    }

    @Override
    public List<Order> getUserOrders(Long userId, String status) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId)
                   .eq(Order::getDeleted, 0)
                   .orderByDesc(Order::getCreateTime);

        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Order::getStatus, status);
        }

        return orderMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void processTimeoutOrders() {
        log.info("开始处理超时订单...");

        // 查询所有超时未支付的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.PENDING.getCode())
                   .or()
                   .eq(Order::getStatus, OrderStatus.PAYING.getCode())
                   .lt(Order::getExpireTime, LocalDateTime.now());

        List<Order> timeoutOrders = orderMapper.selectList(queryWrapper);

        for (Order order : timeoutOrders) {
            try {
                cancelOrder(order.getId(), "订单超时自动取消");
                log.info("超时订单取消成功：orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                log.error("超时订单取消失败：orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }

        log.info("超时订单处理完成，共处理{}个订单", timeoutOrders.size());
    }

    @Override
    public boolean canChangeStatus(Long orderId, OrderStatus targetStatus) {
        Order order = getOrderById(orderId);
        if (order == null) {
            return false;
        }
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        return OrderStateMachine.canTransition(currentStatus, targetStatus);
    }

    @Override
    public String generateOrderNo() {
        return "ORD_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 根据商品ID获取会员等级
     */
    private String getLevelByProductId(Long productId) {
        if (productId == 1L || productId == 3L) {
            return MemberLevel.NORMAL.getCode();
        } else if (productId == 2L || productId == 4L) {
            return MemberLevel.VIP.getCode();
        }
        return MemberLevel.FREE.getCode();
    }

    /**
     * 根据商品ID获取有效天数
     */
    private int getDurationByProductId(Long productId) {
        if (productId == 1L || productId == 2L) {
            return 365;
        } else if (productId == 3L || productId == 4L) {
            return 30;
        }
        return 0;
    }
}