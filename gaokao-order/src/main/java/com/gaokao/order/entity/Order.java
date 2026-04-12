package com.gaokao.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 *
 * 设计说明：
 * - 支持多种订单类型：会员购买、单次服务（专家咨询等）
 * - 订单状态通过状态机管理，确保状态流转的合法性
 * - 订单超时自动取消通过RocketMQ延迟队列实现
 * - 订单号采用时间戳+随机数的方式生成，保证唯一性
 */
@Data
@TableName("`order`")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单号（唯一，用于对外展示）
     */
    private String orderNo;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 订单类型：MEMBERSHIP（会员购买）/ SERVICE（单次服务）
     */
    private String orderType;

    /**
     * 商品 ID（关联的具体商品，如会员等级ID）
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 实际支付金额（扣除优惠券后）
     */
    private BigDecimal payAmount;

    /**
     * 优惠券 ID（使用的优惠券）
     */
    private Long couponId;

    /**
     * 订单状态
     * PENDING - 待支付
     * PAYING - 支付中
     * PAID - 已支付
     * COMPLETED - 已完成
     * CANCELLED - 已取消
     * REFUNDED - 已退款
     */
    private String status;

    /**
     * 支付方式：WECHAT/ALIPAY/MOCK
     */
    private String paymentMethod;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 订单过期时间（支付超时时间，通常30分钟）
     */
    private LocalDateTime expireTime;

    /**
     * 取消/退款原因
     */
    private String cancelReason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}