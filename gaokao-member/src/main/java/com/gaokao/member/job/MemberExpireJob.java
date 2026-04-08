package com.gaokao.member.job;

import com.gaokao.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会员到期处理定时任务
 *
 * 功能：
 * - 扫描已过期但状态仍为正常的会员
 * - 自动降级为免费会员
 * - 发送到期提醒通知（可扩展）
 *
 * 调度策略：
 * - 每小时执行一次，扫描即将过期的会员
 * - 每天凌晨执行一次，处理已过期会员
 *
 * 技术亮点：
 * - 使用Spring的@Scheduled注解实现定时任务
 * - 可扩展为XXL-JOB分布式任务调度
 * - 支持批量处理，避免单次处理过多数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberExpireJob {

    private final MemberService memberService;

    /**
     * 处理过期会员
     * 每小时执行一次
     *
     * Cron表达式：0 0 * * * ?
     * - 秒：0
     * - 分：0
     * - 时：每小时
     * - 日：每天
     * - 月：每月
     * - 周：不限制
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void processExpiredMembers() {
        log.info("========== 开始执行会员过期处理任务 ==========");

        long startTime = System.currentTimeMillis();

        try {
            memberService.processExpiredMembers();

            long costTime = System.currentTimeMillis() - startTime;
            log.info("========== 会员过期处理任务完成，耗时：{}ms ==========", costTime);

        } catch (Exception e) {
            log.error("会员过期处理任务执行失败", e);
        }
    }

    /**
     * 发送会员到期提醒
     * 每天上午9点执行
     *
     * 扫描即将在7天内过期的会员，发送提醒通知
     *
     * Cron表达式：0 0 9 * * ?
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendExpireReminder() {
        log.info("========== 开始执行会员到期提醒任务 ==========");

        try {
            // TODO: 实现会员到期提醒逻辑
            // 1. 查询7天内即将过期的会员
            // 2. 发送短信/邮件/站内消息提醒
            // 3. 可以通过RocketMQ异步发送通知

            log.info("========== 会员到期提醒任务完成 ==========");

        } catch (Exception e) {
            log.error("会员到期提醒任务执行失败", e);
        }
    }
}