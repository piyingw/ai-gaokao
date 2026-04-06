package com.gaokao.order.entity;

import lombok.Getter;

/**
 * 订单状态枚举
 *
 * 状态流转规则（状态机设计）：
 * PENDING → PAYING（用户发起支付）
 * PAYING → PAID（支付成功）
 * PAID → COMPLETED（业务完成，如会员开通）
 * PENDING → CANCELLED（超时自动取消或用户主动取消）
 * PAID → REFUNDED（退款）
 *
 * 不允许的状态流转：
 * - CANCELLED → 其他状态（已取消不可恢复）
 * - REFUNDED → 其他状态（已退款不可恢复）
 * - COMPLETED → 其他状态（已完成不可变更）
 */
@Getter
public enum OrderStatus {

    /**
     * 待支付
     * 初始状态，用户下单后进入此状态
     * 可流转到：PAYING, CANCELLED
     */
    PENDING("PENDING", "待支付", "订单创建成功，等待用户支付"),

    /**
     * 支付中
     * 用户发起支付后进入此状态
     * 可流转到：PAID, PENDING（支付失败回退）, CANCELLED
     */
    PAYING("PAYING", "支付中", "正在处理支付请求"),

    /**
     * 已支付
     * 支付成功后进入此状态
     * 可流转到：COMPLETED, REFUNDED
     */
    PAID("PAID", "已支付", "支付成功，等待业务处理"),

    /**
     * 已完成
     * 业务处理完成后进入此状态（如会员已开通）
     * 终态，不可流转
     */
    COMPLETED("COMPLETED", "已完成", "订单业务处理完成"),

    /**
     * 已取消
     * 超时自动取消或用户主动取消
     * 终态，不可流转
     */
    CANCELLED("CANCELLED", "已取消", "订单已取消"),

    /**
     * 已退款
     * 用户申请退款并处理完成
     * 终态，不可流转
     */
    REFUNDED("REFUNDED", "已退款", "订单已退款");

    private final String code;
    private final String name;
    private final String description;

    OrderStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据code获取订单状态
     */
    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;  // 默认返回待支付
    }

    /**
     * 判断是否为终态（不可变更）
     */
    public boolean isFinalState() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }

    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == PAYING;
    }

    /**
     * 判断是否可以退款
     */
    public boolean canRefund() {
        return this == PAID;
    }
}