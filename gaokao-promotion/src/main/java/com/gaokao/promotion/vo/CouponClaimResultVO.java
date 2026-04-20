package com.gaokao.promotion.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券异步领取结果VO
 *
 * 用于返回异步领取请求的处理状态
 */
@Data
public class CouponClaimResultVO {

    /**
     * 事件ID（用于查询领取结果）
     */
    private String eventId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券模板ID
     */
    private Long templateId;

    /**
     * 处理状态
     * PROCESSING - 处理中
     * SUCCESS - 领取成功
     * FAILED - 领取失败
     */
    private String status;

    /**
     * 优惠券编码（领取成功后返回）
     */
    private String couponCode;

    /**
     * 优惠券名称
     */
    private String couponName;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 状态常量
     */
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    /**
     * 创建处理中的结果
     */
    public static CouponClaimResultVO processing(String eventId, Long userId, Long templateId) {
        CouponClaimResultVO vo = new CouponClaimResultVO();
        vo.setEventId(eventId);
        vo.setUserId(userId);
        vo.setTemplateId(templateId);
        vo.setStatus(STATUS_PROCESSING);
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    /**
     * 创建成功结果
     */
    public static CouponClaimResultVO success(String eventId, String couponCode, String couponName) {
        CouponClaimResultVO vo = new CouponClaimResultVO();
        vo.setEventId(eventId);
        vo.setStatus(STATUS_SUCCESS);
        vo.setCouponCode(couponCode);
        vo.setCouponName(couponName);
        vo.setCompleteTime(LocalDateTime.now());
        return vo;
    }

    /**
     * 创建失败结果
     */
    public static CouponClaimResultVO failed(String eventId, String failReason) {
        CouponClaimResultVO vo = new CouponClaimResultVO();
        vo.setEventId(eventId);
        vo.setStatus(STATUS_FAILED);
        vo.setFailReason(failReason);
        vo.setCompleteTime(LocalDateTime.now());
        return vo;
    }
}