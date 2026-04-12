-- 高考志愿填报系统数据库初始化脚本

-- 设置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS gaokao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gaokao;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像',
    gender TINYINT DEFAULT 1 COMMENT '性别 0-女 1-男',
    province VARCHAR(50) COMMENT '省份',
    grade VARCHAR(20) COMMENT '年级',
    subjects VARCHAR(50) COMMENT '选科组合',
    target_score INT COMMENT '目标分数',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 院校表
-- ----------------------------
DROP TABLE IF EXISTS university;
CREATE TABLE university (
    id BIGINT PRIMARY KEY COMMENT '院校ID',
    name VARCHAR(100) NOT NULL COMMENT '院校名称',
    code VARCHAR(20) COMMENT '院校代码',
    province VARCHAR(50) COMMENT '所在省份',
    city VARCHAR(50) COMMENT '所在城市',
    level VARCHAR(20) COMMENT '院校层次 985/211/双一流/普通',
    type VARCHAR(20) COMMENT '院校类型 综合/理工/师范/医药等',
    nature VARCHAR(20) COMMENT '办学性质 公办/民办/中外合作',
    ranking INT COMMENT '综合排名',
    intro TEXT COMMENT '院校简介',
    features TEXT COMMENT '特色专业JSON',
    admission_url VARCHAR(500) COMMENT '招生网地址',
    official_url VARCHAR(500) COMMENT '官网地址',
    tags VARCHAR(500) COMMENT '院校标签JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_province (province),
    INDEX idx_level (level),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院校表';

-- ----------------------------
-- 专业表
-- ----------------------------
DROP TABLE IF EXISTS major;
CREATE TABLE major (
    id BIGINT PRIMARY KEY COMMENT '专业ID',
    name VARCHAR(100) NOT NULL COMMENT '专业名称',
    code VARCHAR(20) COMMENT '专业代码',
    category VARCHAR(50) COMMENT '学科门类',
    sub_category VARCHAR(50) COMMENT '专业类',
    degree_type VARCHAR(50) COMMENT '学位类型',
    duration INT DEFAULT 4 COMMENT '学制(年)',
    intro TEXT COMMENT '专业简介',
    courses TEXT COMMENT '主要课程JSON',
    employment TEXT COMMENT '就业方向JSON',
    employment_rating INT COMMENT '就业前景评分1-5',
    avg_salary INT COMMENT '平均薪资',
    gender_ratio VARCHAR(20) COMMENT '男女比例',
    subject_requirement TEXT COMMENT '选科要求JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_category (category),
    INDEX idx_sub_category (sub_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

-- ----------------------------
-- 历年分数线表
-- ----------------------------
DROP TABLE IF EXISTS admission_score;
CREATE TABLE admission_score (
    id BIGINT PRIMARY KEY,
    university_id BIGINT NOT NULL COMMENT '院校ID',
    major_id BIGINT COMMENT '专业ID',
    province VARCHAR(50) NOT NULL COMMENT '招生省份',
    year INT NOT NULL COMMENT '年份',
    batch VARCHAR(20) COMMENT '批次',
    subject_type VARCHAR(20) COMMENT '科类 物理类/历史类/理科/文科',
    min_score INT COMMENT '最低分',
    avg_score DECIMAL(5,2) COMMENT '平均分',
    max_score INT COMMENT '最高分',
    min_rank INT COMMENT '最低位次',
    max_rank INT COMMENT '最高位次',
    enrollment INT COMMENT '招生人数',
    admitted INT COMMENT '录取人数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_university (university_id),
    INDEX idx_province_year (province, year),
    INDEX idx_score (min_score),
    INDEX idx_university_province (university_id, province),
    INDEX idx_province_subject (province, subject_type),
    INDEX idx_year (year),
    INDEX idx_min_rank (min_rank),
    INDEX idx_composite (province, subject_type, year, min_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历年分数线表';

-- ----------------------------
-- 用户志愿表
-- ----------------------------
DROP TABLE IF EXISTS user_application;
CREATE TABLE user_application (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) COMMENT '志愿方案名称',
    score INT COMMENT '高考分数',
    province VARCHAR(50) COMMENT '省份',
    subject_type VARCHAR(20) COMMENT '科类',
    applications TEXT COMMENT '志愿列表JSON',
    status TINYINT DEFAULT 0 COMMENT '状态 0-草稿 1-已提交',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户志愿表';

-- ----------------------------
-- 政策文档表
-- ----------------------------
DROP TABLE IF EXISTS policy_document;
CREATE TABLE policy_document (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    type VARCHAR(50) COMMENT '文档类型',
    province VARCHAR(50) COMMENT '适用省份',
    year INT COMMENT '年份',
    content TEXT COMMENT '文档内容',
    summary TEXT COMMENT '摘要',
    keywords TEXT COMMENT '关键词JSON',
    source VARCHAR(200) COMMENT '来源',
    source_url VARCHAR(500) COMMENT '来源URL',
    publish_time DATETIME COMMENT '发布时间',
    vector_id VARCHAR(100) COMMENT '向量ID',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_province_year (province, year),
    INDEX idx_type (type),
    FULLTEXT INDEX ft_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策文档表';

-- ----------------------------
-- 插入测试数据
-- ----------------------------

-- 插入测试用户
INSERT INTO sys_user (id, username, password, phone, nickname, province, grade, subjects, target_score, status) VALUES
(1, 'test', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', '测试用户', '北京', '高三', '物理+化学+生物', 600, 1);

-- 插入测试院校
INSERT INTO university (id, name, code, province, city, level, type, nature, ranking, intro) VALUES
(1, '清华大学', '10003', '北京', '北京', '985', '综合', '公办', 1, '清华大学是中国著名高等学府，是中国高层次人才培养和科学技术研究的重要基地之一。'),
(2, '北京大学', '10001', '北京', '北京', '985', '综合', '公办', 2, '北京大学创办于1898年，是中国近代第一所国立综合性大学。'),
(3, '复旦大学', '10246', '上海', '上海', '985', '综合', '公办', 3, '复旦大学创建于1905年，原名复旦公学，是中国人自主创办的第一所高等院校。'),
(4, '上海交通大学', '10248', '上海', '上海', '985', '综合', '公办', 4, '上海交通大学是我国历史最悠久、享誉海内外的高等学府之一。'),
(5, '浙江大学', '10335', '浙江', '杭州', '985', '综合', '公办', 5, '浙江大学是一所历史悠久、声誉卓著的高等学府，坐落于中国历史文化名城杭州。');

-- 插入测试专业
INSERT INTO major (id, name, code, category, sub_category, degree_type, duration, intro, employment_rating, avg_salary) VALUES
(1, '计算机科学与技术', '080901', '工学', '计算机类', '工学学士', 4, '计算机科学与技术是研究计算机系统结构、软件与理论、应用技术的学科。', 5, 15000),
(2, '软件工程', '080902', '工学', '计算机类', '工学学士', 4, '软件工程是研究软件开发、维护和管理的工程学科。', 5, 14000),
(3, '人工智能', '080717T', '工学', '电子信息类', '工学学士', 4, '人工智能是研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统的一门新的技术科学。', 5, 18000),
(4, '电子信息工程', '080701', '工学', '电子信息类', '工学学士', 4, '电子信息工程是一门应用计算机等现代化技术进行电子信息控制和信息处理的学科。', 4, 12000),
(5, '临床医学', '100201K', '医学', '临床医学类', '医学学士', 5, '临床医学是研究疾病的病因、诊断、治疗和预后，提高临床治疗水平，促进人体健康的科学。', 5, 15000);

-- 插入测试分数线数据
INSERT INTO admission_score (id, university_id, province, year, batch, subject_type, min_score, avg_score, max_score, min_rank, enrollment) VALUES
(1, 1, '北京', 2023, '本科批', '物理类', 680, 690.5, 710, 100, 50),
(2, 2, '北京', 2023, '本科批', '物理类', 675, 685.0, 705, 150, 60),
(3, 3, '北京', 2023, '本科批', '物理类', 660, 670.0, 690, 300, 40),
(4, 4, '北京', 2023, '本科批', '物理类', 655, 665.0, 685, 400, 45),
(5, 5, '北京', 2023, '本科批', '物理类', 650, 660.0, 680, 500, 55);

-- ----------------------------
-- 会员表
-- ----------------------------
DROP TABLE IF EXISTS member;
CREATE TABLE member (
    id BIGINT PRIMARY KEY COMMENT '会员ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    level VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '会员等级：FREE/NORMAL/VIP',
    start_time DATETIME COMMENT '会员开始时间',
    end_time DATETIME COMMENT '会员结束时间',
    status INT NOT NULL DEFAULT 1 COMMENT '会员状态：0-已过期 1-正常 2-冻结',
    total_spent DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计消费金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_level (level),
    INDEX idx_status (status),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- ----------------------------
-- 会员权益配置表
-- ----------------------------
DROP TABLE IF EXISTS member_privilege;
CREATE TABLE member_privilege (
    id BIGINT PRIMARY KEY COMMENT '权益配置ID',
    level VARCHAR(20) NOT NULL COMMENT '会员等级：FREE/NORMAL/VIP',
    privilege_code VARCHAR(50) NOT NULL COMMENT '权益代码',
    privilege_name VARCHAR(100) NOT NULL COMMENT '权益名称',
    limit_count INT NOT NULL DEFAULT -1 COMMENT '每日使用次数限制，-1表示无限制',
    description VARCHAR(500) COMMENT '权益描述',
    status INT NOT NULL DEFAULT 1 COMMENT '权益状态：0-禁用 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_level_privilege (level, privilege_code),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员权益配置表';

-- 初始化会员权益配置数据
INSERT INTO member_privilege (id, level, privilege_code, privilege_name, limit_count, description, status) VALUES
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

-- ----------------------------
-- 会员每日使用次数记录表
-- ----------------------------
DROP TABLE IF EXISTS member_usage;
CREATE TABLE member_usage (
    id BIGINT PRIMARY KEY COMMENT '使用记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    privilege_code VARCHAR(50) NOT NULL COMMENT '权益代码',
    usage_date DATE NOT NULL COMMENT '使用日期',
    usage_count INT NOT NULL DEFAULT 0 COMMENT '当日使用次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_privilege_date (user_id, privilege_code, usage_date),
    INDEX idx_user_id (user_id),
    INDEX idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员每日使用次数记录表';

-- 为测试用户创建免费会员记录
INSERT INTO member (id, user_id, level, status, total_spent) VALUES
(1, 1, 'FREE', 1, 0.00);