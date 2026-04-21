package com.gaokao.ai.agent.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 关键词匹配器
 * 使用权重化关键词库进行意图识别
 */
@Slf4j
@Component
public class KeywordMatcher {

    /**
     * Agent关键词权重配置
     * 格式: Map<AgentName, Map<Keyword, Weight>>
     */
    private final Map<String, Map<String, Double>> keywordWeights;

    /**
     * 高置信度关键词（直接路由）
     */
    private final Map<String, String> highConfidenceKeywords;

    /**
     * 排除词（当出现这些词时，降低对应Agent的置信度）
     */
    private final Map<String, List<String>> exclusionKeywords;

    public KeywordMatcher() {
        this.keywordWeights = initKeywordWeights();
        this.highConfidenceKeywords = initHighConfidenceKeywords();
        this.exclusionKeywords = initExclusionKeywords();
    }

    /**
     * 匹配用户问题，返回路由结果
     */
    public IntentRouteResult.KeywordMatchResult match(String question) {
        if (question == null || question.trim().isEmpty()) {
            return IntentRouteResult.KeywordMatchResult.builder()
                    .agent("recommend")
                    .score(0.3)
                    .matchedKeywords(List.of())
                    .matchCount(0)
                    .build();
        }

        String normalizedQuestion = normalizeQuestion(question);

        // 1. 检查高置信度关键词
        for (Map.Entry<String, String> entry : highConfidenceKeywords.entrySet()) {
            if (normalizedQuestion.contains(entry.getKey())) {
                return IntentRouteResult.KeywordMatchResult.builder()
                        .agent(entry.getValue())
                        .score(0.95)
                        .matchedKeywords(List.of(entry.getKey()))
                        .matchCount(1)
                        .build();
            }
        }

        // 2. 计算各Agent的匹配得分
        Map<String, Double> agentScores = new HashMap<>();
        Map<String, List<String>> matchedKeywordsByAgent = new HashMap<>();

        for (String agent : keywordWeights.keySet()) {
            double score = calculateAgentScore(agent, normalizedQuestion, matchedKeywordsByAgent);
            agentScores.put(agent, score);
        }

        // 3. 应用排除词规则
        applyExclusionRules(normalizedQuestion, agentScores);

        // 4. 找出最高得分的Agent
        String bestAgent = "recommend";
        double bestScore = 0.3;
        List<String> bestMatchedKeywords = List.of();

        for (Map.Entry<String, Double> entry : agentScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestAgent = entry.getKey();
                bestScore = entry.getValue();
                bestMatchedKeywords = matchedKeywordsByAgent.getOrDefault(entry.getKey(), List.of());
            }
        }

        log.debug("关键词匹配结果: agent={}, score={}, keywords={}",
                bestAgent, bestScore, bestMatchedKeywords);

