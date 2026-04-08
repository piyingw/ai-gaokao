package com.gaokao.order.job;

import com.gaokao.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * 功能：
 * - 扫描超时未支付的订单
 * - 自动取消超时订单
 * - 释放库存/优惠券等资源（可扩展）
 *
 * 调度策略：
 * - 每5分钟执行一次，扫描超时订单
 *
 * 技术亮点：
 * - 使用Spring的@Scheduled注解实现定时任务
 * - 可扩展为RocketMQ延迟队列实现（更精确）
 * - 支持批量处理，避免单次处理过多数据
 *
 * 注意事项：
 * - 定时任务方案存在一定延迟（最多5分钟）
 * - 如需更精确的超时控制，建议使用RocketMQ延迟队列
 * - 订单创建时发送延迟消息到MQ，到期后自动消费处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderService orderService;

    /**
     * 处理超时订单
     * 每5分钟执行一次
     *
     * Cron表达式：0 0/5 * * * ?
     * - 秒：0
     * - 分：每5分钟
     * - 时：每小时
     * - 日：每天
     * - 月：每月
     * - 周：不限制
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void processTimeoutOrders() {
        log.info("========== 开始执行订单超时处理任务 ==========");

        long startTime = System.currentTimeMillis();

        try {
            orderService.processTimeoutOrders();

            long costTime = System.currentTimeMillis() - startTime;
            log.info("========== 订单超时处理任务完成，耗时：{}ms ==========", costTime);

        } catch (Exception e) {
            log.error("订单超时处理任务执行失败", e);
        }
    }

    /**
     * 订单统计任务
     * 每天凌晨1点执行
     *
     * Cron表达式：0 0 1 * * ?
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyOrderStatistics() {
        log.info("========== 开始执行订单统计任务 ==========");

        try {
            // TODO: 实现订单统计逻辑
            // 1. 统计昨日订单数量、金额
            // 2. 统计会员转化率
            // 3. 生成统计报表

            log.info("========== 订单统计任务完成 ==========");

        } catch (Exception e) {
            log.error("订单统计任务执行失败", e);
        }
    }
}