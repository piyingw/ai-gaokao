package com.gaokao.common.constant;

/**
 * 缓存常量定义
 * 
 * 缓存 Key 命名规范：gaokao:模块:业务:标识
 * 例如：gaokao:university:detail:1
 */
public class CacheConstants {

    private CacheConstants() {
    }

    /**
     * 缓存 Key 前缀
     */
    public static final String CACHE_PREFIX = "gaokao:cache:";

    // ==================== 院校相关缓存 ====================

    /**
     * 院校详情缓存 Key
     * 完整格式：gaokao:cache:university:detail:{id}
     */
    public static final String UNIVERSITY_DETAIL = CACHE_PREFIX + "university:detail:";

    /**
     * 院校详情缓存过期时间（秒）- 1 小时
     */
    public static final long UNIVERSITY_DETAIL_TTL = 3600;

    /**
     * 院校列表缓存 Key
     * 完整格式：gaokao:cache:university:list:{province}:{level}:{pageNum}
     */
    public static final String UNIVERSITY_LIST = CACHE_PREFIX + "university:list:";

    /**
     * 院校列表缓存过期时间（秒）- 30 分钟
     */
    public static final long UNIVERSITY_LIST_TTL = 1800;

    /**
     * 院校分数线缓存 Key
     * 完整格式：gaokao:cache:university:scores:{universityId}:{province}:{subjectType}
     */
    public static final String UNIVERSITY_SCORES = CACHE_PREFIX + "university:scores:";

    /**
     * 院校分数线缓存过期时间（秒）- 2 小时
     */
    public static final long UNIVERSITY_SCORES_TTL = 7200;

    // ==================== 专业相关缓存 ====================

    /**
     * 专业详情缓存 Key
     * 完整格式：gaokao:cache:major:detail:{id}
     */
    public static final String MAJOR_DETAIL = CACHE_PREFIX + "major:detail:";

    /**
     * 专业详情缓存过期时间（秒）- 1 小时
     */
    public static final long MAJOR_DETAIL_TTL = 3600;

    /**
     * 专业列表缓存 Key
     * 完整格式：gaokao:cache:major:list:{category}:{pageNum}
     */
    public static final String MAJOR_LIST = CACHE_PREFIX + "major:list:";

    /**
     * 专业列表缓存过期时间（秒）- 30 分钟
     */
    public static final long MAJOR_LIST_TTL = 1800;

    // ==================== AI 推荐相关缓存 ====================

    /**
     * AI 推荐结果缓存 Key
     * 完整格式：gaokao:cache:recommend:{userId}:{score}:{province}
     */
    public static final String AI_RECOMMEND = CACHE_PREFIX + "ai:recommend:";

    /**
     * AI 推荐结果缓存过期时间（秒）- 24 小时
     */
    public static final long AI_RECOMMEND_TTL = 86400;

    /**
     * 同分去向分析缓存 Key
     * 完整格式：gaokao:cache:sameScore:{score}:{province}:{subjectType}
     */
    public static final String SAME_SCORE_ANALYSIS = CACHE_PREFIX + "analysis:sameScore:";

    /**
     * 同分去向分析缓存过期时间（秒）- 12 小时
     */
    public static final long SAME_SCORE_ANALYSIS_TTL = 43200;

    // ==================== 政策相关缓存 ====================

    /**
     * 政策文档缓存 Key
     * 完整格式：gaokao:cache:policy:{province}:{year}
     */
    public static final String POLICY_LIST = CACHE_PREFIX + "policy:list:";

    /**
     * 政策文档缓存过期时间（秒）- 6 小时
     */
    public static final long POLICY_LIST_TTL = 21600;

    /**
     * 政策问答缓存 Key
     * 完整格式：gaokao:cache:policy:qa:{questionHash}
     */
    public static final String POLICY_QA = CACHE_PREFIX + "policy:qa:";

    /**
     * 政策问答缓存过期时间（秒）- 1 小时
     */
    public static final long POLICY_QA_TTL = 3600;

    // ==================== 热点数据缓存 ====================

    /**
     * 省份列表缓存 Key
     */
    public static final String PROVINCE_LIST = CACHE_PREFIX + "province:list";

    /**
     * 省份列表缓存过期时间（秒）- 24 小时
     */
    public static final long PROVINCE_LIST_TTL = 86400;

    /**
     * 院校层次列表缓存 Key
     */
    public static final String LEVEL_LIST = CACHE_PREFIX + "level:list";

    /**
     * 院校层次列表缓存过期时间（秒）- 24 小时
     */
    public static final long LEVEL_LIST_TTL = 86400;

    /**
     * 专业类别列表缓存 Key
     */
    public static final String CATEGORY_LIST = CACHE_PREFIX + "category:list";

    /**
     * 专业类别列表缓存过期时间（秒）- 24 小时
     */
    public static final long CATEGORY_LIST_TTL = 86400;
}