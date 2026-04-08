package com.gaokao.promotion.entity;

import lombok.Getter;

/**
 * 优惠券类型枚举
 */
@Getter
public enum CouponType {

    /**
     * 折扣券（如9折券）
     */
    DISCOUNT("DISCOUNT", "折扣券"),

    /**
     * 满减券（如满100减10）
     */
    FULL_REDUCTION("FULL_REDUCTION", "满减券");

    private final String code;
    private final String name;

    CouponType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CouponType fromCode(String code) {
        for (CouponType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return DISCOUNT;
    }
}