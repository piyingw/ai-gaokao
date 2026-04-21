package com.gaokao.ai.agent.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent上下文
 * 包含Agent执行时的环境和状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 当前问题
     */
    private String currentQuestion;

    /**
     * 用户长期记忆上下文
     */
    private String longTermMemoryContext;

    /**
     * 用户基本信息
     */
    private UserInfo userInfo;

    /**
     * 上一轮对话结果
     */
    private String previousResult;

    /**
     * 已尝试的数据源
     */
    @Builder.Default
    private Map<String, Boolean> attemptedDataSources = new HashMap<>();

    /**
     * 当前轮次
     */
    @Builder.Default
    private int round = 0;

    /**
     * 最大轮次限制
     */
    @Builder.Default
    private int maxRounds = 3;

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 用户基本信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Integer score;
        private String province;
        private String subjectType;
        private String personalityType;
        private String interests;
    }

    /**
     * 标记已尝试某个数据源
     */
    public void markDataSourceAttempted(String dataSource) {
        attemptedDataSources.put(dataSource, true);
    }

    /**
     * 检查是否已尝试某个数据源
     */
    public boolean hasAttemptedDataSource(String dataSource) {
        return attemptedDataSources.containsKey(dataSource);
    }

    /**
     * 检查是否还能继续尝试
     */
    public boolean canContinue() {
        return round < maxRounds;
    }

    /**
     * 增加轮次
     */
    public void incrementRound() {
        round++;
    }

    /**
     * 设置属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 创建简单上下文
     */
    public static AgentContext of(String userId, String question) {
        return AgentContext.builder()
                .userId(userId)
                .currentQuestion(question)
                .build();
    }

    /**
     * 创建带用户信息的上下文
     */
    public static AgentContext of(String userId, String question, UserInfo userInfo) {
        return AgentContext.builder()
                .userId(userId)
                .currentQuestion(question)
                .userInfo(userInfo)
                .build();
    }
}