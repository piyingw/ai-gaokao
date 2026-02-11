package com.gaokao.common.constant;

/**
 * 系统常量
 */
public class SystemConstants {

    private SystemConstants() {
    }

    /**
     * UTF-8 编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Token 过期时间（小时）
     */
    public static final long TOKEN_EXPIRE_HOURS = 24L;

    /**
     * Redis Key 前缀
     */
    public static class RedisKey {
        public static final String USER_TOKEN = "gaokao:user:token:";
        public static final String USER_INFO = "gaokao:user:info:";
        public static final String SMS_CODE = "gaokao:sms:code:";
        public static final String EMAIL_CODE = "gaokao:email:code:";
        public static final String EMAIL_CODE_TIME = "gaokao:email:code:time:";
        public static final String API_RATE_LIMIT = "gaokao:api:limit:";
        
        private RedisKey() {
        }
    }

    /**
     * 志愿批次
     */
    public static class ApplicationBatch {
        public static final String EARLY = "early";      // 提前批
        public static final String UNDERGRADUATE = "undergraduate";  // 本科批
        public static final String SPECIAL = "special";   // 特殊类型批
        public static final String VOCATIONAL = "vocational";  // 专科批
        
        private ApplicationBatch() {
        }
    }

    /**
     * 科类类型
     */
    public static class SubjectType {
        public static final String PHYSICS = "physics";    // 物理类
        public static final String HISTORY = "history";    // 历史类
        public static final String SCIENCE = "science";    // 理科（老高考）
        public static final String ARTS = "arts";          // 文科（老高考）
        
        private SubjectType() {
        }
    }

    /**
     * 院校层次
     */
    public static class UniversityLevel {
        public static final String LEVEL_985 = "985";
        public static final String LEVEL_211 = "211";
        public static final String DOUBLE_FIRST_CLASS = "double_first_class";
        public static final String ORDINARY = "ordinary";
        
        private UniversityLevel() {
        }
    }
}