package com.gaokao.ai.agent.decision;

import com.gaokao.ai.skill.SkillExecutor;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据检索决策器
 * Agent自主决定数据获取策略的核心组件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetrievalDecider {

    private final ChatModel chatModel;
    private final SkillExecutor skillExecutor;

    /**
     * 执行检索决策
     * 根据问题和上下文，自主决定数据获取策略
     */
    public RetrievalDecision decide(String question, AgentContext context) {
        log.info("开始数据检索决策: question={}", question);

        // 1. 分析问题类型
        RetrievalDecision.QuestionType questionType = analyzeQuestionType(question);
        log.debug("问题类型分析: {}", questionType);

        // 2. 评估数据需求
        RetrievalDecision.DataRequirement requirement = assessDataRequirement(questionType, question, context);
        log.debug("数据需求评估: needsUniversity={}, needsScore={}, needsWeb={}",
                requirement.isNeedsUniversityData(), requirement.isNeedsScoreData(), requirement.isNeedsWebData());

        // 3. 选择数据源和技能
        RetrievalDecision decision = selectDataSource(questionType, requirement, question, context);
        log.info("检索决策结果: dataSource={}, skillName={}", decision.getDataSource(), decision.getSkillName());

        // 4. 设置备选方案
        if (decision.getDataSource() != RetrievalDecision.DataSource.NONE &&
            decision.getDataSource() != RetrievalDecision.DataSource.WEB_SEARCH) {
            RetrievalDecision fallback = createFallbackDecision(decision, question);
            decision.setFallbackDecision(fallback);
        }

        decision.setQuestionType(questionType);
        decision.setDataRequirement(requirement);

        return decision;
    }

    /**
     * 分析问题类型
     */
    private RetrievalDecision.QuestionType analyzeQuestionType(String question) {
        String lower = question.toLowerCase();

        // 分数线相关
        if (containsAny(lower, "分数线", "录取分数", "最低分", "最高分", "多少分", "分数要求")) {
            return RetrievalDecision.QuestionType.SCORE_QUERY;
        }

        // 院校查询
        if (containsAny(lower, "大学", "学院", "院校", "学校", "排名", "层次")) {
            return RetrievalDecision.QuestionType.UNIVERSITY_QUERY;
        }

        // 专业查询
        if (containsAny(lower, "专业", "学科", "就业方向", "课程")) {
            return RetrievalDecision.QuestionType.MAJOR_QUERY;
        }

        // 政策查询
        if (containsAny(lower, "政策", "规则", "批次", "志愿规则", "录取规则", "平行志愿", "顺序志愿")) {
            return RetrievalDecision.QuestionType.POLICY_QUERY;
        }

        // 推荐请求
        if (containsAny(lower, "推荐", "志愿方案", "填报方案", "冲稳保", "录取概率")) {
            return RetrievalDecision.QuestionType.RECOMMEND_REQUEST;
        }

        // 性格分析
        if (containsAny(lower, "性格", "兴趣", "职业规划", "mbti", "霍兰德", "适合")) {
            return RetrievalDecision.QuestionType.PERSONALITY_ANALYSIS;
        }

        // 对比分析
        if (containsAny(lower, "对比", "比较", "区别", "哪个好", "vs", "versus")) {
            return RetrievalDecision.QuestionType.COMPARISON;
        }

        return RetrievalDecision.QuestionType.GENERAL_INFO;
    }

    /**
     * 评估数据需求
     */
    private RetrievalDecision.DataRequirement assessDataRequirement(
            RetrievalDecision.QuestionType questionType, String question, AgentContext context) {

        RetrievalDecision.DataRequirement.DataRequirementBuilder builder = RetrievalDecision.DataRequirement.builder();

        switch (questionType) {
            case SCORE_QUERY:
                builder.needsScoreData(true)
                       .needsUniversityData(true)
                       .urgency("high")
                       .specificity("specific");
                break;

            case UNIVERSITY_QUERY:
                builder.needsUniversityData(true)
                       .needsScoreData(false)
                       .urgency("medium")
                       .specificity("specific");
                // 查询具体院校时可能需要网络搜索补充
                if (containsAny(question.toLowerCase(), "简介", "特色", "详细", "详细介绍")) {
                    builder.needsWebData(true);
                }
                break;

            case MAJOR_QUERY:
                builder.needsMajorData(true)
                       .urgency("medium")
                       .specificity("general");
                break;

            case POLICY_QUERY:
                builder.needsPolicyData(true)
                       .urgency("medium")
                       .specificity("general");
                break;

            case RECOMMEND_REQUEST:
                builder.needsUniversityData(true)
                       .needsScoreData(true)
                       .needsMajorData(true)
                       .needsMemoryData(true)
                       .urgency("high")
                       .specificity("specific");
                break;

            case PERSONALITY_ANALYSIS:
                builder.needsMajorData(true)
                       .needsMemoryData(true)
                       .urgency("medium")
                       .specificity("general");
                break;

            case COMPARISON:
                builder.needsUniversityData(true)
                       .needsScoreData(true)
                       .urgency("high")
                       .specificity("specific");
                break;

            default:
                builder.needsWebData(true)
                       .urgency("low")
                       .specificity("general");
        }

        // 检查是否已尝试某些数据源
        if (context != null) {
            if (context.hasAttemptedDataSource("LOCAL_DB")) {
                builder.needsWebData(true); // 本地已查过，可能需要网络补充
            }
        }

        return builder.build();
    }

    /**
     * 选择数据源和技能
     */
    private RetrievalDecision selectDataSource(
            RetrievalDecision.QuestionType questionType,
            RetrievalDecision.DataRequirement requirement,
            String question,
            AgentContext context) {

        Map<String, Object> params = new HashMap<>();

        switch (questionType) {
            case SCORE_QUERY:
                return buildScoreQueryDecision(question, requirement);

            case UNIVERSITY_QUERY:
                return buildUniversityQueryDecision(question, requirement);

            case MAJOR_QUERY:
                return buildMajorQueryDecision(question, requirement);

            case RECOMMEND_REQUEST:
                return buildRecommendDecision(question, requirement, context);

            case POLICY_QUERY:
                return RetrievalDecision.builder()
                        .dataSource(RetrievalDecision.DataSource.RAG)
                        .reasoning("政策问题优先使用向量检索")
                        .confidence(0.85)
                        .build();

            case PERSONALITY_ANALYSIS:
                return RetrievalDecision.builder()
                        .dataSource(RetrievalDecision.DataSource.MEMORY)
                        .skillName("major-query-skill")
                        .skillParams(Map.of("operation", "recommend-by-personality"))
                        .reasoning("性格分析需要专业匹配数据")
                        .confidence(0.8)
                        .build();

            case COMPARISON:
                return buildComparisonDecision(question, requirement);

            default:
                // 未知类型，使用LLM决策
                return decideWithLLM(question, context);
        }
    }

    /**
     * 构建分数查询决策
     */
    private RetrievalDecision buildScoreQueryDecision(String question, RetrievalDecision.DataRequirement requirement) {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "history");

        // 尝试从问题中提取参数
        extractUniversityFromQuestion(question, params);
        extractProvinceFromQuestion(question, params);
        extractSubjectTypeFromQuestion(question, params);

        return RetrievalDecision.localDbQuery("score-analysis-skill", params,
                "分数查询使用本地数据库技能");
    }

    /**
     * 构建院校查询决策
     */
    private RetrievalDecision buildUniversityQueryDecision(String question, RetrievalDecision.DataRequirement requirement) {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "query");

        // 提取查询参数
        extractProvinceFromQuestion(question, params);
        extractLevelFromQuestion(question, params);

        // 判断是否需要详情
        if (containsAny(question.toLowerCase(), "详情", "详细介绍", "详细信息")) {
            params.put("operation", "detail");
            extractUniversityFromQuestion(question, params);
        }

        // 判断是否需要搜索
        if (containsAny(question.toLowerCase(), "搜索", "查找", "有没有")) {
            params.put("operation", "search");
            params.put("keyword", extractKeyword(question));
        }

        RetrievalDecision primary = RetrievalDecision.localDbQuery("university-query-skill", params,
                "院校查询使用本地数据库技能");

        // 如果需求可能超出本地数据，添加网络搜索备选
        if (requirement.isNeedsWebData()) {
            primary.setFallbackDecision(RetrievalDecision.webSearch(
                    extractKeyword(question), "university",
                    "本地数据可能不足，备选网络搜索"));
        }

        return primary;
    }

    /**
     * 构建专业查询决策
     */
    private RetrievalDecision buildMajorQueryDecision(String question, RetrievalDecision.DataRequirement requirement) {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "query");

        String lower = question.toLowerCase();

        // 判断查询类型
        if (containsAny(lower, "详情", "介绍", "学习内容")) {
            params.put("operation", "detail");
            params.put("majorName", extractMajorName(question));
        } else if (containsAny(lower, "就业", "前景", "方向")) {
            params.put("operation", "detail");
            params.put("majorName", extractMajorName(question));
        } else {
            params.put("category", extractCategory(question));
            params.put("keyword", extractKeyword(question));
        }

        return RetrievalDecision.localDbQuery("major-query-skill", params,
                "专业查询使用本地数据库技能");
    }

    /**
     * 构建推荐决策
     */
    private RetrievalDecision buildRecommendDecision(String question, RetrievalDecision.DataRequirement requirement,
                                                     AgentContext context) {
        // 推荐需要多数据源协作
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "competitiveness");

        // 从上下文或问题中提取用户信息
        if (context != null && context.getUserInfo() != null) {
            AgentContext.UserInfo userInfo = context.getUserInfo();
            if (userInfo.getScore() != null) {
                params.put("score", userInfo.getScore());
            }
            if (userInfo.getProvince() != null) {
                params.put("province", userInfo.getProvince());
            }
            if (userInfo.getSubjectType() != null) {
                params.put("subjectType", userInfo.getSubjectType());
            }
        } else {
            // 尝试从问题提取
            extractScoreFromQuestion(question, params);
            extractProvinceFromQuestion(question, params);
            extractSubjectTypeFromQuestion(question, params);
        }

        return RetrievalDecision.builder()
                .dataSource(RetrievalDecision.DataSource.HYBRID)
                .skillName("score-analysis-skill")
                .skillParams(params)
                .reasoning("志愿推荐需要分数分析、院校查询、专业匹配多数据源协作")
                .confidence(0.85)
                .needsValidation(true)
                .build();
    }

    /**
     * 构建对比决策
     */
    private RetrievalDecision buildComparisonDecision(String question, RetrievalDecision.DataRequirement requirement) {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "detail");

        // 提取要对比的院校
        String keyword = extractKeyword(question);
        params.put("universityName", keyword);

        return RetrievalDecision.localDbQuery("university-query-skill", params,
                "院校对比使用本地数据库查询详情");
    }

    /**
     * 创建备选决策
     */
    private RetrievalDecision createFallbackDecision(RetrievalDecision primary, String question) {
        return RetrievalDecision.webSearch(
                extractKeyword(question),
                "university",
                "本地数据查询失败时的备选方案");
    }

    /**
     * 使用LLM进行决策（复杂情况）
     */
    private RetrievalDecision decideWithLLM(String question, AgentContext context) {
        try {
            DecisionAssistant assistant = AiServices.builder(DecisionAssistant.class)
                    .chatModel(chatModel)
                    .systemMessageProvider(id -> DECISION_SYSTEM_PROMPT)
                    .build();

            String response = assistant.decide(question);
            log.debug("LLM决策响应: {}", response);

            // 解析响应
            if (response.contains("LOCAL_DB")) {
                return RetrievalDecision.localDbQuery("university-query-skill",
                        Map.of("operation", "search", "keyword", extractKeyword(question)),
                        "LLM建议使用本地数据库");
            } else if (response.contains("WEB_SEARCH")) {
                return RetrievalDecision.webSearch(extractKeyword(question), "general",
                        "LLM建议使用网络搜索");
            } else {
                return RetrievalDecision.noRetrievalNeeded("LLM判断无需检索数据");
            }

        } catch (Exception e) {
            log.error("LLM决策失败: {}", e.getMessage());
            return RetrievalDecision.noRetrievalNeeded("决策失败，默认无需检索");
        }
    }

    /**
     * 从问题中提取院校信息
     */
    private void extractUniversityFromQuestion(String question, Map<String, Object> params) {
        // 常见院校名称匹配
        String[] universities = {"清华", "北大", "清华大学", "北京大学", "复旦", "上海交通大学",
                "浙江大学", "南京大学", "武汉大学", "中山大学", "四川大学", "华中科技大学"};

        for (String uni : universities) {
            if (question.contains(uni)) {
                params.put("universityName", uni);
                return;
            }
        }
    }

    /**
     * 从问题中提取省份
     */
    private void extractProvinceFromQuestion(String question, Map<String, Object> params) {
        String[] provinces = {"北京", "上海", "广东", "江苏", "浙江", "山东", "河南",
                "湖北", "湖南", "四川", "陕西", "安徽", "福建", "江西"};

        for (String province : provinces) {
            if (question.contains(province)) {
                params.put("province", province);
                return;
            }
        }
    }

    /**
     * 从问题中提取科目类型
     */
    private void extractSubjectTypeFromQuestion(String question, Map<String, Object> params) {
        String lower = question.toLowerCase();
        if (lower.contains("理科") || lower.contains("物理")) {
            params.put("subjectType", "理科");
        } else if (lower.contains("文科") || lower.contains("历史")) {
            params.put("subjectType", "文科");
        }
    }

    /**
     * 从问题中提取分数
     */
    private void extractScoreFromQuestion(String question, Map<String, Object> params) {
        // 匹配分数模式
        Pattern pattern = Pattern.compile("(\\d{3})分");
        java.util.regex.Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            params.put("score", Integer.parseInt(matcher.group(1)));
        }
    }

    /**
     * 从问题中提取层次
     */
    private void extractLevelFromQuestion(String question, Map<String, Object> params) {
        String lower = question.toLowerCase();
        if (lower.contains("985") || lower.contains("双一流")) {
            params.put("level", "985");
        } else if (lower.contains("211")) {
            params.put("level", "211");
        } else if (lower.contains("一本") || lower.contains("本科一批")) {
            params.put("level", "一本");
        } else if (lower.contains("二本") || lower.contains("本科二批")) {
            params.put("level", "二本");
        }
    }

    /**
     * 提取搜索关键词
     */
    private String extractKeyword(String question) {
        // 移除常见无关词，提取核心关键词
        return question.replaceAll("(请问|帮我|我想|查询|搜索|查找|了解一下|看看)", "")
                      .replaceAll("(的|吗|呢|吧|啊)", "")
                      .trim();
    }

    /**
     * 提取专业名称
     */
    private String extractMajorName(String question) {
        // 常见专业关键词
        String[] majors = {"计算机", "软件工程", "人工智能", "数据科学", "金融",
                "会计", "医学", "法学", "文学", "物理", "化学", "生物"};

        for (String major : majors) {
            if (question.contains(major)) {
                return major;
            }
        }
        return extractKeyword(question);
    }

    /**
     * 提取专业类别
     */
    private String extractCategory(String question) {
        String lower = question.toLowerCase();
        if (containsAny(lower, "工科", "理工", "工程")) {
            return "工学";
        } else if (containsAny(lower, "理科", "科学")) {
            return "理学";
        } else if (containsAny(lower, "文科", "文学", "人文")) {
            return "文学";
        } else if (containsAny(lower, "商科", "经济", "管理", "金融")) {
            return "经济学";
        } else if (containsAny(lower, "医学", "医学")) {
            return "医学";
        }
        return null;
    }

    /**
     * 检查是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 决策助手接口
     */
    interface DecisionAssistant {
        String decide(String question);
    }

    private static final String DECISION_SYSTEM_PROMPT = """
            你是数据检索决策助手。根据用户问题判断需要使用哪种数据源。

            可选数据源：
            - LOCAL_DB: 本地数据库，包含院校信息、专业信息、分数线数据
            - WEB_SEARCH: 网络搜索，用于获取最新或本地缺失的信息
            - NONE: 无需检索，可以直接回答

            返回格式：数据源名称，例如 "LOCAL_DB" 或 "WEB_SEARCH" 或 "NONE"
            """;
}