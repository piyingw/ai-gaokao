package com.gaokao.member.entity;

import lombok.Getter;

/**
 * 会员等级枚举
 *
 * 设计三级会员体系：
 * - FREE: 免费用户，基础功能
 * - NORMAL: 普通会员（98元/年），智能推荐
 * - VIP: 高级会员（298元/年），全功能开放
 */
@Getter
public enum MemberLevel {

    /**
     * 免费用户
     * 权益：基础查询、每日AI对话10次
     */
    FREE("FREE", "免费用户", 0, 10),

    /**
     * 普通会员（98元/年）
     * 权益：智能推荐、详细录取数据、每日AI对话50次
     */
    NORMAL("NORMAL", "普通会员", 98, 50),

    /**
     * VIP会员（298元/年）
     * 权益：一键生成志愿、专家答疑、无限AI对话、优先客服
     */
    VIP("VIP", "VIP会员", 298, -1);  // -1表示无限

    private final String code;
    private final String name;
    private final int yearlyPrice;
    private final int dailyAiLimit;  // 每日AI对话次数限制，-1表示无限制

    MemberLevel(String code, String name, int yearlyPrice, int dailyAiLimit) {
        this.code = code;
        this.name = name;
        this.yearlyPrice = yearlyPrice;
        this.dailyAiLimit = dailyAiLimit;
    }

    /**
     * 根据code获取会员等级
     */
    public static MemberLevel fromCode(String code) {
        for (MemberLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return FREE;  // 默认返回免费用户
    }
}