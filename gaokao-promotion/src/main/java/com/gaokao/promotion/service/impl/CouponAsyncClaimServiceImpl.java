package com.gaokao.promotion.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.promotion.entity.CouponTemplate;
import com.gaokao.promotion.entity.UserCoupon;
import com.gaokao.promotion.entity.UserCouponStatus;
import com.gaokao.promotion.event.CouponClaimEvent;
import com.gaokao.promotion.mapper.CouponTemplateMapper;
import com.gaokao.promotion.mapper.UserCouponMapper;
import com.gaokao.promotion.service.CouponAsyncClaimService;
import com.gaokao.promotion.vo.CouponClaimResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 优惠券异步领取服务实现
 *
 * 实现高并发秒杀流程：
 * 1. 预检阶段：Redis原子扣减库存，发放库存凭证
 * 2. 异步阶段：MQ处理实际领取，写入数据库
 * 3. 结果查询：Redis存储处理结果
 *
 * 技术亮点：
 * - Redis预扣减：秒杀资格预检，快速响应
 * - MQ异步处理：削峰填谷，保护数据库
 * - 库存凭证：防止重复领取，保证原子性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponAsyncClaimServiceImpl implements CouponAsyncClaimService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private DefaultMQProducer producer;

    /**
     * 优惠券库存Key前缀
     */
    private static final String COUPON_STOCK_KEY = "gaokao:coupon:stock:";

    /**
     * 用户已领取记录Key前缀（预检阶段）
     */
    private static final String COUPON_PRECLAIMED_KEY = "gaokao:coupon:preclaimed:";

    /**
     * 库存凭证Key前缀
     */
    private static final String STOCK_TOKEN_KEY = "gaokao:coupon:token:";

    /**
     * 领取结果Key前缀
     */
    private static final String CLAIM_RESULT_KEY = "gaokao:coupon:result:";

    /**
     * 领取结果过期时间（24小时）
     */
    private static final long RESULT_EXPIRE = 24 * 60 * 60;

    /**
     * MQ Topic
     */
    private static final String TOPIC_COUPON_CLAIM = "gaokao-coupon";

    @Override
    public CouponClaimResultVO submitClaimRequest(Long userId, Long templateId) {
        log.info("提交优惠券领取请求：userId={}, templateId={}", userId, templateId);

        String eventId = CouponClaimEvent.generateEventId(userId, templateId);

        // 1. 快速预检：校验优惠券模板状态
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || template.getStatus() != 1) {
            return CouponClaimResultVO.failed(eventId, "优惠券不存在或已下架");
        }

        LocalDateTime now = LocalDateTime.now();
        if (template.getStartTime().isAfter(now) || template.getEndTime().isBefore(now)) {
            return CouponClaimResultVO.failed(eventId, "优惠券不在有效期内");
        }

        // 2. 校验用户领取次数（预检阶段）
        String preclaimedKey = COUPON_PRECLAIMED_KEY + userId + ":" + templateId;
        Long claimedCount = redisTemplate.opsForValue().increment(preclaimedKey, 0);
        if (claimedCount == null) claimedCount = 0L;

        if (claimedCount >= template.getLimitPerUser()) {
            return CouponClaimResultVO.failed(eventId,
                    String.format("每人最多领取%d张", template.getLimitPerUser()));
        }

        // 3. Redis原子扣减库存（预扣减）
        String stockKey = COUPON_STOCK_KEY + templateId;
        Long stock = redisTemplate.opsForValue().decrement(stockKey);

        if (stock == null || stock < 0) {
            // 库存不足，回滚
            redisTemplate.opsForValue().increment(stockKey);
            return CouponClaimResultVO.failed(eventId, "优惠券已领完");
        }

        // 4. 生成库存凭证（用于后续核销）
        String stockToken = UUID.randomUUID().toString();
        String tokenKey = STOCK_TOKEN_KEY + stockToken;
        redisTemplate.opsForValue().set(tokenKey, eventId, 300, TimeUnit.SECONDS); // 5分钟有效期

        // 5. 更新预领取记录
        redisTemplate.opsForValue().increment(preclaimedKey);
        redisTemplate.expire(preclaimedKey, 1, TimeUnit.DAYS);

        // 6. 发送MQ消息（异步处理）
        CouponClaimEvent event = CouponClaimEvent.createClaimRequest(userId, templateId, stockToken);
        sendClaimEvent(event);

        // 7. 存储处理中状态
        CouponClaimResultVO result = CouponClaimResultVO.processing(eventId, userId, templateId);
        redisTemplate.opsForValue().set(CLAIM_RESULT_KEY + eventId, JSON.toJSONString(result),
                RESULT_EXPIRE, TimeUnit.SECONDS);

        log.info("优惠券领取请求已提交：eventId={}, stockToken={}", eventId, stockToken);
        return result;
    }

    @Override
    public CouponClaimResultVO queryClaimResult(String eventId) {
        log.info("查询领取结果：eventId={}", eventId);

        String resultKey = CLAIM_RESULT_KEY + eventId;
        String resultJson = (String) redisTemplate.opsForValue().get(resultKey);

        if (resultJson == null) {
            return CouponClaimResultVO.failed(eventId, "领取记录不存在或已过期");
        }

        return JSON.parseObject(resultJson, CouponClaimResultVO.class);
    }

    @Override
    @Transactional
    public boolean processClaimRequest(CouponClaimEvent event) {
        log.info("处理优惠券领取请求：eventId={}, userId={}, templateId={}",
                event.getEventId(), event.getUserId(), event.getTemplateId());

        try {
            // 1. 验证库存凭证
            String tokenKey = STOCK_TOKEN_KEY + event.getStockToken();
            String storedEventId = (String) redisTemplate.opsForValue().get(tokenKey);

            if (storedEventId == null || !storedEventId.equals(event.getEventId())) {
                log.warn("库存凭证无效或已过期：token={}, eventId={}", event.getStockToken(), event.getEventId());
                updateClaimResult(event.getEventId(), "库存凭证无效，请重新领取");
                return false;
            }

            // 2. 查询优惠券模板
            CouponTemplate template = couponTemplateMapper.selectById(event.getTemplateId());
            if (template == null) {
                updateClaimResult(event.getEventId(), "优惠券不存在");
                return false;
            }

            // 3. 创建用户优惠券
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(event.getUserId());
            userCoupon.setTemplateId(event.getTemplateId());
            userCoupon.setCouponCode(generateCouponCode());
            userCoupon.setCouponName(template.getName());
            userCoupon.setCouponType(template.getType());
            userCoupon.setCouponValue(template.getValue());
            userCoupon.setMinAmount(template.getMinAmount());
            userCoupon.setStatus(UserCouponStatus.UNUSED.getCode());
            userCoupon.setStartTime(template.getStartTime());
            userCoupon.setEndTime(template.getEndTime());

            userCouponMapper.insert(userCoupon);

            // 4. 删除库存凭证
            redisTemplate.delete(tokenKey);

            // 5. 更新领取结果为成功
            CouponClaimResultVO result = CouponClaimResultVO.success(
                    event.getEventId(),
                    userCoupon.getCouponCode(),
                    userCoupon.getCouponName()
            );
            result.setUserId(event.getUserId());
            result.setTemplateId(event.getTemplateId());

            String resultKey = CLAIM_RESULT_KEY + event.getEventId();
            redisTemplate.opsForValue().set(resultKey, JSON.toJSONString(result),
                    RESULT_EXPIRE, TimeUnit.SECONDS);

            log.info("优惠券领取成功：eventId={}, couponCode={}", event.getEventId(), userCoupon.getCouponCode());
            return true;

        } catch (Exception e) {
            log.error("处理优惠券领取请求失败：eventId={}", event.getEventId(), e);

            // 更新领取结果为失败
            updateClaimResult(event.getEventId(), "领取处理失败：" + e.getMessage());

            // 回滚库存（将库存凭证转为失败状态，由定时任务回收）
            return false;
        }
    }

    /**
     * 发送领取事件到MQ
     */
    private void sendClaimEvent(CouponClaimEvent event) {
        if (producer == null) {
            log.warn("RocketMQ未配置，直接处理领取请求：eventId={}", event.getEventId());
            processClaimRequest(event);
            return;
        }

        try {
            String json = JSON.toJSONString(event);
            Message msg = new Message(TOPIC_COUPON_CLAIM, event.getEventType(), json.getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(msg);
            log.info("优惠券领取事件发送成功：topic={}, msgId={}, eventId={}",
                    TOPIC_COUPON_CLAIM, result.getMsgId(), event.getEventId());

        } catch (Exception e) {
            log.error("发送优惠券领取事件失败，降级为同步处理：eventId={}", event.getEventId(), e);
            processClaimRequest(event);
        }
    }

    /**
     * 更新领取结果为失败
     */
    private void updateClaimResult(String eventId, String failReason) {
        CouponClaimResultVO result = CouponClaimResultVO.failed(eventId, failReason);
        String resultKey = CLAIM_RESULT_KEY + eventId;
        redisTemplate.opsForValue().set(resultKey, JSON.toJSONString(result),
                RESULT_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 生成优惠券编码
     */
    private String generateCouponCode() {
        return "CPN_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}