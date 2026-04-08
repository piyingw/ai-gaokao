package com.gaokao.member.vo;

import lombok.Data;

/**
 * 会员商品VO
 */
@Data
public class MemberProductVO {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 现价
     */
    private java.math.BigDecimal price;

    /**
     * 原价
     */
    private java.math.BigDecimal originalPrice;

    /**
     * 有效天数
     */
    private Integer durationDays;

    /**
     * 商品描述
     */
    private String description;
}