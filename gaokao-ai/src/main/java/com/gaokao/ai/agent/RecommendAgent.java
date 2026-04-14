package com.gaokao.ai.agent;

import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.tool.SkillTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 志愿推荐 Agent
 * 根据学生分数、偏好等信息，智能生成志愿填报方案
 */
@Slf4j
@Component
public class RecommendAgent implements GaokaoAgent {

    private final ChatModel chatModel;
    private final SkillTool skillTool;

    public RecommendAgent(ChatModel chatModel,
                          SkillTool skillTool) {
        this.chatModel = chatModel;
        this.skillTool = skillTool;
    }

    @Override
    public String getName() {
        return "recommend";
    }

    @Override
    public String getDescription() {
        return "志愿推荐专家：根据分数、位次、偏好等信息，智能生成志愿填报方案";
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        log.info("RecommendAgent 处理请求: {}", request.getQuestion());

        try {
            // 构建 AI Service
            RecommendAssistant assistant = AiServices.builder(RecommendAssistant.class)
                    .chatModel(chatModel)
                    .tools(skillTool)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .systemMessageProvider(memoryId -> SYSTEM_PROMPT)
                    .build();

            String response = assistant.chat(request.getQuestion());

            return AgentResponse.success(getName(), response);

        } catch (Exception e) {
            log.error("志愿推荐失败", e);
            return AgentResponse.failure(getName(), "志愿推荐服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public boolean canHandle(String question) {
        if (question == null) return false;
        String lower = question.toLowerCase();
        return lower.contains("推荐") || lower.contains("志愿") || lower.contains("填报")
                || lower.contains("选择") || lower.contains("学校") || lower.contains("专业")
                || lower.contains("录取") || lower.contains("分数");
    }

    /**
     * AI Service 接口
     */
    interface RecommendAssistant {
        String chat(String message);
    }

    private static final String SYSTEM_PROMPT = """
            你是一位资深高考志愿填报专家，拥有20年志愿填报经验，精通全国各省高考政策和录取规则。

            你的职责是：
            1. 根据学生的分数、位次、省份、科类，筛选合适的院校
            2. 结合学生的性格特点、兴趣爱好，推荐适合的专业方向
            3. 按照冲稳保策略，生成科学合理的志愿方案
            4. 为每个志愿提供详细的推荐理由和录取概率分析

            你可以使用以下工具：
            - executeSkill: 通用技能执行工具，通过传入技能名称和参数来执行各种功能
              * university-query-skill: 院校查询技能，支持按省份、层次、类型筛选院校（本地数据库）
              * score-analysis-skill: 分数线分析技能，支持查询历史分数线、分数匹配院校、竞争力分析
              * calculator-skill: 计算技能，支持录取概率计算、志愿分配方案、位次换算、性价比评分
              * major-query-skill: 专业查询技能，支持查询专业信息、推荐适合的专业（本地数据库）
              * web-search-skill: 网络搜索技能，当本地数据库无数据时联网搜索院校/专业信息

            重要约束：
            - 优先使用本地数据库技能查询数据
            - 如果本地数据库返回"未找到"，则使用 web-search-skill 联网搜索
            - 不要编造任何数据，必须通过工具获取真实信息
            - searchMemories: 搜索用户的长期记忆，根据查询内容查找最相关的记忆
            - getMemoriesByTag: 按标签检索用户的长期记忆（标签：score, location, subject）
            - getRecentMemories: 获取用户最近的重要记忆
            - rememberInfo: 记住用户提供的信息
            - autoExtractAndRemember: 自动提取并记住对话中的关键信息

            回答要求：
            1. 先了解学生的基本情况（分数、省份、科类）
            2. 主动询问学生的偏好（专业方向、城市偏好、院校层次等）
            3. 使用工具查询数据，不要编造信息
            4. 给出具体的院校推荐和理由
            5. 按冲稳保分类整理推荐结果
            6. 在推荐前，可以使用 searchMemories 查看用户的历史偏好和重要信息

            请用专业、亲切的语气与学生交流，帮助他们做出最佳选择。
            """;
}