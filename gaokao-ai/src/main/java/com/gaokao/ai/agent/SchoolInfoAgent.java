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
 * 学校信息 Agent
 * 提供院校信息查询、对比、分析服务
 */
@Slf4j
@Component
public class SchoolInfoAgent implements GaokaoAgent {

    private final ChatLanguageModel chatModel;
    private final SkillTool skillTool;

    public SchoolInfoAgent(ChatLanguageModel chatModel,
                           SkillTool skillTool) {
        this.chatModel = chatModel;
        this.skillTool = skillTool;
    }

    @Override
    public String getName() {
        return "school";
    }

    @Override
    public String getDescription() {
        return "学校信息专家：查询院校详情、专业信息、院校对比分析";
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        log.info("SchoolInfoAgent 处理请求: {}", request.getQuestion());

        try {
            SchoolAssistant assistant = AiServices.builder(SchoolAssistant.class)
                    .chatLanguageModel(chatModel)
                    .tools(skillTool)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .systemMessageProvider(memoryId -> SYSTEM_PROMPT)
                    .build();

            String response = assistant.chat(request.getQuestion());

            return AgentResponse.success(getName(), response);

        } catch (Exception e) {
            log.error("学校信息查询失败", e);
            return AgentResponse.failure(getName(), "学校信息查询服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public boolean canHandle(String question) {
        if (question == null) return false;
        String lower = question.toLowerCase();
        return lower.contains("大学") || lower.contains("学院") || lower.contains("院校")
                || lower.contains("学校") || lower.contains("专业") || lower.contains("排名")
                || lower.contains("简介") || lower.contains("特色") || lower.contains("对比")
                || lower.contains("比较") || lower.contains("区别");
    }

    /**
     * AI Service 接口
     */
    interface SchoolAssistant {
        String chat(String message);
    }

    private static final String SYSTEM_PROMPT = """
            你是一位专业的院校信息顾问，熟悉全国各类高校的情况。

            你的职责是：
            1. 解答院校基本信息查询，如院校层次、类型、地理位置等
            2. 介绍院校特色专业、优势学科
            3. 提供院校对比分析，帮助考生选择
            4. 解答专业相关问题，如专业介绍、就业方向等
            5. 查询并提供准确的历年分数线信息

            你可以使用以下工具：
            - executeSkill: 通用技能执行工具，通过传入技能名称和参数来执行各种功能
              * university-query-skill: 院校查询技能，支持查询院校列表、详情和搜索（本地数据库）
              * major-query-skill: 专业查询技能，支持查询专业信息、详情和按性格推荐（本地数据库）
              * score-analysis-skill: 分数线分析技能，专门用于查询院校历年分数线数据
              * web-search-skill: 网络搜索技能，当本地数据库没有相关信息时联网搜索
                参数: query(搜索关键词), searchType(university/major/general)
            - validateUniversityExists: 验证院校信息是否存在
            - validateScoreExists: 验证分数线数据是否存在

            重要约束：
            - 首先尝试使用本地数据库技能(university-query-skill)查询院校信息
            - 如果本地数据库返回"未找到"、"不存在"等结果，则使用web-search-skill联网搜索
            - 当用户询问分数线、录取分数、往年分数等相关问题时，必须使用score-analysis-skill工具查询
            - 绝不允许编造或猜测分数线数据，必须通过工具获取真实数据
            - 如果所有工具都无法获取数据，如实告知用户并建议访问官方渠道查询
            - 使用web-search-skill时，searchType参数: university用于院校搜索，major用于专业搜索

            回答要求：
            1. 优先使用本地数据库工具查询，如果找不到再使用网络搜索
            2. 使用网络搜索获取的信息，注明来源并提醒用户核实
            3. 提供全面、准确的院校信息，不编造数据
            4. 对比分析时客观公正，列出优缺点
            5. 根据用户需求推荐合适的院校或专业
            6. 如果网络搜索失败，提供官方渠道供用户自行查询

            请用专业、热情的语气为用户服务。
            """;
}