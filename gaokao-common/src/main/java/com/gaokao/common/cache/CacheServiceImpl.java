package com.gaokao.common.cache;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存服务实现类
 * 
 * 使用 Redisson 分布式锁解决以下问题：
 * 1. 缓存击穿：热点 key 过期瞬间大量请求穿透到数据库
 * 2. 缓存雪崩：大量 key 同时过期
 * 3. 缓存穿透：查询不存在的数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 锁的前缀
     */
    private static final String LOCK_PREFIX = "gaokao:lock:";

    /**
     * 空值标记，用于防止缓存穿透
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

    @Override
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds) {
        // 1. 尝试从缓存获取
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            // 空值标记，返回 null（防止缓存穿透）
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return JSON.parseObject(value, clazz);
        }

        // 2. 缓存不存在，从数据源获取
        T data = supplier.get();
        
        // 3. 设置缓存
        if (data != null) {
            set(key, data, expireSeconds);
        } else {
            // 设置空值标记，防止缓存穿透（过期时间较短）
            redisTemplate.opsForValue().set(key, NULL_VALUE, expireSeconds / 10, TimeUnit.SECONDS);
        }
        
        return data;
    }

    @Override
    public <T> T getOrLoadWithLock(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds) {
        // 1. 双重检查：先尝试从缓存获取
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return JSON.parseObject(value, clazz);
        }

        // 2. 获取分布式锁（防止缓存击穿）
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3. 尝试获取锁
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!locked) {
                // 获取锁失败，等待其他线程完成后再次尝试获取缓存
                Thread.sleep(100);
                return getOrLoadWithLock(key, clazz, supplier, expireSeconds);
            }

            // 4. 获取锁成功，再次检查缓存（双重检查）
            value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                if (NULL_VALUE.equals(value)) {
                    return null;
                }
                return JSON.parseObject(value, clazz);
            }

            // 5. 从数据源获取数据
            log.info("缓存未命中，从数据源加载: {}", key);
            T data = supplier.get();

            // 6. 设置缓存
            if (data != null) {
                set(key, data, expireSeconds);
                log.info("缓存设置成功: {}, 过期时间: {}s", key, expireSeconds);
            } else {
                // 设置空值标记，防止缓存穿透
                redisTemplate.opsForValue().set(key, NULL_VALUE, 300, TimeUnit.SECONDS);
                log.info("设置空值标记: {}", key);
            }

            return data;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: {}", lockKey, e);
            // 降级：直接从数据源获取
            return supplier.get();
        } finally {
            // 7. 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || NULL_VALUE.equals(value)) {
            return null;
        }
        return JSON.parseObject(value, clazz);
    }

    @Override
    public void set(String key, Object value, long expireSeconds) {
        if (value == null) {
            return;
        }
        String json = JSON.toJSONString(value);

        // 添加随机偏移防止缓存雪崩（±10%）
        long randomOffset = (long) (expireSeconds * 0.1 * (Math.random() - 0.5));
        long actualExpire = expireSeconds + randomOffset;

        redisTemplate.opsForValue().set(key, json, actualExpire, TimeUnit.SECONDS);
        log.debug("缓存设置: key={}, expire={}s (基础{}s, 偏移{}s)", key, actualExpire, expireSeconds, randomOffset);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
        log.info("缓存删除成功: {}", key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("批量删除缓存成功, pattern: {}, count: {}", pattern, keys.size());
        }
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}