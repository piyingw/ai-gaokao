package com.gaokao.ai.agent;

import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;

/**
 * Agent 接口
 * 所有具体 Agent 都需要实现此接口
 */
public interface GaokaoAgent {

    /**
     * 处理用户请求
     *
     * @param request 请求对象
     * @return 响应对象
     */
    AgentResponse handle(AgentRequest request);

    /**
     * 获取 Agent 名称
     *
     * @return Agent 名称
     */
    String getName();

    /**
     * 获取 Agent 描述
     *
     * @return Agent 能力描述
     */
    String getDescription();

    /**
     * 判断是否可以处理该请求
     *
     * @param question 用户问题
     * @return 是否可以处理
     */
    default boolean canHandle(String question) {
        return true;
    }
}