package com.gaokao.ai.agent;

import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.tool.SkillTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 学校信息 Agent
 * 提供院校信息查询、对比、分析服务
 * 具备自主数据检索决策能力
 */
@Slf4j
@Component
public class SchoolInfoAgent implements GaokaoAgent {

    private final ChatModel chatModel;
    private final SkillTool skillTool;

    // 增强的关键词匹配库
    private static final List<String> HIGH_CONFIDENCE_KEYWORDS = Arrays.asList(
            "大学信息", "院校信息", "学校详情", "大学详情", "院校详情",
            "学校排名", "大学排名", "院校排名", "清华北大", "985大学",
            "211大学", "双一流", "院校对比", "大学对比", "比较学校"
    );

    private static final List<String> MEDIUM_CONFIDENCE_KEYWORDS = Arrays.asList(
            "大学", "学院", "院校", "学校", "排名", "对比", "比较",
            "区别", "哪个好", "怎么样", "简介", "特色", "优势专业",
            "王牌专业", "分数线", "录取分数", "历年分数", "最低分",
            "最高分", "录取线", "投档线", "学费", "地址", "位置",
            "在哪", "办学性质", "公办", "民办"
    );

    private static final List<String> SPECIFIC_SCHOOL_KEYWORDS = Arrays.asList(
            "清华", "北大", "复旦", "交大", "浙大", "南大", "武大", "华科",
            "中大", "川大", "西交", "哈工", "中科大", "北航", "同济",
            "华东师大", "北师大", "南开", "天大", "东南", "厦大"
    );

    public SchoolInfoAgent(ChatModel chatModel, SkillTool skillTool) {
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
                    .chatModel(chatModel)
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

        // 高置信度关键词直接匹配
        for (String keyword : HIGH_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        // 特定学校名称匹配
        for (String keyword : SPECIFIC_SCHOOL_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        // 中置信度关键词匹配（需要2个以上匹配）
        int mediumCount = 0;
        for (String keyword : MEDIUM_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword)) {
                mediumCount++;
            }
        }
        if (mediumCount >= 2) {
            return true;
        }

        // 分数线关键词 + 院校关键词组合
        boolean hasScoreKeyword = lower.contains("分数") || lower.contains("分数线") || lower.contains("录取");
        boolean hasSchoolKeyword = lower.contains("大学") || lower.contains("院校") || lower.contains("学校");

        return hasScoreKeyword && hasSchoolKeyword;
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

            ## 数据检索决策指南

            你需要自主判断何时使用何种数据源，遵循以下优先级：

            ### 数据源优先级
            1. **本地数据库优先**（university-query-skill, major-query-skill, score-analysis-skill）
               - 查询院校信息：university-query-skill
               - 查询分数线：score-analysis-skill
               - 查询专业信息：major-query-skill

            2. **网络搜索补充**（web-search-skill）
               - 当本地返回"未找到"或需要最新信息时使用
               - 参数示例：{"query": "XX大学2024招生简章", "searchType": "university"}

            ### 典型场景数据决策

            | 用户请求 | 推荐技能 | 参数示例 |
            |---------|---------|---------|
            | "清华大学详情" | university-query-skill | operation: "detail", universityName: "清华" |
            | "985大学名单" | university-query-skill | operation: "query", level: "985" |
            | "清华分数线" | score-analysis-skill | operation: "history", universityName: "清华" |
            | "计算机专业介绍" | major-query-skill | operation: "detail", majorName: "计算机" |
            | "清华北大对比" | university-query-skill | 分别查询两个院校详情 |
            | "某院校最新信息" | 先本地，后web-search | 本地失败时启用网络搜索 |

            ### 检索结果验证
            1. 执行本地数据库查询
            2. 检查返回是否为"未找到"
            3. 如果本地数据不足，使用web-search-skill
            4. 网络搜索结果需注明来源并提醒用户核实

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