package com.gaokao.promotion.job;

import com.gaokao.promotion.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券过期处理定时任务
 *
 * 功能：
 * - 处理过期优惠券，更新状态为EXPIRED
 * - 预热优惠券库存到Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireJob {

    private final CouponService couponService;

    /**
     * 处理过期优惠券
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void processExpiredCoupons() {
        log.info("========== 开始执行优惠券过期处理任务 ==========");

        try {
            couponService.processExpiredCoupons();
            log.info("========== 优惠券过期处理任务完成 ==========");

        } catch (Exception e) {
            log.error("优惠券过期处理任务执行失败", e);
        }
    }
}