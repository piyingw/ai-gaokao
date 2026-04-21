package com.gaokao.ai.agent.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索决策结果
 * Agent自主决定如何获取数据的决策记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalDecision {

    /**
     * 决策ID
     */
    private String decisionId;

    /**
     * 选定的数据源
     * LOCAL_DB - 本地数据库
     * WEB_SEARCH - 网络搜索
     * RAG - 向量检索
     * MEMORY - 长期记忆
     * NONE - 无需检索
     */
    private DataSource dataSource;

    /**
     * 要执行的技能名称
     */
    private String skillName;

    /**
     * 技能参数
     */
    private Map<String, Object> skillParams;

    /**
     * 决策置信度
     */
    private Double confidence;

    /**
     * 决策理由
     */
    private String reasoning;

    /**
     * 问题类型分析
     */
    private QuestionType questionType;

    /**
     * 数据需求评估
     */
    private DataRequirement dataRequirement;

    /**
     * 是否需要验证结果
     */
    @Builder.Default
    private boolean needsValidation = true;

    /**
     * 备选方案（当首选失败时）
     */
    private RetrievalDecision fallbackDecision;

    /**
     * 数据源枚举
     */
    public enum DataSource {
        LOCAL_DB("本地数据库", 1),
        WEB_SEARCH("网络搜索", 2),
        RAG("向量检索", 1),
        MEMORY("长期记忆", 1),
        NONE("无需检索", 0),
        HYBRID("混合检索", 3);

        private final String description;
        private final int priority; // 优先级，数字越小优先级越高

        DataSource(String description, int priority) {
            this.description = description;
            this.priority = priority;
        }

        public String getDescription() {
            return description;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * 问题类型枚举
     */
    public enum QuestionType {
        SCORE_QUERY("分数查询", "查询分数线、录取分数"),
        UNIVERSITY_QUERY("院校查询", "查询院校信息、排名"),
        MAJOR_QUERY("专业查询", "查询专业信息、就业方向"),
        POLICY_QUERY("政策查询", "查询政策规则、录取规则"),
        RECOMMEND_REQUEST("推荐请求", "生成志愿方案、录取概率"),
        PERSONALITY_ANALYSIS("性格分析", "分析性格特点、职业兴趣"),
        COMPARISON("对比分析", "院校对比、专业对比"),
        GENERAL_INFO("一般信息", "其他信息查询"),
        UNKNOWN("未知类型", "需要更多信息");

        private final String name;
        private final String description;

        QuestionType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 数据需求评估
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataRequirement {
        private boolean needsUniversityData;
        private boolean needsScoreData;
        private boolean needsMajorData;
        private boolean needsPolicyData;
        private boolean needsWebData; // 本地数据可能不存在
        private boolean needsMemoryData; // 需要用户历史信息

        private String urgency; // high, medium, low
        private String specificity; // specific, general
    }

    /**
     * 创建无需检索的决策
     */
    public static RetrievalDecision noRetrievalNeeded(String reasoning) {
        return RetrievalDecision.builder()
                .dataSource(DataSource.NONE)
                .reasoning(reasoning)
                .confidence(1.0)
                .build();
    }

    /**
     * 创建本地数据库检索决策
     */
    public static RetrievalDecision localDbQuery(String skillName, Map<String, Object> params, String reasoning) {
        return RetrievalDecision.builder()
                .dataSource(DataSource.LOCAL_DB)
                .skillName(skillName)
                .skillParams(params)
                .reasoning(reasoning)
                .confidence(0.9)
                .needsValidation(true)
                .build();
    }

    /**
     * 创建网络搜索决策
     */
    public static RetrievalDecision webSearch(String query, String searchType, String reasoning) {
        return RetrievalDecision.builder()
                .dataSource(DataSource.WEB_SEARCH)
                .skillName("web-search-skill")
                .skillParams(Map.of("query", query, "searchType", searchType))
                .reasoning(reasoning)
                .confidence(0.7)
                .needsValidation(true)
                .build();
    }

    /**
     * 创建带备选的决策
     */
    public static RetrievalDecision withFallback(RetrievalDecision primary, RetrievalDecision fallback) {
        return RetrievalDecision.builder()
                .dataSource(primary.getDataSource())
                .skillName(primary.getSkillName())
                .skillParams(primary.getSkillParams())
                .confidence(primary.getConfidence())
                .reasoning(primary.getReasoning())
                .questionType(primary.getQuestionType())
                .dataRequirement(primary.getDataRequirement())
                .fallbackDecision(fallback)
                .build();
    }
}