package com.gaokao.common.annotation;

/**
 * 限流类型枚举
 */
public enum RateLimitType {

    /**
     * 基于用户ID限流
     * 每个用户独立计算请求次数
     */
    USER,

    /**
     * 基于IP地址限流
     * 每个IP独立计算请求次数
     */
    IP,

    /**
     * 全局限流
     * 所有用户共享同一个配额
     */
    ALL
}