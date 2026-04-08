package com.gaokao.member.service;

import com.gaokao.member.entity.Member;
import com.gaokao.member.entity.MemberLevel;

import java.time.LocalDateTime;

/**
 * 会员服务接口
 */
public interface MemberService {

    /**
     * 根据用户ID获取会员信息（优先从缓存获取）
     *
     * @param userId 用户ID
     * @return 会员信息，不存在返回null
     */
    Member getMemberByUserId(Long userId);

    /**
     * 创建免费会员（新用户默认为免费会员）
     *
     * @param userId 用户ID
     * @return 会员信息
     */
    Member createFreeMember(Long userId);

    /**
     * 开通/升级会员
     *
     * @param userId 用户ID
     * @param level 目标会员等级
     * @param durationDays 有效天数
     * @return 会员信息
     */
    Member upgradeMember(Long userId, MemberLevel level, int durationDays);

    /**
     * 会员过期自动降级
     *
     * @param userId 用户ID
     */
    void downgradeExpiredMember(Long userId);

    /**
     * 检查会员是否有效
     *
     * @param userId 用户ID
     * @return true-有效 false-无效或已过期
     */
    boolean isMemberValid(Long userId);

    /**
     * 获取权益使用次数（Redis计数器）
     *
     * @param usageKey 使用次数缓存Key
     * @return 今日已使用次数
     */
    int getUsageCount(String usageKey);

    /**
     * 增加权益使用次数（Redis原子计数）
     *
     * @param usageKey 使用次数缓存Key
     */
    void incrementUsageCount(String usageKey);

    /**
     * 清除会员缓存
     *
     * @param userId 用户ID
     */
    void clearMemberCache(Long userId);

    /**
     * 处理会员到期定时任务
     * 扫描所有即将过期或已过期的会员，进行降级处理
     */
    void processExpiredMembers();
}