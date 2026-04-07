package com.gaokao.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 *
 * 设计说明：
 * - 每次支付行为（包括支付失败）都记录一条支付记录
 * - 通过payment_no关联第三方支付流水号
 * - 支付回调通过幂等性校验防止重复处理
 */
@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付记录 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联订单 ID
     */
    private Long orderId;

    /**
     * 支付流水号（第三方支付平台的唯一标识）
     */
    private String paymentNo;

    /**
     * 支付方式：WECHAT/ALIPAY/MOCK
     */
    private String paymentMethod;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付状态：INIT/PROCESSING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 支付回调时间
     */
    private LocalDateTime callbackTime;

    /**
     * 支付回调数据（JSON格式，存储第三方回调原始数据）
     */
    private String callbackData;

    /**
     * 错误信息（支付失败时记录）
     */
    private String errorMessage;

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
}