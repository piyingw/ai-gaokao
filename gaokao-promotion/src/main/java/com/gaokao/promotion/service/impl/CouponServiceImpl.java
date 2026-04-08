package com.gaokao.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.promotion.entity.CouponTemplate;
import com.gaokao.promotion.entity.CouponType;
import com.gaokao.promotion.entity.UserCoupon;
import com.gaokao.promotion.entity.UserCouponStatus;
import com.gaokao.promotion.mapper.CouponTemplateMapper;
import com.gaokao.promotion.mapper.UserCouponMapper;
import com.gaokao.promotion.service.CouponService;
import com.gaokao.promotion.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 优惠券服务实现类
 *
 * 技术亮点：
 * 1. Redis原子操作扣减库存，防止超发
 * 2. Redisson分布式锁防止并发领取
 * 3. 防重校验，防止重复领取
 * 4. 乐观锁保证数据一致性
 *
 * 并发控制设计：
 * - 库存扣减：使用Redis DECR原子操作
 * - 领取防重：使用Redis SETNX记录已领取
 * - 分布式锁：领取时加用户级锁，防止并发领取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 优惠券库存Key前缀
     */
    private static final String COUPON_STOCK_KEY = "gaokao:coupon:stock:";

    /**
     * 用户已领取记录Key前缀
     */
    private static final String COUPON_CLAIMED_KEY = "gaokao:coupon:claimed:";

    /**
     * 分布式锁Key前缀
     */
    private static final String COUPON_LOCK_KEY = "gaokao:coupon:lock:";

    @Override
    @Transactional
    public UserCoupon claimCoupon(Long userId, Long templateId) {
        log.info("领取优惠券：userId={}, templateId={}", userId, templateId);

        // 1. 获取分布式锁（防止并发领取）
        String lockKey = COUPON_LOCK_KEY + userId + ":" + templateId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，最多等待3秒，锁自动释放时间10秒
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ResultCode.OPERATION_FAILED, "领取太频繁，请稍后再试");
            }

            // 2. 查询优惠券模板
            CouponTemplate template = couponTemplateMapper.selectById(templateId);
            if (template == null || template.getStatus() != 1) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "优惠券不存在或已下架");
            }

            // 3. 校验有效期
            LocalDateTime now = LocalDateTime.now();
            if (template.getStartTime().isAfter(now) || template.getEndTime().isBefore(now)) {
                throw new BusinessException(ResultCode.OPERATION_FAILED, "优惠券不在有效期内");
            }

            // 4. 校验用户领取次数（防重）
            String claimedKey = COUPON_CLAIMED_KEY + userId + ":" + templateId;
            Long claimedCount = redisTemplate.opsForValue().increment(claimedKey, 0);
            if (claimedCount == null) claimedCount = 0L;

            if (claimedCount >= template.getLimitPerUser()) {
                throw new BusinessException(ResultCode.OPERATION_FAILED,
                        String.format("每人最多领取%d张", template.getLimitPerUser()));
            }

            // 5. 扣减库存（Redis原子操作）
            String stockKey = COUPON_STOCK_KEY + templateId;
            Long stock = redisTemplate.opsForValue().decrement(stockKey);

            if (stock == null || stock < 0) {
                // 库存不足，回滚
                redisTemplate.opsForValue().increment(stockKey);
                throw new BusinessException(ResultCode.OPERATION_FAILED, "优惠券已领完");
            }

            // 6. 创建用户优惠券
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(userId);
            userCoupon.setTemplateId(templateId);
            userCoupon.setCouponCode(generateCouponCode());
            userCoupon.setCouponName(template.getName());
            userCoupon.setCouponType(template.getType());
            userCoupon.setCouponValue(template.getValue());
            userCoupon.setMinAmount(template.getMinAmount());
            userCoupon.setStatus(UserCouponStatus.UNUSED.getCode());
            userCoupon.setStartTime(template.getStartTime());
            userCoupon.setEndTime(template.getEndTime());

            userCouponMapper.insert(userCoupon);

            // 7. 更新领取记录
            redisTemplate.opsForValue().increment(claimedKey);
            redisTemplate.expire(claimedKey, 1, TimeUnit.DAYS);

            log.info("优惠券领取成功：userId={}, couponCode={}", userId, userCoupon.getCouponCode());
            return userCoupon;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.ERROR, "领取失败，请重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public BigDecimal useCoupon(Long userId, String couponCode, Long orderId, BigDecimal orderAmount) {
        log.info("使用优惠券：userId={}, couponCode={}, orderId={}", userId, couponCode, orderId);

        // 1. 查询优惠券
        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getCouponCode, couponCode)
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getDeleted, 0)
        );

        if (coupon == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "优惠券不存在");
        }

        // 2. 校验状态
        if (!UserCouponStatus.UNUSED.getCode().equals(coupon.getStatus())) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "优惠券已使用或已过期");
        }

        // 3. 校验有效期
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime().isAfter(now) || coupon.getEndTime().isBefore(now)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "优惠券已过期");
        }

        // 4. 校验最低消费金额
        if (coupon.getMinAmount() != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED,
                    String.format("订单金额需满%s元", coupon.getMinAmount()));
        }

        // 5. 计算优惠金额
        BigDecimal discount = calculateDiscount(couponCode, orderAmount);

        // 6. 更新优惠券状态（使用乐观锁）
        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserCoupon::getId, coupon.getId())
                     .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())  // 乐观锁
                     .set(UserCoupon::getStatus, UserCouponStatus.USED.getCode())
                     .set(UserCoupon::getUseTime, now)
                     .set(UserCoupon::getOrderId, orderId);

        int rows = userCouponMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "优惠券使用失败，请重试");
        }

        log.info("优惠券使用成功：couponCode={}, discount={}", couponCode, discount);
        return discount;
    }

    @Override
    public List<UserCouponVO> getAvailableCoupons(Long userId, BigDecimal orderAmount) {
        List<UserCoupon> coupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                        .eq(UserCoupon::getDeleted, 0)
                        .ge(UserCoupon::getEndTime, LocalDateTime.now())
                        .orderByAsc(UserCoupon::getEndTime)
        );

        List<UserCouponVO> voList = new ArrayList<>();
        for (UserCoupon coupon : coupons) {
            UserCouponVO vo = convertToVO(coupon);

            // 判断是否可用
            boolean available = true;
            String reason = null;
            if (coupon.getMinAmount() != null && orderAmount != null) {
                if (orderAmount.compareTo(coupon.getMinAmount()) < 0) {
                    available = false;
                    reason = String.format("订单金额需满%s元", coupon.getMinAmount());
                }
            }
            vo.setAvailable(available);
            vo.setUnavailableReason(reason);

            voList.add(vo);
        }

        return voList;
    }

    @Override
    public List<UserCouponVO> getUserCoupons(Long userId) {
        List<UserCoupon> coupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getDeleted, 0)
                        .orderByDesc(UserCoupon::getCreateTime)
        );

        List<UserCouponVO> voList = new ArrayList<>();
        for (UserCoupon coupon : coupons) {
            voList.add(convertToVO(coupon));
        }

        return voList;
    }

    @Override
    public List<CouponTemplate> getAvailableTemplates() {
        LocalDateTime now = LocalDateTime.now();
        return couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, 1)
                        .le(CouponTemplate::getStartTime, now)
                        .ge(CouponTemplate::getEndTime, now)
                        .orderByAsc(CouponTemplate::getEndTime)
        );
    }

    @Override
    public BigDecimal calculateDiscount(String couponCode, BigDecimal orderAmount) {
        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getCouponCode, couponCode)
                        .eq(UserCoupon::getDeleted, 0)
        );

        if (coupon == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (CouponType.DISCOUNT.getCode().equals(coupon.getCouponType())) {
            // 折扣券：订单金额 * (1 - 折扣率)
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(coupon.getCouponValue()))
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (CouponType.FULL_REDUCTION.getCode().equals(coupon.getCouponType())) {
            // 满减券：直接减免
            discount = coupon.getCouponValue();
        }

        // 优惠金额不能超过订单金额
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    @Override
    @Transactional
    public void processExpiredCoupons() {
        log.info("开始处理过期优惠券...");

        // 更新所有已过期但状态仍为UNUSED的优惠券
        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                     .lt(UserCoupon::getEndTime, LocalDateTime.now())
                     .set(UserCoupon::getStatus, UserCouponStatus.EXPIRED.getCode());

        int rows = userCouponMapper.update(null, updateWrapper);
        log.info("过期优惠券处理完成，共处理{}张", rows);
    }

    @Override
    @Transactional
    public boolean returnCoupon(String couponCode) {
        log.info("退还优惠券：couponCode={}", couponCode);

        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserCoupon::getCouponCode, couponCode)
                     .eq(UserCoupon::getStatus, UserCouponStatus.USED.getCode())
                     .set(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                     .set(UserCoupon::getUseTime, null)
                     .set(UserCoupon::getOrderId, null);

        int rows = userCouponMapper.update(null, updateWrapper);
        return rows > 0;
    }

    /**
     * 生成优惠券编码
     */
    private String generateCouponCode() {
        return "CPN_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 转换为VO
     */
    private UserCouponVO convertToVO(UserCoupon coupon) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(coupon.getId());
        vo.setCouponCode(coupon.getCouponCode());
        vo.setCouponName(coupon.getCouponName());
        vo.setCouponType(coupon.getCouponType());
        vo.setCouponTypeName(CouponType.fromCode(coupon.getCouponType()).getName());
        vo.setCouponValue(coupon.getCouponValue());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setStatus(coupon.getStatus());
        vo.setStatusName(UserCouponStatus.fromCode(coupon.getStatus()).getName());
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());

        // 判断是否可用
        LocalDateTime now = LocalDateTime.now();
        boolean available = UserCouponStatus.UNUSED.getCode().equals(coupon.getStatus())
                && coupon.getStartTime().isBefore(now)
                && coupon.getEndTime().isAfter(now);
        vo.setAvailable(available);

        return vo;
    }
}