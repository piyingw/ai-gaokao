package com.gaokao.promotion.service;

import com.gaokao.promotion.vo.CouponClaimResultVO;

/**
 * 优惠券异步领取服务接口
 *
 * 用于高并发秒杀场景：
 * 1. 预检阶段：快速校验资格，预扣减库存
 * 2. 异步处理：MQ处理实际领取逻辑
 * 3. 结果查询：用户查询领取结果
 */
public interface CouponAsyncClaimService {

    /**
     * 提交领取请求（异步秒杀）
     *
     * 流程：
     * 1. 快速预检：校验用户资格、优惠券状态
     * 2. 预扣减库存：Redis原子操作扣减，获取库存凭证
     * 3. 发送MQ消息：异步处理实际领取
     * 4. 返回处理状态：用户可通过eventId查询结果
     *
     * @param userId 用户ID
     * @param templateId 优惠券模板ID
     * @return 领取结果（包含eventId和状态）
     */
    CouponClaimResultVO submitClaimRequest(Long userId, Long templateId);

    /**
     * 查询领取结果
     *
     * @param eventId 事件ID
     * @return 领取结果
     */
    CouponClaimResultVO queryClaimResult(String eventId);

    /**
     * 处理领取请求（由MQ消费者调用）
     *
     * @param event 领取事件
     * @return 是否成功
     */
    boolean processClaimRequest(com.gaokao.promotion.event.CouponClaimEvent event);
}