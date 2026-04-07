package com.gaokao.order.entity;

import lombok.Getter;

/**
 * 订单类型枚举
 */
@Getter
public enum OrderType {

    /**
     * 会员购买订单
     * 购买会员等级升级
     */
    MEMBERSHIP("MEMBERSHIP", "会员购买", "购买会员服务"),

    /**
     * 单次服务订单
     * 专家咨询等单次付费服务
     */
    SERVICE("SERVICE", "单次服务", "购买单次服务");

    private final String code;
    private final String name;
    private final String description;

    OrderType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static OrderType fromCode(String code) {
        for (OrderType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return MEMBERSHIP;
    }
}