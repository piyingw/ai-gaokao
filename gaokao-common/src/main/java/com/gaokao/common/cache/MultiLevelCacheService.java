package com.gaokao.common.cache;

/**
 * 多级缓存服务接口
 * 实现本地缓存(Caffeine) + 分布式缓存(Redis) 两级架构
 *
 * 设计理念：
 * 1. 本地缓存存储极热点数据，减少Redis访问压力
 * 2. Redis作为二级缓存，保证数据一致性
 * 3. 本地缓存容量有限，只存储高频访问数据
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
     * @return 命中率（0-1）
     */
    double getLocalCacheHitRate();

    /**
     * 获取本地缓存统计信息
     *
     * @return 统计信息字符串
     */
    String getLocalCacheStats();
}