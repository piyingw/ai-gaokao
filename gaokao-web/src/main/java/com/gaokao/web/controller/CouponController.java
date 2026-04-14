package com.gaokao.web.controller;

import com.gaokao.common.result.Result;
import com.gaokao.promotion.entity.CouponTemplate;
import com.gaokao.promotion.entity.UserCoupon;
import com.gaokao.promotion.service.CouponService;
import com.gaokao.promotion.service.CouponAsyncClaimService;
import com.gaokao.promotion.vo.UserCouponVO;
import com.gaokao.promotion.vo.CouponClaimResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券控制器
 *
 * API设计：
 * - GET  /api/coupon/templates        - 获取可领取的优惠券列表
 * - POST /api/coupon/claim/{id}       - 领取优惠券（同步）
 * - POST /api/coupon/seckill/{id}     - 秒杀领取优惠券（异步）
 * - GET  /api/coupon/result/{eventId} - 查询秒杀领取结果
 * - GET  /api/coupon/list             - 获取我的优惠券列表
 * - GET  /api/coupon/available        - 获取可用优惠券（下单时）
 * - POST /api/coupon/calculate        - 计算优惠金额
 *
 * 技术亮点：
 * - Redis原子扣减库存
 * - Redisson分布式锁
 * - 防重校验机制
 * - MQ异步秒杀（高并发场景）
 */
@Slf4j
@Tag(name = "优惠券管理", description = "优惠券领取、使用、查询")
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponAsyncClaimService couponAsyncClaimService;

    @Operation(summary = "获取可领取的优惠券列表")
    @GetMapping("/templates")
    public Result<List<CouponTemplate>> getAvailableTemplates() {
        List<CouponTemplate> templates = couponService.getAvailableTemplates();
        return Result.success(templates);
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/claim/{templateId}")
    public Result<UserCouponVO> claimCoupon(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long templateId) {

        log.info("领取优惠券：userId={}, templateId={}", userId, templateId);

        UserCoupon coupon = couponService.claimCoupon(userId, templateId);

        UserCouponVO vo = new UserCouponVO();
        vo.setId(coupon.getId());
        vo.setCouponCode(coupon.getCouponCode());
        vo.setCouponName(coupon.getCouponName());
        vo.setCouponType(coupon.getCouponType());
        vo.setStatus(coupon.getStatus());
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());

        return Result.success(vo);
    }

    @Operation(summary = "获取我的优惠券列表")
    @GetMapping("/list")
    public Result<List<UserCouponVO>> getMyCoupons(@RequestAttribute("userId") Long userId) {
        List<UserCouponVO> coupons = couponService.getUserCoupons(userId);
        return Result.success(coupons);
    }

    @Operation(summary = "获取可用优惠券", description = "根据订单金额筛选可用的优惠券")
    @GetMapping("/available")
    public Result<List<UserCouponVO>> getAvailableCoupons(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "订单金额") @RequestParam(required = false) BigDecimal orderAmount) {

        List<UserCouponVO> coupons = couponService.getAvailableCoupons(userId, orderAmount);
        return Result.success(coupons);
    }

    @Operation(summary = "计算优惠金额")
    @PostMapping("/calculate")
    public Result<BigDecimal> calculateDiscount(
            @Parameter(description = "优惠券编码") @RequestParam String couponCode,
            @Parameter(description = "订单金额") @RequestParam BigDecimal orderAmount) {

        BigDecimal discount = couponService.calculateDiscount(couponCode, orderAmount);
        return Result.success(discount);
    }

    @Operation(summary = "秒杀领取优惠券（异步）", description = "高并发场景使用，先预检资格后异步处理")
    @PostMapping("/seckill/{templateId}")
    public Result<CouponClaimResultVO> seckillClaimCoupon(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long templateId) {

        log.info("秒杀领取优惠券：userId={}, templateId={}", userId, templateId);

        // 提交异步领取请求
        CouponClaimResultVO result = couponAsyncClaimService.submitClaimRequest(userId, templateId);

        return Result.success(result);
    }

    @Operation(summary = "查询秒杀领取结果")
    @GetMapping("/result/{eventId}")
    public Result<CouponClaimResultVO> queryClaimResult(@PathVariable String eventId) {

        log.info("查询秒杀领取结果：eventId={}", eventId);

        CouponClaimResultVO result = couponAsyncClaimService.queryClaimResult(eventId);

        return Result.success(result);
    }
}