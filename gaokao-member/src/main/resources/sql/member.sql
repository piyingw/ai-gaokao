-- =====================================================
-- 会员系统数据库表设计
-- =====================================================

-- 会员表
CREATE TABLE IF NOT EXISTS `member` (
    `id` BIGINT NOT NULL COMMENT '会员ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `level` VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '会员等级：FREE/NORMAL/VIP',
    `start_time` DATETIME COMMENT '会员开始时间',
    `end_time` DATETIME COMMENT '会员结束时间',
    `status` INT NOT NULL DEFAULT 1 COMMENT '会员状态：0-已过期 1-正常 2-冻结',
    `total_spent` DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计消费金额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_level` (`level`),
    KEY `idx_status` (`status`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员表';

-- 会员权益配置表
CREATE TABLE IF NOT EXISTS `member_privilege` (
    `id` BIGINT NOT NULL COMMENT '权益配置ID',
    `level` VARCHAR(20) NOT NULL COMMENT '会员等级：FREE/NORMAL/VIP',
    `privilege_code` VARCHAR(50) NOT NULL COMMENT '权益代码',
    `privilege_name` VARCHAR(100) NOT NULL COMMENT '权益名称',
    `limit_count` INT NOT NULL DEFAULT -1 COMMENT '每日使用次数限制，-1表示无限制',
    `description` VARCHAR(500) COMMENT '权益描述',
    `status` INT NOT NULL DEFAULT 1 COMMENT '权益状态：0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_privilege` (`level`, `privilege_code`),
    KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员权益配置表';

-- 初始化会员权益配置数据
INSERT INTO `member_privilege` (`id`, `level`, `privilege_code`, `privilege_name`, `limit_count`, `description`, `status`) VALUES
-- 免费用户权益
(1, 'FREE', 'AI_CHAT', 'AI智能对话', 10, '每日AI对话次数限制为10次', 1),
(2, 'FREE', 'UNIVERSITY_QUERY', '院校查询', -1, '不限次数查询院校信息', 1),
(3, 'FREE', 'MAJOR_QUERY', '专业查询', -1, '不限次数查询专业信息', 1),
(4, 'FREE', 'BASIC_RECOMMEND', '基础推荐', 3, '每日基础志愿推荐次数限制为3次', 1),
-- 普通会员权益
(5, 'NORMAL', 'AI_CHAT', 'AI智能对话', 50, '每日AI对话次数限制为50次', 1),
(6, 'NORMAL', 'UNIVERSITY_QUERY', '院校查询', -1, '不限次数查询院校信息', 1),
(7, 'NORMAL', 'MAJOR_QUERY', '专业查询', -1, '不限次数查询专业信息', 1),
(8, 'NORMAL', 'SMART_RECOMMEND', '智能推荐', -1, '不限次数使用智能推荐功能', 1),
(9, 'NORMAL', 'DETAILED_DATA', '详细录取数据', -1, '查看详细录取分数线数据', 1),
-- VIP会员权益
(10, 'VIP', 'AI_CHAT', 'AI智能对话', -1, '无限AI对话次数', 1),
(11, 'VIP', 'UNIVERSITY_QUERY', '院校查询', -1, '不限次数查询院校信息', 1),
(12, 'VIP', 'MAJOR_QUERY', '专业查询', -1, '不限次数查询专业信息', 1),
(13, 'VIP', 'SMART_RECOMMEND', '智能推荐', -1, '不限次数使用智能推荐功能', 1),
(14, 'VIP', 'ONE_CLICK_GENERATE', '一键生成志愿', -1, '一键生成完整志愿表', 1),
(15, 'VIP', 'EXPERT_QA', '专家在线答疑', 5, '每月专家答疑次数限制为5次', 1),
(16, 'VIP', 'PRIORITY_SUPPORT', '优先客服支持', -1, '享受优先客服响应', 1);

-- 会员每日使用次数记录表（用于统计每日权益使用情况）
CREATE TABLE IF NOT EXISTS `member_usage` (
    `id` BIGINT NOT NULL COMMENT '使用记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `privilege_code` VARCHAR(50) NOT NULL COMMENT '权益代码',
    `usage_date` DATE NOT NULL COMMENT '使用日期',
    `usage_count` INT NOT NULL DEFAULT 0 COMMENT '当日使用次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_privilege_date` (`user_id`, `privilege_code`, `usage_date`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员每日使用次数记录表';