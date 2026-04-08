package com.gaokao.member.vo;

import lombok.Data;

/**
 * 会员权益VO
 */
@Data
public class MemberPrivilegeVO {

    /**
     * 权益代码
     */
    private String privilegeCode;

    /**
     * 权益名称
     */
    private String privilegeName;

    /**
     * 每日使用次数限制（-1表示无限制）
     */
    private Integer limitCount;

    /**
     * 今日已使用次数
     */
    private Integer usedCount;

    /**
     * 权益描述
     */
    private String description;
}