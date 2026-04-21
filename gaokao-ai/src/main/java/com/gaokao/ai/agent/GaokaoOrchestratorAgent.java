package com.gaokao.ai.agent;

import com.gaokao.ai.agent.decision.AgentContext;
import com.gaokao.ai.agent.decision.DataRetrievalDecider;
import com.gaokao.ai.agent.decision.RetrievalDecision;
import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.agent.router.IntentRouter;
import com.gaokao.ai.agent.router.IntentRouteResult;
import com.gaokao.ai.entity.LongTermMemory;
import com.gaokao.ai.service.LongTermMemoryService;
import com.gaokao.ai.skill.SkillExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主协调 Agent
 * 负责理解用户意图，路由到对应的子 Agent 处理
 * 支持关键词匹配 + LLM意图识别双重路由
 * 支持多Agent协作处理复杂请求
 */
@Slf4j
@Component
public class GaokaoOrchestratorAgent {

    private final SkillExecutor skillExecutor;
    private final LongTermMemoryService longTermMemoryService;
    private final IntentRouter intentRouter;
    private final DataRetrievalDecider dataRetrievalDecider;
    private final Map<String, GaokaoAgent> agents;

    public GaokaoOrchestratorAgent(SkillExecutor skillExecutor,
                                   LongTermMemoryService longTermMemoryService,
                                   IntentRouter intentRouter,
                                   DataRetrievalDecider dataRetrievalDecider,
                                   RecommendAgent recommendAgent,
                                   PolicyAgent policyAgent,
                                   SchoolInfoAgent schoolInfoAgent,
                                   PersonalityAgent personalityAgent) {
        this.skillExecutor = skillExecutor;
        this.longTermMemoryService = longTermMemoryService;
        this.intentRouter = intentRouter;
        this.dataRetrievalDecider = dataRetrievalDecider;
        this.agents = new HashMap<>();
        this.agents.put("recommend", recommendAgent);
        this.agents.put("policy", policyAgent);
        this.agents.put("school", schoolInfoAgent);
        this.agents.put("personality", personalityAgent);
    }

