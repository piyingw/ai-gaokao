package com.gaokao.promotion.entity;

import lombok.Getter;

/**
 * 用户优惠券状态枚举
 */
@Getter
public enum UserCouponStatus {

    /**
     * 未使用
     */
    UNUSED("UNUSED", "未使用"),

    /**
     * 已使用
     */
    USED("USED", "已使用"),

    /**
     * 已过期
     */
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String name;

    UserCouponStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static UserCouponStatus fromCode(String code) {
        for (UserCouponStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return UNUSED;
    }
}