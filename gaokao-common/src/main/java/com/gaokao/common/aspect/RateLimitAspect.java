package com.gaokao.common.aspect;

import com.gaokao.common.annotation.RateLimit;
import com.gaokao.common.annotation.RateLimitType;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式限流切面
 *
 * 使用Redisson的RRateLimiter实现分布式限流
 * 支持基于用户ID、IP地址或全局限流
 *
 * 原理：
 * 1. 在Redis中维护一个计数器，记录请求次数
 * 2. 使用RateLimiter的令牌桶算法控制请求速率
 * 3. 超过配额的请求会被拒绝
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;

    /**
     * 限流器Key前缀
     */
    private static final String RATE_LIMITER_KEY_PREFIX = "gaokao:rate_limit:";

    /**
     * 环绕通知，拦截所有标注了@RateLimit的方法
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 构建限流器Key
        String key = buildRateLimiterKey(point, rateLimit.type());

        // 获取或创建限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 设置限流规则
        // RateType.OVERALL 表示全局限流（对所有客户端共享配额）
        // 但我们通过不同的Key实现用户/IP级别的限流
        rateLimiter.trySetRate(RateType.OVERALL, rateLimit.permits(), rateLimit.timeWindow(), RateIntervalUnit.SECONDS);

        // 尝试获取许可
        boolean acquired;
        if (rateLimit.waiting()) {
            // 等待模式：阻塞直到获取许可或超时
            acquired = rateLimiter.tryAcquire(rateLimit.waitTimeout(), TimeUnit.MILLISECONDS);
        } else {
            // 非等待模式：立即尝试获取
            acquired = rateLimiter.tryAcquire();
        }

        if (!acquired) {
            log.warn("限流触发: key={}, type={}, permits={}/{}s", key, rateLimit.type(), rateLimit.permits(), rateLimit.timeWindow());
            throw new BusinessException(ResultCode.RATE_LIMIT_EXCEEDED, rateLimit.message());
        }

        log.debug("限流检查通过: key={}", key);
        return point.proceed();
    }

    /**
     * 构建限流器Key
     * 根据限流类型，Key的构成不同：
     * - USER: gaokao:rate_limit:{methodName}:{userId}
     * - IP: gaokao:rate_limit:{methodName}:{ip}
     * - ALL: gaokao:rate_limit:{methodName}
     */
    private String buildRateLimiterKey(ProceedingJoinPoint point, RateLimitType type) {
        // 获取方法名
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // 获取请求信息
        HttpServletRequest request = getRequest();
        String identifier = getIdentifier(request, type);

        return RATE_LIMITER_KEY_PREFIX + methodName + ":" + identifier;
    }

    /**
     * 根据限流类型获取标识符
     */
    private String getIdentifier(HttpServletRequest request, RateLimitType type) {
        if (request == null) {
            return "unknown";
        }

        switch (type) {
            case USER:
                // 从请求属性中获取用户ID（由拦截器设置）
                Object userId = request.getAttribute("userId");
                return userId != null ? userId.toString() : "anonymous";

            case IP:
                // 获取客户端IP
                return getClientIp(request);

            case ALL:
                // 全局限流，所有用户共享
                return "global";

            default:
                return "unknown";
        }
    }

    /**
     * 获取客户端真实IP
     * 处理代理情况，获取真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于多级代理，取第一个非unknown的IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}