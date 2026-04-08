package com.gaokao.order.statemachine;

import com.gaokao.order.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单状态机
 *
 * 设计说明：
 * - 定义订单状态流转规则
 * - 使用状态模式实现状态转换
 * - 状态流转前进行合法性校验
 *
 * 状态流转图：
 * PENDING → PAYING（用户发起支付）
 * PAYING → PAID（支付成功）
 * PAID → COMPLETED（业务完成）
 * PENDING → CANCELLED（超时取消或用户取消）
 * PAID → REFUNDED（申请退款）
 *
 * 技术亮点：
 * - 状态机设计，防止非法状态变更
 * - 状态事件驱动，便于扩展
 * - 与RocketMQ结合，异步处理状态变更
 */
@Slf4j
public class OrderStateMachine {

    /**
     * 状态转换表
     * 定义每个状态可以转换到哪些状态
     */
    private static final java.util.Map<OrderStatus, java.util.Set<OrderStatus>> TRANSITION_TABLE = java.util.Map.of(
            OrderStatus.PENDING, java.util.Set.of(OrderStatus.PAYING, OrderStatus.CANCELLED),
            OrderStatus.PAYING, java.util.Set.of(OrderStatus.PAID, OrderStatus.PENDING, OrderStatus.CANCELLED),
            OrderStatus.PAID, java.util.Set.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED),
            OrderStatus.COMPLETED, java.util.Set.of(),  // 终态，不可转换
            OrderStatus.CANCELLED, java.util.Set.of(),  // 终态，不可转换
            OrderStatus.REFUNDED, java.util.Set.of()    // 终态，不可转换
    );

    /**
     * 校验状态转换是否合法
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否可以转换
     */
    public static boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        java.util.Set<OrderStatus> allowedTargets = TRANSITION_TABLE.get(currentStatus);
        if (allowedTargets == null) {
            log.warn("未知的订单状态：{}", currentStatus);
            return false;
        }
        return allowedTargets.contains(targetStatus);
    }

    /**
     * 执行状态转换
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 转换后的状态
     * @throws IllegalStateException 如果状态转换非法
     */
    public static OrderStatus transition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            log.error("非法的订单状态转换：{} → {}", currentStatus, targetStatus);
            throw new IllegalStateException(
                    String.format("非法的订单状态转换：%s → %s", currentStatus.getCode(), targetStatus.getCode()));
        }

        log.info("订单状态转换成功：{} → {}", currentStatus, targetStatus);
        return targetStatus;
    }

    /**
     * 获取状态可以转换的目标状态列表
     *
     * @param currentStatus 当前状态
     * @return 可转换的目标状态列表
     */
    public static java.util.Set<OrderStatus> getNextStates(OrderStatus currentStatus) {
        return TRANSITION_TABLE.getOrDefault(currentStatus, java.util.Set.of());
    }

    /**
     * 判断是否为终态
     *
     * @param status 订单状态
     * @return 是否为终态
     */
    public static boolean isFinalState(OrderStatus status) {
        return status.isFinalState();
    }

    /**
     * 判断是否可以取消
     *
     * @param status 订单状态
     * @return 是否可以取消
     */
    public static boolean canCancel(OrderStatus status) {
        return status.canCancel();
    }

    /**
     * 判断是否可以退款
     *
     * @param status 订单状态
     * @return 是否可以退款
     */
    public static boolean canRefund(OrderStatus status) {
        return status.canRefund();
    }
}