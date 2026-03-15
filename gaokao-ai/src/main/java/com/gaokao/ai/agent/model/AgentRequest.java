package com.gaokao.ai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class
AgentRequest {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID（用于多轮对话）
     */
    private String sessionId;

    /**
     * 用户问题/输入
     */
    private String question;

    /**
     * 上下文信息（可选）
     */
    private String context;

    /**
     * 额外参数
     */
    private Map<String, Object> parameters;

    /**
     * 创建简单请求
     */
    public static AgentRequest of(String question) {
        return AgentRequest.builder()
                .question(question)
                .build();
    }

    /**
     * 创建带用户ID的请求
     */
    public static AgentRequest of(String userId, String question) {
        return AgentRequest.builder()
                .userId(userId)
                .question(question)
                .build();
    }
}