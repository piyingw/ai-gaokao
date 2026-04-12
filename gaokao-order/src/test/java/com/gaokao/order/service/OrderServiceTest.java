package com.gaokao.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.member.entity.MemberLevel;
import com.gaokao.member.service.MemberService;
import com.gaokao.order.dto.PaymentCallback;
import com.gaokao.order.dto.PaymentResponse;
import com.gaokao.order.entity.Order;
import com.gaokao.order.entity.OrderStatus;
import com.gaokao.order.mapper.OrderMapper;
import com.gaokao.order.mapper.PaymentRecordMapper;
import com.gaokao.order.payment.PaymentFactory;
import com.gaokao.order.payment.PaymentService;
import com.gaokao.order.service.impl.OrderServiceImpl;
import com.gaokao.order.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 *
 * 测试场景：
 * 1. 创建会员订单
 * 2. 订单状态机流转
 * 3. 支付回调幂等性
 * 4. 订单取消
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private PaymentFactory paymentFactory;

    @Mock
    private MemberService memberService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Long testUserId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUserId = 1001L;

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD_20240101120000_ABCD1234");
        testOrder.setUserId(testUserId);
        testOrder.setOrderType("MEMBERSHIP");
        testOrder.setProductId(1L);
        testOrder.setProductName("普通会员（年卡）");
        testOrder.setAmount(new BigDecimal("98.00"));
        testOrder.setPayAmount(new BigDecimal("98.00"));
        testOrder.setStatus(OrderStatus.PENDING.getCode());
        testOrder.setExpireTime(LocalDateTime.now().plusMinutes(30));
        testOrder.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建会员订单")
    void testCreateMembershipOrder() {
        // Given
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        // When
        Order result = orderService.createMembershipOrder(testUserId, 1L);

        // Then
        assertNotNull(result);
        assertEquals("MEMBERSHIP", result.getOrderType());
        assertEquals(new BigDecimal("98.00"), result.getAmount());
        assertEquals(OrderStatus.PENDING.getCode(), result.getStatus());
        assertNotNull(result.getOrderNo());
        assertTrue(result.getOrderNo().startsWith("ORD_"));
    }

    @Test
    @DisplayName("订单状态机-PENDING可转到PAYING")
    void testStateMachinePendingToPaying() {
        // When
        boolean canTransition = OrderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.PAYING);

        // Then
        assertTrue(canTransition);
    }

    @Test
    @DisplayName("订单状态机-PENDING可转到CANCELLED")
    void testStateMachinePendingToCancelled() {
        // When
        boolean canTransition = OrderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.CANCELLED);

        // Then
        assertTrue(canTransition);
    }

    @Test
    @DisplayName("订单状态机-CANCELLED不可转到其他状态")
    void testStateMachineCancelledIsFinal() {
        // When
        boolean canTransitionToPaid = OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.PAID);
        boolean canTransitionToPending = OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.PENDING);

        // Then
        assertFalse(canTransitionToPaid);
        assertFalse(canTransitionToPending);
        assertTrue(OrderStateMachine.isFinalState(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("订单状态机-PAID可转到REFUNDED")
    void testStateMachinePaidToRefunded() {
        // When
        boolean canTransition = OrderStateMachine.canTransition(OrderStatus.PAID, OrderStatus.REFUNDED);

        // Then
        assertTrue(canTransition);
        assertTrue(OrderStateMachine.canRefund(OrderStatus.PAID));
    }

    @Test
    @DisplayName("取消订单")
    void testCancelOrder() {
        // Given
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        // When
        boolean result = orderService.cancelOrder(1L, "用户主动取消");

        // Then
        assertTrue(result);
        verify(orderMapper, times(1)).updateById(any(Order.class));
    }

    @Test
    @DisplayName("订单号生成")
    void testGenerateOrderNo() {
        // When
        String orderNo = orderService.generateOrderNo();

        // Then
        assertNotNull(orderNo);
        assertTrue(orderNo.startsWith("ORD_"));
        assertEquals(24, orderNo.length());  // ORD_ + 14位时间 + _ + 8位随机
    }

    @Test
    @DisplayName("VIP商品价格验证")
    void testVIPProductPrice() {
        // When
        Order vipOrder = orderService.createMembershipOrder(testUserId, 2L);

        // Then
        assertEquals(new BigDecimal("298.00"), vipOrder.getAmount());
        assertEquals("VIP会员（年卡）", vipOrder.getProductName());
    }
}