        return IntentRouteResult.KeywordMatchResult.builder()
                .agent(bestAgent)
                .score(bestScore)
                .matchedKeywords(bestMatchedKeywords)
                .matchCount(bestMatchedKeywords.size())
                .build();
    }

    /**
     * 计算单个Agent的匹配得分
     */
    private double calculateAgentScore(String agent, String question,
                                       Map<String, List<String>> matchedKeywordsByAgent) {
        Map<String, Double> keywords = keywordWeights.get(agent);
        if (keywords == null || keywords.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        List<String> matchedKeywords = new ArrayList<>();

        for (Map.Entry<String, Double> entry : keywords.entrySet()) {
            String keyword = entry.getKey();
            double weight = entry.getValue();

            if (question.contains(keyword)) {
                totalScore += weight;
                matchedKeywords.add(keyword);
            }
        }

        // 匹配多个关键词时，得分叠加但有上限
        double finalScore = Math.min(totalScore, 0.85);

        matchedKeywordsByAgent.put(agent, matchedKeywords);
        return finalScore;
    }

    /**
     * 应用排除词规则
     */
    private void applyExclusionRules(String question, Map<String, Double> agentScores) {
        for (Map.Entry<String, List<String>> entry : exclusionKeywords.entrySet()) {
            String agent = entry.getKey();
            List<String> exclusions = entry.getValue();

            for (String exclusion : exclusions) {
                if (question.contains(exclusion)) {
                    // 降低该Agent的置信度
                    Double currentScore = agentScores.get(agent);
                    if (currentScore != null) {
                        agentScores.put(agent, currentScore * 0.5);
                        log.debug("排除词'{}'降低agent '{}'得分: {} -> {}",
                                exclusion, agent, currentScore, currentScore * 0.5);
                    }
                }
            }
        }
    }

    /**
     * 标准化问题文本
     */
    private String normalizeQuestion(String question) {
        return question.toLowerCase()
                .replaceAll("[，。！？、；：]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 初始化关键词权重库
     */
    private Map<String, Map<String, Double>> initKeywordWeights() {
        Map<String, Map<String, Double>> weights = new HashMap<>();

        // 志愿推荐Agent关键词
        Map<String, Double> recommendKeywords = new HashMap<>();
        recommendKeywords.put("推荐", 0.4);
        recommendKeywords.put("志愿", 0.45);
        recommendKeywords.put("填报", 0.4);
        recommendKeywords.put("选择学校", 0.35);
        recommendKeywords.put("分数匹配", 0.3);
        recommendKeywords.put("录取概率", 0.35);
        recommendKeywords.put("冲稳保", 0.4);
        recommendKeywords.put("志愿方案", 0.45);
        recommendKeywords.put("报考建议", 0.35);
        recommendKeywords.put("填报策略", 0.4);
        recommendKeywords.put("志愿填报", 0.5);
        recommendKeywords.put("选学校", 0.3);
        recommendKeywords.put("选专业", 0.3);
        recommendKeywords.put("我的分数", 0.25);
        recommendKeywords.put("能报", 0.25);
        recommendKeywords.put("可以报", 0.25);
        recommendKeywords.put("能上", 0.25);
        weights.put("recommend", recommendKeywords);

        // 政策问答Agent关键词
        Map<String, Double> policyKeywords = new HashMap<>();
        policyKeywords.put("政策", 0.45);
        policyKeywords.put("规则", 0.4);
        policyKeywords.put("录取规则", 0.45);
        policyKeywords.put("批次", 0.35);
        policyKeywords.put("平行志愿", 0.45);
        policyKeywords.put("顺序志愿", 0.4);
        policyKeywords.put("投档", 0.4);
        policyKeywords.put("退档", 0.35);
        policyKeywords.put("调剂", 0.35);
        policyKeywords.put("征集志愿", 0.4);
        policyKeywords.put("加分", 0.35);
        policyKeywords.put("专项计划", 0.35);
        policyKeywords.put("招生简章", 0.35);
        policyKeywords.put("录取方式", 0.3);
        policyKeywords.put("高考政策", 0.45);
        policyKeywords.put("志愿规则", 0.4);
        policyKeywords.put("什么是", 0.2);
        policyKeywords.put("怎么理解", 0.2);
        policyKeywords.put("解释一下", 0.2);
        weights.put("policy", policyKeywords);

        // 学校信息Agent关键词
        Map<String, Double> schoolKeywords = new HashMap<>();
        schoolKeywords.put("大学", 0.35);
        schoolKeywords.put("学院", 0.3);
        schoolKeywords.put("院校", 0.4);
        schoolKeywords.put("学校信息", 0.45);
        schoolKeywords.put("排名", 0.35);
        schoolKeywords.put("对比", 0.3);
        schoolKeywords.put("比较", 0.3);
        schoolKeywords.put("区别", 0.3);
        schoolKeywords.put("哪个好", 0.25);
        schoolKeywords.put("怎么样", 0.2);
        schoolKeywords.put("简介", 0.3);
        schoolKeywords.put("特色", 0.3);
        schoolKeywords.put("优势专业", 0.35);
        schoolKeywords.put("王牌专业", 0.35);
        schoolKeywords.put("分数线", 0.3);
        schoolKeywords.put("录取分数", 0.35);
        schoolKeywords.put("历年分数", 0.3);
        schoolKeywords.put("最低分", 0.25);
        schoolKeywords.put("最高分", 0.25);
        schoolKeywords.put("多少分能上", 0.3);
        schoolKeywords.put("录取线", 0.3);
        schoolKeywords.put("学费", 0.2);
        schoolKeywords.put("地址", 0.15);
        schoolKeywords.put("位置", 0.15);
        schoolKeywords.put("在哪", 0.2);
        weights.put("school", schoolKeywords);

        // 性格分析Agent关键词
        Map<String, Double> personalityKeywords = new HashMap<>();
        personalityKeywords.put("性格", 0.45);
        personalityKeywords.put("兴趣", 0.4);
        personalityKeywords.put("爱好", 0.35);
        personalityKeywords.put("适合", 0.3);
        personalityKeywords.put("职业", 0.35);
        personalityKeywords.put("规划", 0.3);
        personalityKeywords.put("测试", 0.25);
        personalityKeywords.put("分析", 0.2);
        personalityKeywords.put("mbti", 0.45);
        personalityKeywords.put("霍兰德", 0.45);
        personalityKeywords.put("职业兴趣", 0.4);
        personalityKeywords.put("专业方向", 0.3);
        personalityKeywords.put("职业方向", 0.35);
        personalityKeywords.put("我适合", 0.3);
        personalityKeywords.put("适合什么", 0.3);
        personalityKeywords.put("性格测试", 0.4);
        personalityKeywords.put("职业规划", 0.4);
        personalityKeywords.put("发展方向", 0.3);
        personalityKeywords.put("就业前景", 0.25);
        personalityKeywords.put("未来发展", 0.25);
        weights.put("personality", personalityKeywords);

        return weights;
    }

    /**
     * 初始化高置信度关键词（直接路由，无需LLM）
     */
    private Map<String, String> initHighConfidenceKeywords() {
        Map<String, String> keywords = new HashMap<>();
        keywords.put("一键生成志愿", "recommend");
        keywords.put("生成志愿方案", "recommend");
        keywords.put("志愿填报方案", "recommend");
        keywords.put("平行志愿是什么", "policy");
        keywords.put("什么是平行志愿", "policy");
        keywords.put("顺序志愿是什么", "policy");
        keywords.put("什么是顺序志愿", "policy");
        keywords.put("录取规则是什么", "policy");
        keywords.put("投档规则", "policy");
        keywords.put("性格测试", "personality");
        keywords.put("职业兴趣测试", "personality");
        keywords.put("mbti测试", "personality");
        keywords.put("霍兰德测试", "personality");
        keywords.put("清华大学分数线", "school");
        keywords.put("北京大学分数线", "school");
        keywords.put("清华北大对比", "school");
        keywords.put("院校对比", "school");
        keywords.put("大学排名", "school");
        return keywords;
    }

    /**
     * 初始化排除词规则
     */
    private Map<String, List<String>> initExclusionKeywords() {
        Map<String, List<String>> exclusions = new HashMap<>();

        // 当用户询问"推荐专业"时，不应仅路由到school
        exclusions.put("school", List.of("推荐", "建议", "志愿方案"));

        // 当用户询问具体学校分数线时，不应路由到personality
        exclusions.put("personality", List.of("分数线", "录取", "分数", "院校"));

        // 当用户询问性格相关时，不应路由到policy
        exclusions.put("policy", List.of("性格", "兴趣", "适合什么专业"));

        return exclusions;
    }

    /**
     * 获取所有Agent的关键词统计
     */
    public Map<String, Integer> getKeywordStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (String agent : keywordWeights.keySet()) {
            stats.put(agent, keywordWeights.get(agent).size());
        }
        return stats;
    }
}