-- =====================================================
-- 优惠券系统数据库表设计
-- =====================================================

-- 优惠券模板表
CREATE TABLE IF NOT EXISTS `coupon_template` (
    `id` BIGINT NOT NULL COMMENT '模板ID',
    `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    `type` VARCHAR(20) NOT NULL COMMENT '优惠券类型：DISCOUNT/FULL_REDUCTION',
    `value` DECIMAL(10,2) NOT NULL COMMENT '优惠券值（折扣率或减免金额）',
    `min_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低消费金额',
    `total_count` INT NOT NULL COMMENT '总发行量',
    `used_count` INT DEFAULT 0 COMMENT '已领取数量',
    `limit_per_user` INT DEFAULT 1 COMMENT '每人限领数量',
    `start_time` DATETIME NOT NULL COMMENT '有效期开始时间',
    `end_time` DATETIME NOT NULL COMMENT '有效期结束时间',
    `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `description` VARCHAR(500) COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id` BIGINT NOT NULL COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `coupon_code` VARCHAR(50) NOT NULL COMMENT '优惠券编码',
    `coupon_name` VARCHAR(100) COMMENT '优惠券名称',
    `coupon_type` VARCHAR(20) COMMENT '优惠券类型',
    `coupon_value` DECIMAL(10,2) COMMENT '优惠券值',
    `min_amount` DECIMAL(10,2) COMMENT '最低消费金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED/USED/EXPIRED',
    `use_time` DATETIME COMMENT '使用时间',
    `order_id` BIGINT COMMENT '关联订单ID',
    `start_time` DATETIME COMMENT '有效期开始时间',
    `end_time` DATETIME COMMENT '有效期结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_code` (`coupon_code`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 初始化优惠券模板数据
INSERT INTO `coupon_template` (`id`, `name`, `type`, `value`, `min_amount`, `total_count`, `limit_per_user`, `start_time`, `end_time`, `status`, `description`) VALUES
(1, '新人专享9折券', 'DISCOUNT', 0.90, 50.00, 1000, 1, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 1, '新用户专享，满50元可用'),
(2, '满100减20', 'FULL_REDUCTION', 20.00, 100.00, 500, 2, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 1, '满100元减20元'),
(3, 'VIP专享8折券', 'DISCOUNT', 0.80, 100.00, 200, 1, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 1, 'VIP专享，满100元8折');