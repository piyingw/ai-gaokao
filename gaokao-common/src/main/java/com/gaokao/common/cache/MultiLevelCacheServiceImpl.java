package com.gaokao.common.cache;

import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存服务实现
 *
 * 缓存层级：
 * L1: Caffeine本地缓存（容量1000，过期时间5分钟）
 * L2: Redis分布式缓存
 *
 * 缓存穿透防护：空值标记
 * 缓存击穿防护：Redisson分布式锁
 * 缓存雪崩防护：随机过期时间（父类实现）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiLevelCacheServiceImpl implements MultiLevelCacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 本地缓存最大容量
     */
    private static final int LOCAL_CACHE_MAX_SIZE = 1000;

    /**
     * 本地缓存默认过期时间（秒）
     */
    private static final int LOCAL_CACHE_EXPIRE_SECONDS = 300;

    /**
     * 锁的前缀
     */
    private static final String LOCK_PREFIX = "gaokao:lock:";

    /**
     * 空值标记
     */
    private static final String NULL_VALUE = "NULL_VALUE";

    /**
     * 分布式锁等待时间（秒）
     */
    private static final long LOCK_WAIT_TIME = 3;

    /**
     * 分布式锁持有时间（秒）
     */
    private static final long LOCK_LEASE_TIME = 10;

    /**
     * 本地缓存（Caffeine）
     * 配置：
     * - 最大容量1000
     * - 写入后5分钟过期
     * - 记录统计信息
     */
    private final Cache<String, Object> localCache = Caffeine.newBuilder()
            .maximumSize(LOCAL_CACHE_MAX_SIZE)
            .expireAfterWrite(LOCAL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .recordStats()
            .build();

    @Override
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds) {
        // 1. 先查本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            if (NULL_VALUE.equals(localValue)) {
                return null;
            }
            log.debug("本地缓存命中: {}", key);
            return (T) localValue;
        }

        // 2. 本地缓存未命中，查Redis并回填本地缓存
        T data = getFromRedisOrLoad(key, clazz, supplier, expireSeconds);

        // 3. 回填本地缓存
        if (data != null) {
            localCache.put(key, data);
        } else {
            localCache.put(key, NULL_VALUE);
        }

        return data;
    }

    @Override
    public <T> T getOrLoadWithLock(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds) {
        // 1. 先查本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            if (NULL_VALUE.equals(localValue)) {
                return null;
            }
            log.debug("本地缓存命中(带锁模式): {}", key);
            return (T) localValue;
        }

        // 2. 双重检查：先查Redis
        String redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            if (NULL_VALUE.equals(redisValue)) {
                localCache.put(key, NULL_VALUE);
                return null;
            }
            T data = JSON.parseObject(redisValue, clazz);
            localCache.put(key, data);
            log.debug("Redis缓存命中: {}", key);
            return data;
        }

        // 3. 获取分布式锁（防止缓存击穿）
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!locked) {
                Thread.sleep(100);
                return getOrLoadWithLock(key, clazz, supplier, expireSeconds);
            }

            // 4. 获取锁成功，再次检查缓存（双重检查）
            redisValue = redisTemplate.opsForValue().get(key);
            if (redisValue != null) {
                if (NULL_VALUE.equals(redisValue)) {
                    localCache.put(key, NULL_VALUE);
                    return null;
                }
                T data = JSON.parseObject(redisValue, clazz);
                localCache.put(key, data);
                return data;
            }

            // 5. 从数据源获取数据
            log.info("缓存未命中，从数据源加载: {}", key);
            T data = supplier.get();

            // 6. 设置Redis缓存（带随机过期时间防止雪崩）
            if (data != null) {
                setWithRandomExpire(key, data, expireSeconds);
                localCache.put(key, data);
            } else {
                redisTemplate.opsForValue().set(key, NULL_VALUE, 300, TimeUnit.SECONDS);
                localCache.put(key, NULL_VALUE);
            }

            return data;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: {}", lockKey, e);
            return supplier.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        // 先查本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            if (NULL_VALUE.equals(localValue)) {
                return null;
            }
            return (T) localValue;
        }

        // 查Redis并回填本地缓存
        String redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue == null || NULL_VALUE.equals(redisValue)) {
            return null;
        }

        T data = JSON.parseObject(redisValue, clazz);
        localCache.put(key, data);
        return data;
    }

    @Override
    public void set(String key, Object value, long expireSeconds) {
        if (value == null) {
            return;
        }

        // 设置本地缓存
        localCache.put(key, value);

        // 设置Redis缓存（带随机过期时间）
        setWithRandomExpire(key, value, expireSeconds);
    }

    @Override
    public void delete(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
        log.info("缓存删除成功: {}", key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        // 清除本地缓存（Caffeine不支持通配符，清除全部）
        localCache.invalidateAll();
        log.info("本地缓存全部清除");

        // 清除Redis缓存
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Redis缓存批量删除成功, pattern: {}, count: {}", pattern, keys.size());
        }
    }

    @Override
    public boolean exists(String key) {
        // 先查本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null && !NULL_VALUE.equals(localValue)) {
            return true;
        }

        // 查Redis
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void clearLocalCache(String key) {
        localCache.invalidate(key);
        log.debug("本地缓存清除: {}", key);
    }

    @Override
    public void clearLocalCacheByPattern(String pattern) {
        // Caffeine不支持通配符清除，这里清除全部本地缓存
        localCache.invalidateAll();
        log.info("本地缓存全部清除(pattern: {})", pattern);
    }

    @Override
    public void refreshHotData() {
        // 清除本地缓存，让下次请求重新加载
        localCache.invalidateAll();
        log.info("热点数据刷新完成");
    }

    @Override
    public double getLocalCacheHitRate() {
        CacheStats stats = localCache.stats();
        long total = stats.hitCount() + stats.missCount();
        return total == 0 ? 0 : stats.hitCount() / (double) total;
    }

    @Override
    public String getLocalCacheStats() {
        CacheStats stats = localCache.stats();
        return String.format("本地缓存统计: 命中率=%.2f%%, 命中次数=%d, 未命中次数=%d, 缓存大小=%d",
                getLocalCacheHitRate() * 100,
                stats.hitCount(),
                stats.missCount(),
                localCache.estimatedSize());
    }

    /**
     * 从Redis获取或加载数据
     */
    private <T> T getFromRedisOrLoad(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds) {
        String redisValue = redisTemplate.opsForValue().get(key);

        if (redisValue != null) {
            if (NULL_VALUE.equals(redisValue)) {
                return null;
            }
            log.debug("Redis缓存命中: {}", key);
            return JSON.parseObject(redisValue, clazz);
        }

        // Redis未命中，从数据源加载
        log.debug("缓存未命中，从数据源加载: {}", key);
        T data = supplier.get();

        // 设置Redis缓存
        if (data != null) {
            setWithRandomExpire(key, data, expireSeconds);
        } else {
            redisTemplate.opsForValue().set(key, NULL_VALUE, 300, TimeUnit.SECONDS);
        }

        return data;
    }

    /**
     * 设置Redis缓存（带随机过期时间防止缓存雪崩）
     * 随机偏移范围：±10%
     */
    private void setWithRandomExpire(String key, Object value, long baseExpireSeconds) {
        String json = JSON.toJSONString(value);

        // 计算随机偏移（±10%）
        long randomOffset = (long) (baseExpireSeconds * 0.1 * (Math.random() - 0.5));
        long actualExpire = baseExpireSeconds + randomOffset;

        redisTemplate.opsForValue().set(key, json, actualExpire, TimeUnit.SECONDS);
        log.debug("Redis缓存设置: key={}, expire={}s (基础{}s, 偏移{}s)",
                key, actualExpire, baseExpireSeconds, randomOffset);
    }
}