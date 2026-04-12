-- =====================================================
-- 订单支付系统数据库表设计
-- =====================================================

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号（唯一）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_type` VARCHAR(20) NOT NULL COMMENT '订单类型：MEMBERSHIP/SERVICE',
    `product_id` BIGINT COMMENT '商品ID',
    `product_name` VARCHAR(100) COMMENT '商品名称',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `pay_amount` DECIMAL(10,2) COMMENT '实际支付金额',
    `coupon_id` BIGINT COMMENT '使用的优惠券ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
    `payment_method` VARCHAR(20) COMMENT '支付方式：WECHAT/ALIPAY/MOCK',
    `payment_time` DATETIME COMMENT '支付时间',
    `expire_time` DATETIME COMMENT '订单过期时间',
    `cancel_reason` VARCHAR(200) COMMENT '取消/退款原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 支付记录表
CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT NOT NULL COMMENT '支付记录ID',
    `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
    `payment_no` VARCHAR(100) COMMENT '支付流水号（第三方）',
    `payment_method` VARCHAR(20) NOT NULL COMMENT '支付方式',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'INIT' COMMENT '支付状态：INIT/PROCESSING/SUCCESS/FAILED',
    `callback_time` DATETIME COMMENT '支付回调时间',
    `callback_data` TEXT COMMENT '支付回调原始数据（JSON）',
    `error_message` VARCHAR(500) COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_payment_no` (`payment_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- 商品/会员等级定价表
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `type` VARCHAR(20) NOT NULL COMMENT '商品类型：MEMBERSHIP/SERVICE',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    `original_price` DECIMAL(10,2) COMMENT '原价',
    `description` VARCHAR(500) COMMENT '商品描述',
    `duration_days` INT COMMENT '有效天数（会员商品）',
    `status` INT NOT NULL DEFAULT 1 COMMENT '商品状态：0-下架 1-上架',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 初始化会员商品数据
INSERT INTO `product` (`id`, `name`, `type`, `price`, `original_price`, `description`, `duration_days`, `status`) VALUES
(1, '普通会员（年卡）', 'MEMBERSHIP', 98.00, 128.00, '智能推荐、详细录取数据、每日50次AI对话', 365, 1),
(2, 'VIP会员（年卡）', 'MEMBERSHIP', 298.00, 398.00, '一键生成志愿、专家答疑、无限AI对话', 365, 1),
(3, '普通会员（月卡）', 'MEMBERSHIP', 19.00, 28.00, '智能推荐、详细录取数据、每日50次AI对话', 30, 1),
(4, 'VIP会员（月卡）', 'MEMBERSHIP', 49.00, 68.00, '一键生成志愿、专家答疑、无限AI对话', 30, 1);