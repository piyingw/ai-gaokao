package com.gaokao.common.cache;

/**
 * 多级缓存服务接口
 * 实现本地缓存(Caffeine) + 分布式缓存(Redis) 两级架构
 *
 * 设计理念：
 * 1. 本地缓存存储极热点数据，减少Redis访问压力
 * 2. Redis作为二级缓存，保证数据一致性
 * 3. 本地缓存容量有限，只存储高频访问数据
 * 4. 布隆过滤器防止缓存穿透（判断不存在则直接返回）
 */
public interface MultiLevelCacheService extends CacheService {

    /**
     * 清除本地缓存（数据更新时调用）
     *
     * @param key 缓存键
     */
    void clearLocalCache(String key);

    /**
     * 清除本地缓存（支持通配符）
     *
     * @param pattern 缓存键模式
     */
    void clearLocalCacheByPattern(String pattern);

    /**
     * 刷新热点数据到本地缓存
     * 通常在系统启动或定时任务中调用
     */
    void refreshHotData();

    /**
     * 获取本地缓存命中率
     *
     * @return 呔中率（0-1）
     */
    double getLocalCacheHitRate();

    /**
     * 获取本地缓存统计信息
     *
     * @return 统计信息字符串
     */
    String getLocalCacheStats();

    /**
     * 带布隆过滤器的查询（防止缓存穿透）
     *
     * @param key 缓存键
     * @param clazz 返回类型
     * @param supplier 数据加载器
     * @param expireSeconds 过期时间
     * @param bloomFilterType 布隆过滤器类型（university/major/general）
     * @return 数据
     */
    <T> T getWithBloomFilter(String key, Class<T> clazz, java.util.function.Supplier<T> supplier,
                             long expireSeconds, String bloomFilterType);

    /**
     * 逻辑过期缓存查询（防止缓存击穿）
     *
     * 设计说明：
     * 1. 缓存数据设置逻辑过期时间，不设置物理TTL
     * 2. 逻辑过期后，返回旧数据，同时触发异步重建
     * 3. 避免热点数据过期瞬间大量请求击穿到数据库
     *
     * @param key 缓存键
     * @param clazz 返回类型
     * @param supplier 数据加载器
     * @param expireSeconds 逻辑过期时间（秒）
     * @return 数据（可能返回旧数据）
     */
    <T> T getWithLogicalExpire(String key, Class<T> clazz, java.util.function.Supplier<T> supplier,
                               long expireSeconds);

    /**
     * 异步重建缓存（逻辑过期方案调用）
     *
     * @param key 缓存键
     * @param supplier 数据加载器
     * @param expireSeconds 逻辑过期时间
     */
    <T> void asyncRebuildCache(String key, java.util.function.Supplier<T> supplier, long expireSeconds);
}