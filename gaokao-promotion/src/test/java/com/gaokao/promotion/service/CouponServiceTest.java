package com.gaokao.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.promotion.entity.CouponTemplate;
import com.gaokao.promotion.entity.CouponType;
import com.gaokao.promotion.entity.UserCoupon;
import com.gaokao.promotion.entity.UserCouponStatus;
import com.gaokao.promotion.mapper.CouponTemplateMapper;
import com.gaokao.promotion.mapper.UserCouponMapper;
import com.gaokao.promotion.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 优惠券服务单元测试
 *
 * 测试场景：
 * 1. 领取优惠券（并发控制）
 * 2. 使用优惠券
 * 3. 计算优惠金额
 * 4. 优惠券过期处理
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Long testUserId;
    private CouponTemplate testTemplate;
    private UserCoupon testUserCoupon;

    @BeforeEach
    void setUp() {
        testUserId = 1001L;

        testTemplate = new CouponTemplate();
        testTemplate.setId(1L);
        testTemplate.setName("满100减20");
        testTemplate.setType(CouponType.FULL_REDUCTION.getCode());
        testTemplate.setValue(new BigDecimal("20.00"));
        testTemplate.setMinAmount(new BigDecimal("100.00"));
        testTemplate.setTotalCount(100);
        testTemplate.setUsedCount(0);
        testTemplate.setLimitPerUser(2);
        testTemplate.setStatus(1);
        testTemplate.setStartTime(LocalDateTime.now().minusDays(1));
        testTemplate.setEndTime(LocalDateTime.now().plusDays(30));

        testUserCoupon = new UserCoupon();
        testUserCoupon.setId(1L);
        testUserCoupon.setUserId(testUserId);
        testUserCoupon.setTemplateId(1L);
        testUserCoupon.setCouponCode("CPN_20240101120000_ABCD1234");
        testUserCoupon.setCouponName("满100减20");
        testUserCoupon.setCouponType(CouponType.FULL_REDUCTION.getCode());
        testUserCoupon.setCouponValue(new BigDecimal("20.00"));
        testUserCoupon.setMinAmount(new BigDecimal("100.00"));
        testUserCoupon.setStatus(UserCouponStatus.UNUSED.getCode());
        testUserCoupon.setStartTime(LocalDateTime.now().minusDays(1));
        testUserCoupon.setEndTime(LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("计算满减券优惠金额")
    void testCalculateDiscount_FullReduction() {
        // Given
        when(userCouponMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUserCoupon);
        BigDecimal orderAmount = new BigDecimal("150.00");

        // When
        BigDecimal discount = couponService.calculateDiscount(testUserCoupon.getCouponCode(), orderAmount);

        // Then
        assertEquals(new BigDecimal("20.00"), discount);
    }

    @Test
    @DisplayName("计算折扣券优惠金额")
    void testCalculateDiscount_Discount() {
        // Given
        testUserCoupon.setCouponType(CouponType.DISCOUNT.getCode());
        testUserCoupon.setCouponValue(new BigDecimal("0.90"));  // 9折
        when(userCouponMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUserCoupon);
        BigDecimal orderAmount = new BigDecimal("100.00");

        // When
        BigDecimal discount = couponService.calculateDiscount(testUserCoupon.getCouponCode(), orderAmount);

        // Then
        assertEquals(new BigDecimal("10.00"), discount);  // 100 * 0.1 = 10
    }

    @Test
    @DisplayName("领取优惠券-成功")
    void testClaimCoupon_Success() throws InterruptedException {
        // Given
        when(couponTemplateMapper.selectById(1L)).thenReturn(testTemplate);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(0L);  // 已领取次数
        when(valueOperations.decrement(anyString())).thenReturn(99L);  // 扣减后库存
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);

        // When
        UserCoupon result = couponService.claimCoupon(testUserId, 1L);

        // Then
        assertNotNull(result);
        assertEquals(UserCouponStatus.UNUSED.getCode(), result.getStatus());
        assertTrue(result.getCouponCode().startsWith("CPN_"));

        verify(userCouponMapper, times(1)).insert(any(UserCoupon.class));
    }

    @Test
    @DisplayName("优惠券类型枚举验证")
    void testCouponTypeEnum() {
        assertEquals("DISCOUNT", CouponType.DISCOUNT.getCode());
        assertEquals("FULL_REDUCTION", CouponType.FULL_REDUCTION.getCode());
        assertEquals(CouponType.DISCOUNT, CouponType.fromCode("DISCOUNT"));
    }

    @Test
    @DisplayName("用户优惠券状态枚举验证")
    void testUserCouponStatusEnum() {
        assertEquals("UNUSED", UserCouponStatus.UNUSED.getCode());
        assertEquals("USED", UserCouponStatus.USED.getCode());
        assertEquals("EXPIRED", UserCouponStatus.EXPIRED.getCode());
    }
}