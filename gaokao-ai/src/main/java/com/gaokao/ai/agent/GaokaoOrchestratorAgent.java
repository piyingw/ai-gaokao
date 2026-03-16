package com.gaokao.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.agent.model.AgentRoute;
import com.gaokao.ai.entity.LongTermMemory;
import com.gaokao.ai.service.LongTermMemoryService;
import com.gaokao.ai.skill.SkillExecutor;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主协调 Agent
 * 负责理解用户意图，路由到对应的子 Agent 处理
 */
@Slf4j
@Component
public class GaokaoOrchestratorAgent {

    private final ChatLanguageModel chatModel;
    private final SkillExecutor skillExecutor;
    private final LongTermMemoryService longTermMemoryService;
    private final Map<String, GaokaoAgent> agents;

    public GaokaoOrchestratorAgent(ChatLanguageModel chatModel,
                                   SkillExecutor skillExecutor,
                                   LongTermMemoryService longTermMemoryService,
                                   RecommendAgent recommendAgent,
                                   PolicyAgent policyAgent,
                                   SchoolInfoAgent schoolInfoAgent,
                                   PersonalityAgent personalityAgent) {
        this.chatModel = chatModel;
        this.skillExecutor = skillExecutor;
        this.longTermMemoryService = longTermMemoryService;
        this.agents = new HashMap<>();
        this.agents.put("recommend", recommendAgent);
        this.agents.put("policy", policyAgent);
        this.agents.put("school", schoolInfoAgent);
        this.agents.put("personality", personalityAgent);
    }

    /**
     * 处理用户请求
     */
    public AgentResponse process(String userId, String sessionId, String question) {
        log.info("OrchestratorAgent 处理请求：userId={}, question={}", userId, question);

        try {
            // 1. 检索用户的长期记忆
            String longTermContext = retrieveLongTermMemoryContext(userId, question);

            // 2. 路由判断
            AgentRoute route = route(question);
            log.info("路由结果：agent={}, confidence={}", route.getAgent(), route.getConfidence());

            // 3. 获取目标 Agent
            GaokaoAgent targetAgent = agents.get(route.getAgent());
            if (targetAgent == null) {
                targetAgent = agents.get("recommend"); // 默认使用推荐 Agent
            }

            // 4. 构建请求并执行
            AgentRequest request = AgentRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .question(question)
                    .context(longTermContext) // 添加长期记忆上下文
                    .build();

            // 5. 自动提取关键信息并存储到长期记忆
            longTermMemoryService.autoExtractAndRemember(userId, question);

            return targetAgent.handle(request);

        } catch (Exception e) {
            log.error("处理请求失败", e);
            return AgentResponse.failure("orchestrator", "服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 检索用户的长期记忆上下文
     * 使用语义搜索，根据问题内容查找最相关的记忆
     */
    private String retrieveLongTermMemoryContext(String userId, String question) {
        try {
            // 使用语义搜索查找与问题最相关的记忆
            List<LongTermMemory> memories = longTermMemoryService.searchMemories(userId, question, 5);

            if (memories.isEmpty()) {
                // 如果没有语义相关的记忆，尝试获取最近的重要记忆
                memories = longTermMemoryService.getRecentMemories(userId, 3);
            }

            if (memories.isEmpty()) {
                return "";
            }

            // 将记忆转换为文本上下文
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
            AgentRequest recommendRequest = AgentRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .question(recommendQuestion)
                    .context(personalityResponse.getContent())
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

        // 简单实现：直接路由处理
        return process(userId, sessionId, message);
    }

    /**
     * 路由判断 - 使用 LLM 意图识别 + 关键词匹配双重保障
     */
    private AgentRoute route(String question) {
        // 1. 首先尝试关键词快速匹配（高效兜底）
        AgentRoute keywordRoute = simpleRoute(question);
        if (keywordRoute.getConfidence() >= 0.8) {
            log.debug("关键词匹配成功: agent={}, confidence={}", keywordRoute.getAgent(), keywordRoute.getConfidence());
            return keywordRoute;
        }

        // 2. 关键词匹配置信度不足时，使用 LLM 意图识别
        try {
            AgentRoute llmRoute = routeWithLLM(question);
            if (llmRoute != null && llmRoute.getConfidence() > keywordRoute.getConfidence()) {
                log.info("LLM路由成功: agent={}, confidence={}", llmRoute.getAgent(), llmRoute.getConfidence());
                return llmRoute;
            }
        } catch (Exception e) {
            log.warn("LLM路由失败，使用关键词路由: {}", e.getMessage());
        }

        return keywordRoute;
    }

    /**
     * 使用 LLM 进行意图识别路由
     */
    private AgentRoute routeWithLLM(String question) {
        try {
            // 使用 AiServices 创建路由助手
            RouterAssistant routerAssistant = AiServices.builder(RouterAssistant.class)
                    .chatLanguageModel(chatModel)
                    .systemMessageProvider(id -> ROUTER_SYSTEM_PROMPT)
                    .build();

            String responseText = routerAssistant.route(question);
            log.debug("LLM路由响应: {}", responseText);

            // 解析 JSON 结果
            JSONObject json = parseJson(responseText);
            if (json != null) {
                String agent = json.getString("agent");
                Double confidence = json.getDouble("confidence");

                // 验证 agent 是否有效
                if (agents.containsKey(agent) && confidence != null) {
                    return AgentRoute.of(agent, confidence);
                }
            }
        } catch (Exception e) {
            log.error("LLM路由解析异常: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 路由助手接口
     */
    interface RouterAssistant {
        String route(String question);
    }

    private static final String ROUTER_SYSTEM_PROMPT = """
            你是高考志愿填报系统的智能路由助手。根据用户的问题，判断应该由哪个 Agent 处理。

            可用的 Agent：
            1. recommend - 志愿推荐：根据分数、偏好生成志愿方案，录取概率分析
            2. school - 学校信息：查询学校详情、专业信息、院校对比
            3. policy - 政策问答：解答高考政策、录取规则、志愿填报政策
            4. personality - 性格分析：分析学生性格，推荐适合的专业方向

            请以 JSON 格式返回（只返回 JSON，不要其他内容）：
            {"agent": "agent名称", "confidence": 0.0-1.0之间的数值}
            """;

    /**
     * 简单路由（基于关键词）
     */
    private AgentRoute simpleRoute(String question) {
        String lower = question.toLowerCase();

        for (Map.Entry<String, GaokaoAgent> entry : agents.entrySet()) {
            if (entry.getValue().canHandle(question)) {
                return AgentRoute.of(entry.getKey(), 0.7);
            }
        }

        // 默认路由到推荐 Agent
        return AgentRoute.of("recommend", 0.5);
    }

    /**
     * 解析 JSON
     */
    private JSONObject parseJson(String text) {
        try {
            // 尝试提取 JSON 部分
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return JSON.parseObject(text.substring(start, end + 1));
            }
        } catch (Exception e) {
            log.debug("JSON 解析失败：{}", text);
        }
        return null;
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

    public record AgentInfo(String name, String description) {}
}
