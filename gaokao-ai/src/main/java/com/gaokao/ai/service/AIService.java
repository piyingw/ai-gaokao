package com.gaokao.ai.service;

import com.gaokao.ai.agent.GaokaoOrchestratorAgent;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.dto.OneClickRecommendDTO;
import com.gaokao.ai.dto.ChatRequestDTO;
import com.gaokao.ai.vo.ChatResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 服务门面
 * 统一对外提供AI服务接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final GaokaoOrchestratorAgent orchestratorAgent;

    /**
     * 智能对话
     */
    public ChatResponseVO chat(String userId, ChatRequestDTO request) {
        log.info("AI对话: userId={}, message={}", userId, request.getMessage());

        AgentResponse response = orchestratorAgent.chat(
                userId,
                request.getSessionId(),
                request.getMessage()
        );

        return ChatResponseVO.builder()
                .sessionId(request.getSessionId())
                .content(response.getContent())
                .agentName(response.getAgentName())
                .success(response.isSuccess())
                .errorMessage(response.getErrorMessage())
                .build();
    }

    /**
     * 一键生成志愿
     */
    public AgentResponse oneClickRecommend(String userId, OneClickRecommendDTO dto) {
        log.info("一键生成志愿: userId={}, score={}", userId, dto.getScore());

        return orchestratorAgent.oneClickRecommend(
                userId,
                dto.getSessionId(),
                dto.getScore(),
                dto.getProvince(),
                dto.getSubjectType(),
                dto.getPersonalityDescription()
        );
    }

    /**
     * 志愿推荐
     */
    public AgentResponse recommend(String userId, String question) {
        log.info("志愿推荐: userId={}", userId);

        return orchestratorAgent.process(userId, null, question);
    }

    /**
     * 政策问答
     */
    public AgentResponse policyQA(String userId, String question) {
        log.info("政策问答: userId={}", userId);

        return orchestratorAgent.process(userId, null, question);
    }

    /**
     * 学校信息查询
     */
    public AgentResponse schoolInfo(String userId, String question) {
        log.info("学校信息查询: userId={}", userId);

        return orchestratorAgent.process(userId, null, question);
    }

    /**
     * 性格分析
     */
    public AgentResponse personalityAnalysis(String userId, String description) {
        log.info("性格分析: userId={}", userId);

        return orchestratorAgent.process(userId, null, 
                "请分析我的性格特点并推荐适合的专业：" + description);
    }
}