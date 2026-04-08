package com.gaokao.promotion.service;

import com.gaokao.promotion.entity.CouponTemplate;
import com.gaokao.promotion.entity.UserCoupon;
import com.gaokao.promotion.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接口
 */
public interface CouponService {

    /**
     * 领取优惠券
     *
     * 技术亮点：
     * - Redis原子扣减库存，防止超发
     * - Redisson分布式锁防止并发领取
     * - 防重校验，防止重复领取
     *
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 用户优惠券
     */
    UserCoupon claimCoupon(Long userId, Long templateId);

    /**
     * 使用优惠券
     *
     * @param userId 用户ID
     * @param couponCode 优惠券编码
     * @param orderId 订单ID
     * @param orderAmount 订单金额
     * @return 优惠金额
     */
    BigDecimal useCoupon(Long userId, String couponCode, Long orderId, BigDecimal orderAmount);

    /**
     * 获取用户可用优惠券列表
     *
     * @param userId 用户ID
     * @param orderAmount 订单金额（可选，用于筛选满减门槛）
     * @return 可用优惠券列表
     */
    List<UserCouponVO> getAvailableCoupons(Long userId, BigDecimal orderAmount);

    /**
     * 获取用户所有优惠券
     *
     * @param userId 用户ID
     * @return 优惠券列表
     */
    List<UserCouponVO> getUserCoupons(Long userId);

    /**
     * 获取可用优惠券模板列表
     *
     * @return 模板列表
     */
    List<CouponTemplate> getAvailableTemplates();

    /**
     * 计算优惠金额
     *
     * @param couponCode 优惠券编码
     * @param orderAmount 订单金额
     * @return 优惠金额
     */
    BigDecimal calculateDiscount(String couponCode, BigDecimal orderAmount);

    /**
     * 处理过期优惠券
     * 定时任务调用
     */
    void processExpiredCoupons();

    /**
     * 退还优惠券（订单取消时）
     *
     * @param couponCode 优惠券编码
     * @return 是否成功
     */
    boolean returnCoupon(String couponCode);
}