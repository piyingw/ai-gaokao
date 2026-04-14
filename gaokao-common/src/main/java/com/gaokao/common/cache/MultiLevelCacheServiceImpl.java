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
 * 缓存穿透防护：布隆过滤器 + 空值标记双重保障
 * 缓存击穿防护：Redisson分布式锁 / 逻辑过期方案
 * 缓存雪崩防护：随机过期时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiLevelCacheServiceImpl implements MultiLevelCacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final BloomFilterService bloomFilterService;

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

    /**
     * 带布隆过滤器的查询（防止缓存穿透）
     *
     * 设计说明：
     * 1. 先通过布隆过滤器判断Key是否可能存在
     * 2. 如果布隆过滤器判断不存在，直接返回null，无需查询数据库
     * 3. 如果布隆过滤器判断可能存在，继续正常缓存查询流程
     * 4. 查询结果为空时，仍然缓存空值标记（双重保障）
     *
     * 注意：布隆过滤器可能存在误判（判断存在但实际不存在）
     *       但不会漏判（判断不存在则一定不存在）
     *       所以当判断不存在时，可以放心返回null
     */
    @Override
    public <T> T getWithBloomFilter(String key, Class<T> clazz, Supplier<T> supplier,
                                    long expireSeconds, String bloomFilterType) {
        // 1. 先查本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            if (NULL_VALUE.equals(localValue)) {
                return null;
            }
            log.debug("本地缓存命中(布隆过滤器模式): {}", key);
            return (T) localValue;
        }

        // 2. 布隆过滤器判断（防止缓存穿透）
        String keyId = extractIdFromKey(key);
        boolean mightExist = checkBloomFilter(keyId, bloomFilterType);

        if (!mightExist) {
            log.warn("布隆过滤器判断不存在，直接返回null: key={}, type={}", key, bloomFilterType);
            return null;
        }

        // 3. 布隆过滤器判断可能存在，继续正常查询流程
        return getOrLoadWithLock(key, clazz, supplier, expireSeconds);
    }

    /**
     * 根据类型检查布隆过滤器
     */
    private boolean checkBloomFilter(String keyId, String bloomFilterType) {
        if (bloomFilterType == null || bloomFilterType.isEmpty()) {
            // 未指定类型，使用通用布隆过滤器
            return bloomFilterService.mightContainKey(keyId);
        }

        switch (bloomFilterType.toLowerCase()) {
            case "university":
                return bloomFilterService.mightContainUniversity(keyId);
            case "major":
                return bloomFilterService.mightContainMajor(keyId);
            default:
                return bloomFilterService.mightContainKey(keyId);
        }
    }

    /**
     * 从缓存Key中提取ID
     * 例如：gaokao:university:123 -> 123
     */
    private String extractIdFromKey(String key) {
        if (key == null) {
            return "";
        }
        String[] parts = key.split(":");
        return parts.length > 0 ? parts[parts.length - 1] : key;
    }

    /**
     * 逻辑过期缓存查询（防止缓存击穿）
     *
     * 设计说明：
     * 1. 缓存数据不设置物理TTL，设置逻辑过期时间
     * 2. 查询时检查逻辑过期：
     *    - 未过期：直接返回缓存数据
     *    - 已过期：返回旧数据，同时触发异步重建
     * 3. 使用互斥锁保证只有一个线程执行重建
     *
     * 适用场景：
     * - 热点数据（如首页推荐、热门院校）
     * - 对一致性要求不高，可接受短暂返回旧数据
     */
    @Override
    public <T> T getWithLogicalExpire(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds) {
        // 1. 查询缓存（带逻辑过期时间）
        String redisValue = redisTemplate.opsForValue().get(key);

        if (redisValue != null) {
            // 解析逻辑过期数据
            LogicalExpireCacheData<T> cacheData = JSON.parseObject(redisValue,
                    new com.alibaba.fastjson2.TypeReference<LogicalExpireCacheData<T>>() {});

            if (cacheData != null && cacheData.getData() != null) {
                // 2. 检查是否逻辑过期
                if (!cacheData.isExpired()) {
                    // 未过期，直接返回
                    log.debug("逻辑过期缓存命中（未过期）：key={}", key);
                    // 回填本地缓存
                    localCache.put(key, cacheData.getData());
                    return cacheData.getData();
                }

                // 3. 已过期，返回旧数据，同时尝试异步重建
                log.info("逻辑过期缓存已过期，返回旧数据并触发重建：key={}", key);

                // 4. 尝试获取重建锁（只有获取到锁的线程才执行重建）
                String rebuildLockKey = LOCK_PREFIX + "rebuild:" + key;
                RLock rebuildLock = redissonClient.getLock(rebuildLockKey);

                try {
                    // 非阻塞尝试获取锁（立即返回结果）
                    boolean locked = rebuildLock.tryLock(0, 10, TimeUnit.SECONDS);

                    if (locked) {
                        // 获取到锁，异步执行重建
                        asyncRebuildCache(key, supplier, expireSeconds);
                    } else {
                        // 未获取到锁，说明已有其他线程在重建，直接返回旧数据
                        log.debug("已有其他线程在重建缓存：key={}", key);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("获取重建锁被中断：key={}", key);
                } finally {
                    // 注意：异步重建会在完成后释放锁
                }

                // 返回旧数据（保证服务可用）
                return cacheData.getData();
            }
        }

        // 5. 缓存不存在，需要首次加载
        log.info("逻辑过期缓存不存在，首次加载：key={}", key);

        // 首次加载使用分布式锁（防止并发击穿）
        T data = getOrLoadWithLock(key, clazz, supplier, expireSeconds);

        // 包装为逻辑过期数据存储
        if (data != null) {
            LogicalExpireCacheData<T> cacheData = LogicalExpireCacheData.of(data, expireSeconds);
            redisTemplate.opsForValue().set(key, JSON.toJSONString(cacheData));
            localCache.put(key, data);
        }

        return data;
    }

    /**
     * 异步重建缓存
     *
     * 使用独立线程池执行重建，避免阻塞请求线程
     */
    @Override
    public <T> void asyncRebuildCache(String key, Supplier<T> supplier, long expireSeconds) {
        // 使用CompletableFuture异步执行
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                log.info("开始异步重建缓存：key={}", key);

                // 1. 从数据源获取最新数据
                T data = supplier.get();

                if (data != null) {
                    // 2. 包装为逻辑过期数据
                    LogicalExpireCacheData<T> cacheData = LogicalExpireCacheData.of(data, expireSeconds);

                    // 3. 更新Redis缓存（不设置物理过期）
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(cacheData));

                    // 4. 更新本地缓存
                    localCache.put(key, data);

                    log.info("异步重建缓存完成：key={}", key);
                } else {
                    log.warn("异步重建缓存数据为空：key={}", key);
                }

            } catch (Exception e) {
                log.error("异步重建缓存失败：key={}", key, e);
            } finally {
                // 5. 释放重建锁
                String rebuildLockKey = LOCK_PREFIX + "rebuild:" + key;
                RLock rebuildLock = redissonClient.getLock(rebuildLockKey);
                if (rebuildLock.isHeldByCurrentThread()) {
                    rebuildLock.unlock();
                }
            }
        });
    }
}