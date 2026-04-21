package com.gaokao.ai.agent.router;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 意图路由结果
 * 包含路由决策的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentRouteResult {

    /**
     * 目标Agent名称
     */
    private String agent;

    /**
     * 路由置信度 (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * 路由来源: keyword(关键词匹配), llm(LLM识别), merged(融合决策)
     */
    private String source;

    /**
     * 路由理由
     */
    private String reason;

    /**
     * 关键词匹配结果详情
     */
    private KeywordMatchResult keywordResult;

    /**
     * LLM识别结果详情
     */
    private LlmRecognizeResult llmResult;

    /**
     * 所有候选Agent及其得分
     */
    private List<AgentScore> candidateScores;

    /**
     * 是否需要多Agent协作
     */
    @Builder.Default
    private boolean needsCollaboration = false;

    /**
     * 协作Agent列表（如果需要）
     */
    private List<String> collaborationAgents;

    /**
     * 创建成功路由结果
     */
    public static IntentRouteResult of(String agent, Double confidence, String source) {
        return IntentRouteResult.builder()
                .agent(agent)
                .confidence(confidence)
                .source(source)
                .build();
    }

    /**
     * 创建带理由的路由结果
     */
    public static IntentRouteResult of(String agent, Double confidence, String source, String reason) {
        return IntentRouteResult.builder()
                .agent(agent)
                .confidence(confidence)
                .source(source)
                .reason(reason)
                .build();
    }

    /**
     * Agent得分记录
     */
    @Data
    @AllArgsConstructor
    public static class AgentScore {
        private String agentName;
        private double score;
        private String matchedKeywords;
    }

    /**
     * 关键词匹配结果详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordMatchResult {
        private String agent;
        private double score;
        private List<String> matchedKeywords;
        private int matchCount;
    }

    /**
     * LLM识别结果详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmRecognizeResult {
        private String agent;
        private double confidence;
        private String reasoning;
    }
}