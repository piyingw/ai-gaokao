package com.gaokao.ai.agent;

import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.tool.SkillTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 性格分析 Agent
 * 分析学生性格特点，推荐适合的专业方向
 */
@Slf4j
@Component
public class PersonalityAgent implements GaokaoAgent {

    private final ChatLanguageModel chatModel;
    private final SkillTool skillTool;

    public PersonalityAgent(ChatLanguageModel chatModel, SkillTool skillTool) {
        this.chatModel = chatModel;
        this.skillTool = skillTool;
    }

    @Override
    public String getName() {
        return "personality";
    }

    @Override
    public String getDescription() {
        return "性格分析专家：分析学生性格特点，推荐适合的专业方向和职业规划";
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        log.info("PersonalityAgent 处理请求: {}", request.getQuestion());

        try {
            PersonalityAssistant assistant = AiServices.builder(PersonalityAssistant.class)
                    .chatLanguageModel(chatModel)
                    .tools(skillTool)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .systemMessageProvider(memoryId -> SYSTEM_PROMPT)
                    .build();

            String response = assistant.chat(request.getQuestion());

            return AgentResponse.success(getName(), response);

        } catch (Exception e) {
            log.error("性格分析失败", e);
            return AgentResponse.failure(getName(), "性格分析服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public boolean canHandle(String question) {
        if (question == null) return false;
        String lower = question.toLowerCase();
        return lower.contains("性格") || lower.contains("兴趣") || lower.contains("爱好")
                || lower.contains("适合") || lower.contains("职业") || lower.contains("规划")
                || lower.contains("测试") || lower.contains("分析") || lower.contains("mbti")
                || lower.contains("霍兰德");
    }

    /**
     * AI Service 接口
     */
    interface PersonalityAssistant {
        String chat(String message);
    }

    private static final String SYSTEM_PROMPT = """
            你是一位专业的职业规划顾问和性格分析师，擅长帮助学生发现自己的潜能和兴趣。

            你的职责是：
            1. 通过对话了解学生的性格特点、兴趣爱好、学习风格
            2. 基于性格分析推荐适合的专业方向
            3. 提供职业规划建议
            4. 帮助学生认识自己，做出更好的选择

            分析框架：
            1. MBTI性格类型分析
            2. 霍兰德职业兴趣分析（RIASEC）
            3. 学习风格偏好
            4. 价值观和人生目标

            你可以使用以下工具：
            - executeSkill: 通用技能执行工具，通过传入技能名称和参数来执行各种功能
              * major-query-skill: 专业查询技能，支持查询专业信息、详情和按性格推荐（本地数据库）
              * web-search-skill: 网络搜索技能，搜索专业详情、就业前景等最新信息
            - searchMemories: 搜索用户的长期记忆，根据查询内容查找最相关的记忆
            - getRecentMemories: 获取用户最近的重要记忆
            - rememberInfo: 记住用户提供的信息
            - autoExtractAndRemember: 自动提取并记住对话中的关键信息

            回答要求：
            1. 先通过提问了解学生情况，不要急于下结论
            2. 分析要客观、全面，避免刻板印象
            3. 推荐要具体，说明推荐理由
            4. 尊重学生的个人意愿，提供建议而非指令

            请用温暖、鼓励的语气与学生交流，帮助他们发现自己的优势。
            """;
}