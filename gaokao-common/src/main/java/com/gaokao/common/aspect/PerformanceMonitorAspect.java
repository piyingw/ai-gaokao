package com.gaokao.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控切面
 *
 * 监控Controller层方法的执行耗时，记录性能数据
 * 可用于：
 * 1. 发现性能瓶颈
 * 2. 监控系统健康状况
 * 3. 为优化提供数据支持
 *
 * 日志输出示例：
 * [性能监控] AIController.chat 执行耗时: 1523ms, 状态: 成功
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitorAspect {

    /**
     * 性能统计数据存储
     * Key: 方法名, Value: 统计信息
     */
    private final ConcurrentHashMap<String, MethodStats> statsMap = new ConcurrentHashMap<>();

    /**
     * 慢查询阈值（毫秒）
     * 超过此阈值的请求会被标记为慢请求
     */
    private static final long SLOW_REQUEST_THRESHOLD = 3000;

    /**
     * 监控Controller层所有方法
     */
    @Around("execution(* com.gaokao.web.controller.*.*(..))")
    public Object monitor(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = getMethodName(point);

        try {
            Object result = point.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // 记录成功执行的统计
            recordStats(methodName, duration, true);

            // 根据耗时级别输出不同日志
            if (duration > SLOW_REQUEST_THRESHOLD) {
                log.warn("[性能监控] {} 执行耗时: {}ms [慢请求]", methodName, duration);
            } else if (duration > 1000) {
                log.info("[性能监控] {} 执行耗时: {}ms", methodName, duration);
            } else {
                log.debug("[性能监控] {} 执行耗时: {}ms", methodName, duration);
            }

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;

            // 记录失败执行的统计
            recordStats(methodName, duration, false);

            log.error("[性能监控] {} 执行耗时: {}ms, 状态: 失败, 异常: {}",
                    methodName, duration, e.getClass().getSimpleName());

            throw e;
        }
    }

    /**
     * 获取方法名
     */
    private String getMethodName(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    /**
     * 记录性能统计
     */
    private void recordStats(String methodName, long duration, boolean success) {
        MethodStats stats = statsMap.computeIfAbsent(methodName, k -> new MethodStats());
        stats.record(duration, success);
    }

    /**
     * 获取性能统计报告
     *
     * @return 统计报告字符串
     */
    public String getStatsReport() {
        StringBuilder sb = new StringBuilder("性能统计报告:\n");

        statsMap.forEach((method, stats) -> {
            sb.append(String.format("  - %s: 总调用=%d, 成功=%d, 失败=%d, 平均耗时=%.2fms, 最大耗时=%dms\n",
                    method,
                    stats.totalCount.get(),
                    stats.successCount.get(),
                    stats.failureCount.get(),
                    stats.getAverageDuration(),
                    stats.maxDuration.get()));
        });

        return sb.toString();
    }

    /**
     * 获取慢请求列表
     *
     * @param thresholdMs 阈值（毫秒）
     * @return 慢请求方法列表
     */
    public String getSlowMethods(long thresholdMs) {
        StringBuilder sb = new StringBuilder("慢请求方法列表(阈值>" + thresholdMs + "ms):\n");

        statsMap.forEach((method, stats) -> {
            if (stats.maxDuration.get() > thresholdMs) {
                sb.append(String.format("  - %s: 最大耗时=%dms\n", method, stats.maxDuration.get()));
            }
        });

        return sb.toString();
    }

    /**
     * 清空统计数据
     */
    public void clearStats() {
        statsMap.clear();
        log.info("性能统计数据已清空");
    }

    /**
     * 方法统计信息
     */
    private static class MethodStats {
        final AtomicLong totalCount = new AtomicLong(0);
        final AtomicLong successCount = new AtomicLong(0);
        final AtomicLong failureCount = new AtomicLong(0);
        final AtomicLong totalDuration = new AtomicLong(0);
        final AtomicLong maxDuration = new AtomicLong(0);
        final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);

        void record(long duration, boolean success) {
            totalCount.incrementAndGet();
            totalDuration.addAndGet(duration);

            if (success) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }

            // 更新最大最小值
            long currentMax = maxDuration.get();
            if (duration > currentMax) {
                maxDuration.compareAndSet(currentMax, duration);
            }

            long currentMin = minDuration.get();
            if (duration < currentMin) {
                minDuration.compareAndSet(currentMin, duration);
            }
        }

        double getAverageDuration() {
            long total = totalCount.get();
            return total == 0 ? 0 : totalDuration.get() / (double) total;
        }
    }
}