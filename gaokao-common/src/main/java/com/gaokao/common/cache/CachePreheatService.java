package com.gaokao.common.cache;

import com.gaokao.common.constant.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存预热服务
 *
 * 在系统启动时预加载热点数据到Redis缓存，避免冷启动时的性能问题
 *
 * 预热数据包括：
 * 1. 省份列表 - 高频查询，几乎不变
 * 2. 院校层次列表 - 985/211/双一流等，变化很少
 * 3. 专业类别列表 - 工学/理学/文学等，变化很少
 *
 * 注意：由于本服务在gaokao-common模块，无法直接访问gaokao-data的数据服务
 * 实际预热逻辑应在gaokao-web模块中实现，此处仅提供预热工具方法
 */
@Slf4j
@Service
@Order(1)  // 启动优先级
@RequiredArgsConstructor
public class CachePreheatService implements ApplicationRunner {

    private final StringRedisTemplate redisTemplate;

    /**
     * 应用启动时执行预热
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("======== 开始缓存预热 ========");
        long startTime = System.currentTimeMillis();

        try {
            // 预热基础数据（由子模块实现具体数据获取）
            preheatBasicData();

            long duration = System.currentTimeMillis() - startTime;
            log.info("======== 缓存预热完成，耗时: {}ms ========", duration);

        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
    }

    /**
     * 预热基础数据
     * 实际数据获取逻辑由gaokao-web模块的PreheatRunner实现
     */
    private void preheatBasicData() {
        // 这里只设置预热标记，实际数据由业务模块填充
        log.info("预热标记已设置，等待业务模块填充数据");
    }

    /**
     * 设置热点数据缓存（供外部调用）
     *
     * @param key           缓存键
     * @param value         缓存值（JSON字符串）
     * @param expireSeconds 过期时间
     */
    public void setHotData(String key, String value, long expireSeconds) {
        // 添加随机偏移防止雪崩
        long randomOffset = (long) (expireSeconds * 0.1 * (Math.random() - 0.5));
        long actualExpire = expireSeconds + randomOffset;

        redisTemplate.opsForValue().set(key, value, actualExpire, TimeUnit.SECONDS);
        log.info("热点数据预热完成: key={}, expire={}s", key, actualExpire);
    }

    /**
     * 检查缓存是否已预热
     *
     * @param key 缓存键
     * @return 是否已存在
     */
    public boolean isPreheated(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取预热状态报告
     *
     * @return 预热状态字符串
     */
    public String getPreheatStatus() {
        StringBuilder sb = new StringBuilder("缓存预热状态:\n");

        checkAndReport(sb, CacheConstants.PROVINCE_LIST, "省份列表");
        checkAndReport(sb, CacheConstants.LEVEL_LIST, "院校层次列表");
        checkAndReport(sb, CacheConstants.CATEGORY_LIST, "专业类别列表");

        return sb.toString();
    }

    private void checkAndReport(StringBuilder sb, String key, String name) {
        boolean exists = isPreheated(key);
        sb.append(String.format("  - %s: %s\n", name, exists ? "已预热" : "未预热"));
    }
}