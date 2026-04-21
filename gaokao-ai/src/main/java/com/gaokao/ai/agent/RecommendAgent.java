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
 * 志愿推荐 Agent
 * 根据学生分数、偏好等信息，智能生成志愿填报方案
 * 具备自主数据检索决策能力
 */
@Slf4j
@Component
public class RecommendAgent implements GaokaoAgent {

    private final ChatModel chatModel;
    private final SkillTool skillTool;

    // 增强的关键词匹配库
    private static final List<String> HIGH_CONFIDENCE_KEYWORDS = Arrays.asList(
            "志愿推荐", "填报方案", "冲稳保", "录取概率", "志愿填报",
            "生成志愿", "一键生成", "推荐学校", "推荐专业", "志愿方案"
    );

    private static final List<String> MEDIUM_CONFIDENCE_KEYWORDS = Arrays.asList(
            "推荐", "志愿", "填报", "选择学校", "选择专业", "报考",
            "录取", "分数匹配", "位次匹配", "志愿分配", "填报策略",
            "选校", "择校", "高考志愿", "大学推荐", "院校推荐",
            "能报", "可以报", "能上", "可以上", "录取机会",
            "风险评估", "录取预测", "分数分析", "位次分析"
    );

    private static final List<String> LOW_CONFIDENCE_KEYWORDS = Arrays.asList(
            "分数", "位次", "省控线", "批次线", "投档线",
            "我的情况", "我的成绩", "考生信息", "选什么"
    );

    public RecommendAgent(ChatModel chatModel, SkillTool skillTool) {
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

        // 高置信度关键词直接匹配
        for (String keyword : HIGH_CONFIDENCE_KEYWORDS) {
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

        // 低置信度关键词 + 分数关键词组合匹配
        boolean hasLowKeyword = false;
        boolean hasScoreKeyword = lower.contains("分") || lower.contains("分数");

        for (String keyword : LOW_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword)) {
                hasLowKeyword = true;
                break;
            }
        }

        return hasLowKeyword && hasScoreKeyword;
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

            ## 数据检索决策指南

            你需要自主判断何时使用何种数据源，遵循以下优先级：

            ### 数据源优先级
            1. **本地数据库优先**（university-query-skill, score-analysis-skill, major-query-skill）
               - 当用户查询具体院校、分数线、专业信息时，优先使用本地数据库
               - 参数示例：{"operation": "query", "province": "江苏", "level": "985"}

            2. **网络搜索补充**（web-search-skill）
               - 当本地数据库返回"未找到"时，使用网络搜索获取最新信息
               - 参数示例：{"query": "清华大学2024年录取分数线", "searchType": "university"}

            3. **长期记忆辅助**（searchMemories, getRecentMemories）
               - 在推荐前，先查看用户的历史偏好和重要信息
               - 参数：userId(用户ID), query(搜索内容)

            ### 典型场景数据决策

            | 用户请求 | 推荐技能 | 参数 |
            |---------|---------|------|
            | "600分能报什么学校" | score-analysis-skill | operation: "by-score", score: 600 |
            | "推荐几所985大学" | university-query-skill | operation: "query", level: "985" |
            | "清华北大分数线" | score-analysis-skill | operation: "history", universityName: "清华" |
            | "适合我的专业" | major-query-skill | operation: "recommend-by-personality" |
            | "某院校详细信息" | 先本地查询，若无则web-search | 本地失败时启用网络搜索 |

            ### 检索结果验证循环
            1. 执行技能获取数据
            2. 检查返回结果是否为"未找到"或空
            3. 如果本地数据不足，自动切换到网络搜索
            4. 将检索结果用于分析和推荐

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