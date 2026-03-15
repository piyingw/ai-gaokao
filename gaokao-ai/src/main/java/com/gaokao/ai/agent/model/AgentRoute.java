package com.gaokao.ai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 路由结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRoute {

    /**
     * 目标 Agent 名称
     */
    private String agent;

    /**
     * 路由置信度 (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * 路由理由
     */
    private String reason;

    /**
     * 创建路由
     */
    public static AgentRoute of(String agent, Double confidence) {
        return AgentRoute.builder()
                .agent(agent)
                .confidence(confidence)
                .build();
    }
}