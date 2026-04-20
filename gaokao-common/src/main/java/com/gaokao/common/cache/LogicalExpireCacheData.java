package com.gaokao.common.cache;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 逻辑过期缓存数据
 *
 * 用于解决缓存击穿问题：
 * 1. 缓存数据不设置物理过期时间，只设置逻辑过期时间
 * 2. 当逻辑过期时，缓存数据仍然存在（返回旧数据）
 * 3. 后台线程异步重建缓存，避免大量请求同时击穿到数据库
 *
 * 设计说明：
 * - expireTime: 逻辑过期时间（数据的有效截止时间）
 * - data: 实际缓存数据
 * - isExpired: 判断是否逻辑过期
 */
@Data
public class LogicalExpireCacheData<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实际缓存数据
     */
    private T data;

    /**
     * 逻辑过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 判断是否逻辑过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 创建逻辑过期缓存数据
     *
     * @param data 实际数据
     * @param expireSeconds 过期秒数
     */
    public static <T> LogicalExpireCacheData<T> of(T data, long expireSeconds) {
        LogicalExpireCacheData<T> cacheData = new LogicalExpireCacheData<>();
        cacheData.setData(data);
        cacheData.setCreateTime(LocalDateTime.now());
        cacheData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        return cacheData;
    }

    /**
     * 创建永不过期的缓存数据
     */
    public static <T> LogicalExpireCacheData<T> neverExpire(T data) {
        LogicalExpireCacheData<T> cacheData = new LogicalExpireCacheData<>();
        cacheData.setData(data);
        cacheData.setCreateTime(LocalDateTime.now());
        cacheData.setExpireTime(null);  // null表示永不过期
        return cacheData;
    }
}