package com.gaokao.ai.agent.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 意图路由器
 * 综合关键词匹配和LLM意图识别，实现智能路由决策
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRouter {

    private final KeywordMatcher keywordMatcher;
    private final LlmIntentRecognizer llmIntentRecognizer;

    /**
     * 可用的Agent列表
     */
    private static final List<String> AVAILABLE_AGENTS = List.of(
            "recommend", "policy", "school", "personality"
    );

    /**
     * 关键词匹配置信度阈值
     * 超过此阈值直接使用关键词匹配结果，不调用LLM
     */
    private static final double KEYWORD_CONFIDENCE_THRESHOLD = 0.85;

    /**
     * LLM置信度优先阈值
     * 当LLM置信度超过此值且高于关键词匹配时，使用LLM结果
     */
    private static final double LLM_CONFIDENCE_THRESHOLD = 0.75;

    /**
     * 执行意图路由
     * @param question 用户问题
     * @return 路由结果
     */
    public IntentRouteResult route(String question) {
        log.info("开始意图路由: question={}", question);

        // 1. 关键词快速匹配
        IntentRouteResult.KeywordMatchResult keywordResult = keywordMatcher.match(question);
        log.debug("关键词匹配结果: agent={}, score={}", keywordResult.getAgent(), keywordResult.getScore());

        // 2. 判断是否需要调用LLM
        if (keywordResult.getScore() >= KEYWORD_CONFIDENCE_THRESHOLD) {
            log.info("关键词匹配置信度足够高({}), 直接路由", keywordResult.getScore());
            return IntentRouteResult.builder()
                    .agent(keywordResult.getAgent())
                    .confidence(keywordResult.getScore())
                    .source("keyword")
                    .reason("关键词匹配命中: " + keywordResult.getMatchedKeywords())
                    .keywordResult(keywordResult)
                    .build();
        }

        // 3. 调用LLM进行意图识别
        IntentRouteResult.LlmRecognizeResult llmResult = llmIntentRecognizer.recognize(question, AVAILABLE_AGENTS);
        log.debug("LLM识别结果: agent={}, confidence={}", llmResult.getAgent(), llmResult.getConfidence());

        // 4. 融合决策
        return mergeResults(keywordResult, llmResult);
    }

    /**
     * 融合关键词和LLM结果
     */
    private IntentRouteResult mergeResults(IntentRouteResult.KeywordMatchResult keywordResult,
                                           IntentRouteResult.LlmRecognizeResult llmResult) {
        String finalAgent;
        double finalConfidence;
        String source;
        String reason;

        // 决策逻辑
        if (llmResult.getConfidence() >= LLM_CONFIDENCE_THRESHOLD &&
            llmResult.getConfidence() > keywordResult.getScore()) {
            // LLM置信度足够且高于关键词匹配
            finalAgent = llmResult.getAgent();
            finalConfidence = llmResult.getConfidence();
            source = "llm";
            reason = "LLM识别置信度更高: " + llmResult.getReasoning();
            log.info("采用LLM路由结果: agent={}, confidence={}", finalAgent, finalConfidence);
        } else if (keywordResult.getScore() >= 0.7) {
            // 关键词匹配置信度较高
            finalAgent = keywordResult.getAgent();
            finalConfidence = keywordResult.getScore();
            source = "keyword";
            reason = "关键词匹配置信度较高: " + keywordResult.getMatchedKeywords();
            log.info("采用关键词路由结果: agent={}, confidence={}", finalAgent, finalConfidence);
        } else {
            // 两者置信度都不够高，综合判断
            finalAgent = determineFinalAgent(keywordResult, llmResult);
            finalConfidence = (keywordResult.getScore() + llmResult.getConfidence()) / 2;
            source = "merged";
            reason = "融合关键词和LLM判断";
            log.info("融合路由结果: agent={}, confidence={}", finalAgent, finalConfidence);
        }

        // 构建候选得分列表
        List<IntentRouteResult.AgentScore> candidateScores = buildCandidateScores(keywordResult);

        return IntentRouteResult.builder()
                .agent(finalAgent)
                .confidence(finalConfidence)
                .source(source)
                .reason(reason)
                .keywordResult(keywordResult)
                .llmResult(llmResult)
                .candidateScores(candidateScores)
                .build();
    }

    /**
     * 综合判断最终Agent
     */
    private String determineFinalAgent(IntentRouteResult.KeywordMatchResult keywordResult,
                                       IntentRouteResult.LlmRecognizeResult llmResult) {
        // 如果两者指向同一Agent，直接使用
        if (keywordResult.getAgent().equals(llmResult.getAgent())) {
            return keywordResult.getAgent();
        }

        // 不同Agent时，优先考虑LLM结果（语义理解更准确）
        // 但如果关键词匹配有明确命中，给予更高权重
        if (keywordResult.getMatchCount() > 0 && keywordResult.getScore() > 0.5) {
            return keywordResult.getAgent();
        }

        return llmResult.getAgent();
    }

    /**
     * 构建候选Agent得分列表
     */
    private List<IntentRouteResult.AgentScore> buildCandidateScores(
            IntentRouteResult.KeywordMatchResult keywordResult) {
        List<IntentRouteResult.AgentScore> scores = new ArrayList<>();

        // 当前实现只返回匹配的Agent
        scores.add(new IntentRouteResult.AgentScore(
                keywordResult.getAgent(),
                keywordResult.getScore(),
                String.join(",", keywordResult.getMatchedKeywords())
        ));

        return scores;
    }

    /**
     * 检查是否需要多Agent协作
     */
    public IntentRouteResult checkCollaboration(String question) {
        LlmIntentRecognizer.CollaborationResult collaborationResult =
                llmIntentRecognizer.recognizeCollaboration(question, AVAILABLE_AGENTS);

        if (collaborationResult.needsCollaboration()) {
            return IntentRouteResult.builder()
                    .agent(collaborationResult.agents().get(0)) // 主Agent
                    .confidence(0.8)
                    .source("collaboration")
                    .reason(collaborationResult.reasoning())
                    .needsCollaboration(true)
                    .collaborationAgents(collaborationResult.agents())
                    .build();
        }

        return null; // 不需要协作
    }

    /**
     * 获取路由统计信息
     */
    public Map<String, Integer> getRouterStats() {
        return keywordMatcher.getKeywordStats();
    }
}