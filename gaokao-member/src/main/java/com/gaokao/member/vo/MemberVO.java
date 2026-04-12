package com.gaokao.member.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员信息VO
 */
@Data
public class MemberVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会员等级代码
     */
    private String levelCode;

    /**
     * 会员等级名称
     */
    private String levelName;

    /**
     * 会员开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 会员结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 会员状态：0-已过期 1-正常 2-冻结
     */
    private Integer status;

    /**
     * 累计消费金额
     */
    private BigDecimal totalSpent;

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 剩余天数
     */
    private Long remainingDays;
}