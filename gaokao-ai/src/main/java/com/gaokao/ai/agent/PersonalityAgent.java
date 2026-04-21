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
 * 性格分析 Agent
 * 分析学生性格特点，推荐适合的专业方向
 * 具备自主数据检索决策能力
 */
@Slf4j
@Component
public class PersonalityAgent implements GaokaoAgent {

    private final ChatModel chatModel;
    private final SkillTool skillTool;

    // 增强的关键词匹配库
    private static final List<String> HIGH_CONFIDENCE_KEYWORDS = Arrays.asList(
            "性格测试", "MBTI测试", "霍兰德测试", "职业兴趣测试",
            "性格分析", "兴趣测试", "职业规划", "专业匹配",
            "性格匹配", "兴趣匹配", "职业倾向"
    );

    private static final List<String> MEDIUM_CONFIDENCE_KEYWORDS = Arrays.asList(
            "性格", "兴趣", "爱好", "适合", "职业", "规划",
            "测试", "分析", "mbti", "霍兰德", "riasec",
            "职业兴趣", "专业方向", "职业方向", "我适合",
            "适合什么", "适合做什么", "发展方向", "就业前景",
            "未来发展", "人生规划", "职业选择", "专业选择"
    );

    private static final List<String> PERSONALITY_TYPE_KEYWORDS = Arrays.asList(
            "内向", "外向", "理性", "感性", "严谨", "开放",
            "研究型", "艺术型", "社会型", "企业型", "常规型", "现实型",
            "i", "e", "s", "n", "t", "f", "j", "p"
    );

    public PersonalityAgent(ChatModel chatModel, SkillTool skillTool) {
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
                    .chatModel(chatModel)
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

        // 高置信度关键词直接匹配
        for (String keyword : HIGH_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // 性格类型关键词匹配
        for (String keyword : PERSONALITY_TYPE_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // 中置信度关键词 + 问题关键词组合
        boolean hasMediumKeyword = false;
        for (String keyword : MEDIUM_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                hasMediumKeyword = true;
                break;
            }
        }

        // "适合"关键词 + 专业/职业关键词组合
        boolean hasSuitKeyword = lower.contains("适合");
        boolean hasTargetKeyword = lower.contains("专业") || lower.contains("职业") || lower.contains("工作");

        if (hasSuitKeyword && hasTargetKeyword) {
            return true;
        }

        // 中置信度关键词匹配（需要2个以上）
        int mediumCount = 0;
        for (String keyword : MEDIUM_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                mediumCount++;
            }
        }

        return mediumCount >= 2 || hasMediumKeyword;
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

            ## 分析框架

            ### MBTI性格类型分析
            - ISTJ：适合会计、工程师、公务员等严谨工作
            - ISFJ：适合医护、教师、社工等服务性工作
            - INFJ：适合心理咨询、写作、人力资源等工作
            - INTJ：适合科研、程序员、分析师等智力工作
            - ISTP：适合技师、程序员、运动员等实操工作
            - ISFP：适合艺术、设计、摄影等创意工作
            - INFP：适合作家、心理咨询、NGO等理想主义工作
            - INTP：适合科研、程序员、哲学家等探索性工作
            - ESTP：适合销售、运动员、企业家等冒险性工作
            - ESFP：适合演员、导游、公关等表演性工作
            - ENFP：适合记者、培训师、创业者等激励性工作
            - ENTP：适合咨询、投资、发明家等创新型工作
            - ESTJ：适合管理、法官、军官等领导性工作
            - ESFJ：适合教师、护士、HR等协调性工作
            - ENFJ：适合教师、培训师、公关等引导性工作
            - ENTJ：适合CEO、律师、顾问等战略性工作

            ### 霍兰德职业兴趣分析（RIASEC）
            - R(现实型)：适合技工、工程师、运动员
            - I(研究型)：适合科学家、程序员、分析师
            - A(艺术型)：适合艺术家、设计师、作家
            - S(社会型)：适合教师、医护、咨询师
            - E(企业型)：适合销售、管理、创业者
            - C(常规型)：适合会计、秘书、管理员

            ## 数据检索决策指南

            ### 数据源优先级
            1. **本地数据库优先**（major-query-skill）
               - 查询专业信息、就业方向
               - 按性格推荐专业：operation: "recommend-by-personality"

            2. **长期记忆辅助**
               - 查看用户历史偏好、兴趣记录
               - 使用searchMemories搜索相关记忆

            3. **网络搜索补充**（web-search-skill）
               - 查询最新就业趋势、职业前景
               - 参数示例：{"query": "XX专业就业前景2024", "searchType": "major"}

            ### 典型场景决策
            | 用户请求 | 推荐技能 |
            |---------|---------|
            | "推荐适合内向的专业" | major-query-skill + 性格分析 |
            | "XX专业就业前景" | major-query-skill(本地) + web-search(补充) |
            | "分析我的性格" | MBTI/霍兰德框架分析 |

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
            5. 使用工具查询专业数据，不要编造信息
            6. 记住用户的性格特点，便于后续推荐

            请用温暖、鼓励的语气与学生交流，帮助他们发现自己的优势。
            """;
}