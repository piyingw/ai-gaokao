package com.gaokao.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.member.entity.Member;
import com.gaokao.member.entity.MemberLevel;
import com.gaokao.member.mapper.MemberMapper;
import com.gaokao.member.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 会员服务单元测试
 *
 * 测试场景：
 * 1. 创建免费会员
 * 2. 会员升级
 * 3. 会员过期降级
 * 4. 权益使用次数统计
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberServiceTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private MemberPrivilegeService memberPrivilegeService;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Long testUserId;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testUserId = 1001L;

        testMember = new Member();
        testMember.setId(1L);
        testMember.setUserId(testUserId);
        testMember.setLevel(MemberLevel.FREE.getCode());
        testMember.setStatus(1);
        testMember.setTotalSpent(BigDecimal.ZERO);
        testMember.setCreateTime(LocalDateTime.now());
        testMember.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建免费会员")
    void testCreateFreeMember() {
        // Given
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(memberMapper.insert(any(Member.class))).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        Member result = memberService.createFreeMember(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(MemberLevel.FREE.getCode(), result.getLevel());
        assertEquals(testUserId, result.getUserId());
        assertEquals(1, result.getStatus());

        verify(memberMapper, times(1)).insert(any(Member.class));
    }

    @Test
    @DisplayName("会员升级到VIP")
    void testUpgradeMember() {
        // Given
        testMember.setLevel(MemberLevel.FREE.getCode());
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testMember);
        when(memberMapper.updateById(any(Member.class))).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        Member result = memberService.upgradeMember(testUserId, MemberLevel.VIP, 365);

        // Then
        assertEquals(MemberLevel.VIP.getCode(), result.getLevel());
        assertNotNull(result.getEndTime());
        assertTrue(result.getEndTime().isAfter(LocalDateTime.now()));

        verify(memberMapper, times(1)).updateById(any(Member.class));
    }

    @Test
    @DisplayName("会员过期降级")
    void testDowngradeExpiredMember() {
        // Given
        testMember.setLevel(MemberLevel.VIP.getCode());
        testMember.setEndTime(LocalDateTime.now().minusDays(1));
        testMember.setStatus(1);
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testMember);
        when(memberMapper.updateById(any(Member.class))).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        memberService.downgradeExpiredMember(testUserId);

        // Then
        verify(memberMapper, times(1)).updateById(any(Member.class));
    }

    @Test
    @DisplayName("权益使用次数统计")
    void testUsageCount() {
        // Given
        String usageKey = "gaokao:member:usage:1001:AI_CHAT:2024-01-01";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(usageKey)).thenReturn(5);

        // When
        int count = memberService.getUsageCount(usageKey);

        // Then
        assertEquals(5, count);
    }

    @Test
    @DisplayName("会员等级有效性校验")
    void testMemberLevelOrder() {
        // 验证会员等级顺序
        assertTrue(MemberLevel.VIP.ordinal() > MemberLevel.NORMAL.ordinal());
        assertTrue(MemberLevel.NORMAL.ordinal() > MemberLevel.FREE.ordinal());
    }
}