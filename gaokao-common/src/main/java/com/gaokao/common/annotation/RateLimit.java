package com.gaokao.common.annotation;

import java.lang.annotation.*;

/**
 * 分布式限流注解
 *
 * 使用Redisson的RRateLimiter实现分布式限流
 * 可标注在Controller方法上，保护接口不被滥用
 *
 * 示例用法：
 * @RateLimit(permits = 10, timeWindow = 60)  // 每分钟最多10次
 * public Result<?> chat(...) { ... }
 *
 * @RateLimit(permits = 5, type = RateLimitType.IP)  // 每IP每分钟最多5次
 * public Result<?> sensitiveApi(...) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 每时间窗口允许的请求次数
     */
    int permits() default 10;

    /**
     * 时间窗口大小（秒）
     */
    int timeWindow() default 60;

    /**
     * 限流类型
     * USER: 基于用户ID限流（需要用户已登录）
     * IP: 基于IP地址限流
     * ALL: 全局限流（所有用户共享配额）
     */
    RateLimitType type() default RateLimitType.USER;

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 是否等待获取许可
     * true: 等待直到获取许可或超时
     * false: 立即返回，获取不到许可则抛出异常
     */
    boolean waiting() default false;

    /**
     * 等待超时时间（毫秒），仅当waiting=true时有效
     */
    long waitTimeout() default 1000;
}