    /**
     * 处理用户请求 - 使用增强的意图路由
     */
    public AgentResponse process(String userId, String sessionId, String question) {
        log.info("OrchestratorAgent 处理请求：userId={}, question={}", userId, question);

        try {
            // 1. 检索用户的长期记忆
            String longTermContext = retrieveLongTermMemoryContext(userId, question);

            // 2. 使用IntentRouter进行智能路由
            IntentRouteResult routeResult = intentRouter.route(question);
            log.info("意图路由结果：agent={}, confidence={}, source={}",
                    routeResult.getAgent(), routeResult.getConfidence(), routeResult.getSource());

            // 3. 检查是否需要多Agent协作
            IntentRouteResult collaborationResult = intentRouter.checkCollaboration(question);
            if (collaborationResult != null && collaborationResult.isNeedsCollaboration()) {
                log.info("需要多Agent协作: agents={}", collaborationResult.getCollaborationAgents());
                return handleCollaboration(userId, sessionId, question, collaborationResult, longTermContext);
            }

            // 4. 获取目标 Agent
            GaokaoAgent targetAgent = agents.get(routeResult.getAgent());
            if (targetAgent == null) {
                targetAgent = agents.get("recommend"); // 默认使用推荐 Agent
            }

            // 5. 构建Agent上下文
            AgentContext context = AgentContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .currentQuestion(question)
                    .longTermMemoryContext(longTermContext)
                    .build();

            // 6. 执行自主检索决策
            RetrievalDecision retrievalDecision = dataRetrievalDecider.decide(question, context);
            log.debug("检索决策: dataSource={}, skillName={}",
                    retrievalDecision.getDataSource(), retrievalDecision.getSkillName());

            // 7. 构建请求并执行
            AgentRequest request = AgentRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .question(question)
                    .context(longTermContext)
                    .parameters(buildRequestParameters(routeResult, retrievalDecision))
                    .build();

            // 8. 自动提取关键信息并存储到长期记忆
            longTermMemoryService.autoExtractAndRemember(userId, question);

            return targetAgent.handle(request);

        } catch (Exception e) {
            log.error("处理请求失败", e);
            return AgentResponse.failure("orchestrator", "服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 处理多Agent协作请求
     */
    private AgentResponse handleCollaboration(String userId, String sessionId, String question,
                                              IntentRouteResult collaborationResult, String longTermContext) {
        List<String> agentNames = collaborationResult.getCollaborationAgents();
        if (agentNames == null || agentNames.isEmpty()) {
            return process(userId, sessionId, question); // 无协作需求，正常处理
        }

        // 收集各Agent的处理结果
        List<AgentResponse> responses = new ArrayList<>();
        StringBuilder combinedContent = new StringBuilder();

        for (String agentName : agentNames) {
            GaokaoAgent agent = agents.get(agentName);
            if (agent == null) continue;

            AgentRequest request = AgentRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .question(question)
                    .context(longTermContext)
                    .build();

            try {
                AgentResponse response = agent.handle(request);
                responses.add(response);
                combinedContent.append("【").append(agent.getDescription()).append("】\n");
                combinedContent.append(response.getContent()).append("\n\n");
            } catch (Exception e) {
                log.error("Agent {} 处理失败", agentName, e);
            }
        }

        if (responses.isEmpty()) {
            return AgentResponse.failure("orchestrator", "协作处理失败");
        }

        // 返回综合结果
        return AgentResponse.builder()
                .agentName("orchestrator-collaboration")
                .content(combinedContent.toString())
                .success(true)
                .metadata(Map.of("collaborationAgents", agentNames))
                .build();
    }

    /**
     * 检索用户的长期记忆上下文
     */
    private String retrieveLongTermMemoryContext(String userId, String question) {
        try {
            List<LongTermMemory> memories = longTermMemoryService.searchMemories(userId, question, 5);

            if (memories.isEmpty()) {
                memories = longTermMemoryService.getRecentMemories(userId, 3);
            }

            if (memories.isEmpty()) {
                return "";
            }

            return "用户长期记忆信息：\n" +
                   memories.stream()
                          .map(mem -> "- [" + mem.getType() + "] " + mem.getContent())
                          .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("检索长期记忆失败：{}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 一键生成志愿（基于性格分析）
     */
    public AgentResponse oneClickRecommend(String userId, String sessionId,
                                           Integer score, String province, String subjectType,
                                           String personalityDescription) {
        log.info("一键生成志愿：userId={}, score={}, province={}", userId, score, province);

        try {
            // 构建用户信息上下文
            AgentContext.UserInfo userInfo = AgentContext.UserInfo.builder()
                    .score(score)
                    .province(province)
                    .subjectType(subjectType)
                    .build();

            AgentContext context = AgentContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .userInfo(userInfo)
                    .build();

            // 1. 先进行性格分析
            PersonalityAgent personalityAgent = (PersonalityAgent) agents.get("personality");
            AgentRequest personalityRequest = AgentRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .question("请分析我的性格特点并推荐适合的专业：" + personalityDescription)
                    .build();
            AgentResponse personalityResponse = personalityAgent.handle(personalityRequest);

            // 2. 结合性格分析结果进行志愿推荐
            RecommendAgent recommendAgent = (RecommendAgent) agents.get("recommend");
            String recommendQuestion = String.format(
                    "我是%s省%s考生，高考分数%d分。%s\n\n请根据我的情况推荐合适的院校和专业。",
                    province, subjectType, score, personalityResponse.getContent()
            );

            // 执行检索决策
            RetrievalDecision decision = dataRetrievalDecider.decide(recommendQuestion, context);

            AgentRequest recommendRequest = AgentRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .question(recommendQuestion)
                    .context(personalityResponse.getContent())
                    .parameters(buildRequestParameters(null, decision))
                    .build();

            return recommendAgent.handle(recommendRequest);

        } catch (Exception e) {
            log.error("一键生成志愿失败", e);
            return AgentResponse.failure("orchestrator", "一键生成志愿服务暂时不可用");
        }
    }

    /**
     * 多轮对话
     */
    public AgentResponse chat(String userId, String sessionId, String message) {
        log.info("多轮对话：userId={}, sessionId={}", userId, sessionId);
        return process(userId, sessionId, message);
    }

    /**
     * 构建请求参数
     */
    private Map<String, Object> buildRequestParameters(IntentRouteResult routeResult,
                                                       RetrievalDecision retrievalDecision) {
        Map<String, Object> params = new HashMap<>();

        if (routeResult != null) {
            params.put("routeConfidence", routeResult.getConfidence());
            params.put("routeSource", routeResult.getSource());
        }

        if (retrievalDecision != null) {
            params.put("dataSource", retrievalDecision.getDataSource().name());
            params.put("recommendedSkill", retrievalDecision.getSkillName());
            params.put("skillParams", retrievalDecision.getSkillParams());
            params.put("questionType", retrievalDecision.getQuestionType());
        }

        return params;
    }

    /**
     * 直接执行指定技能
     */
    public AgentResponse executeSkill(String skillName, Map<String, Object> params) {
        log.info("执行技能：name={}, params={}", skillName, params);

        try {
            Object result = skillExecutor.executeSkill(skillName, params);
            return AgentResponse.success("skill-executor", result != null ? result.toString() : "技能执行返回空结果");
        } catch (Exception e) {
            log.error("技能调用失败：" + skillName, e);
            return AgentResponse.failure("skill-executor", "技能执行失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有 Agent 信息
     */
    public List<AgentInfo> getAgentInfos() {
        return agents.entrySet().stream()
                .map(e -> new AgentInfo(e.getKey(), e.getValue().getDescription()))
                .toList();
    }

    /**
     * 获取路由统计信息
     */
    public Map<String, Object> getRouterStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("keywordStats", intentRouter.getRouterStats());
        stats.put("availableAgents", agents.keySet());
        return stats;
    }

    public record AgentInfo(String name, String description) {}
}