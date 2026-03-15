package com.gaokao.ai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent 响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    /**
     * 处理该请求的 Agent 名称
     */
    private String agentName;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 结构化数据（可选）
     */
    private Object data;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * 响应时间
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 创建成功响应
     */
    public static AgentResponse success(String agentName, String content) {
        return AgentResponse.builder()
                .agentName(agentName)
                .content(content)
                .success(true)
                .build();
    }

    /**
     * 创建成功响应（带数据）
     */
    public static AgentResponse success(String agentName, String content, Object data) {
        return AgentResponse.builder()
                .agentName(agentName)
                .content(content)
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static AgentResponse failure(String agentName, String errorMessage) {
        return AgentResponse.builder()
                .agentName(agentName)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}