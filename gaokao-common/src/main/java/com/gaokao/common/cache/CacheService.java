package com.gaokao.common.cache;

import java.util.function.Supplier;

/**
 * 缓存服务接口
 */
public interface CacheService {

    /**
     * 获取缓存（如果不存在则通过 supplier 获取并缓存）
     *
     * @param key           缓存键
     * @param clazz         返回类型
     * @param supplier      数据获取函数
     * @param expireSeconds 过期时间（秒）
     * @param <T>           返回类型
     * @return 缓存数据
     */
    <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds);

    /**
     * 获取缓存（使用分布式锁防止缓存击穿）
     *
     * @param key           缓存键
     * @param clazz         返回类型
     * @param supplier      数据获取函数
     * @param expireSeconds 过期时间（秒）
     * @param <T>           返回类型
     * @return 缓存数据
     */
    <T> T getOrLoadWithLock(String key, Class<T> clazz, Supplier<T> supplier, long expireSeconds);

    /**
     * 直接获取缓存
     *
     * @param key   缓存键
     * @param clazz 返回类型
     * @param <T>   返回类型
     * @return 缓存数据，不存在返回 null
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 设置缓存
     *
     * @param key           缓存键
     * @param value         缓存值
     * @param expireSeconds 过期时间（秒）
     */
    void set(String key, Object value, long expireSeconds);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 批量删除缓存（支持通配符）
     *
     * @param pattern 缓存键模式
     */
    void deleteByPattern(String pattern);

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);
}