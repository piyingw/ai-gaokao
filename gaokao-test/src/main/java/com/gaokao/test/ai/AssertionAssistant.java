package com.gaokao.test.ai;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.test.config.TestAiConfig;
import com.gaokao.test.config.AiServiceProvider;
import com.gaokao.test.model.AssertionRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI智能断言助手
 * 支持语义相似度校验，而非仅精确匹配
 * 特别适用于AI回复等动态响应的测试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssertionAssistant {

    private final AiServiceProvider aiService;
    private final TestAiConfig testAiConfig;

    /**
     * 为API响应生成智能断言规则
     *
     * @param response 实际API响应
     * @return 生成的断言规则列表
     */
    public List<AssertionRule> generateAssertions(String response) {
        log.info("开始为响应生成智能断言规则...");

        String prompt = buildAssertionPrompt(response);
        String aiResponse = aiService.chat(prompt);

        return parseAssertionRules(aiResponse);
    }

    /**
     * 为特定API生成断言规则（带预期响应模板）
     *
     * @param apiPath         API路径
     * @param expectedTemplate 预期响应模板
     * @param actualResponse   实际响应样例
     * @return 断言规则列表
     */
    public List<AssertionRule> generateAssertionsForApi(String apiPath,
                                                         String expectedTemplate,
                                                         String actualResponse) {
        String prompt = """
                你是一位资深测试工程师，请为以下API设计断言规则。

                API路径：%s
                预期响应模板：%s
                实际响应样例：%s

                请分析响应结构，生成以下类型的断言：
                1. 必要字段存在性校验
                2. 数据类型校验
                3. 业务逻辑校验（如状态码、结果标识）
                4. 边界值校验（如列表长度、数值范围）
                5. 动态字段校验（如时间戳、ID等，使用正则或存在性断言）

                输出JSON数组格式：
                [
                  {
                    "assertionId": "AS_xxx",
                    "type": "EXACT_MATCH|TYPE_CHECK|EXISTS|RANGE|REGEX|SEMANTIC|SCHEMA",
                    "fieldPath": "data.field",
                    "expectedValue": "预期值或描述",
                    "condition": "EQUALS|GREATER_THAN|CONTAINS|MATCHES|SIMILAR_TO",
                    "errorMessage": "断言失败时的提示信息"
                  }
                ]
                """.formatted(apiPath, expectedTemplate, actualResponse);

        String response = aiService.chat(prompt);
        return parseAssertionRules(response);
    }

    /**
     * 语义相似度断言（用于AI回复等动态内容校验）
     *
     * @param actual   实际响应内容
     * @param expected 预期响应内容（可以是模板或关键信息）
     * @param threshold 相似度阈值（0-1），推荐0.85
     * @return 是否语义相似
     */
    public SemanticAssertionResult semanticAssert(String actual, String expected, double threshold) {
        log.info("执行语义相似度断言，阈值: {}", threshold);

        // 构建语义比对Prompt
        String prompt = """
                请分析以下两段文本的语义相似度：

                文本A（实际响应）：%s

                文本B（预期内容）：%s

                请判断：
                1. 语义相似度评分（0-1之间的浮点数）
                2. 是否表达了相同的核心信息
                3. 关键信息点对比（列出相同和不同的点）

                输出JSON格式：
                {
                  "similarityScore": 0.xx,
                  "semanticallyEquivalent": true/false,
                  "keyPointsMatch": ["匹配的关键点"],
                  "keyPointsMismatch": ["不匹配的关键点"],
                  "analysis": "简要分析"
                }
                """.formatted(actual, expected);

        String response = aiService.chat(prompt);
        return parseSemanticResult(response, threshold);
    }

    /**
     * 使用默认阈值的语义断言
     */
    public SemanticAssertionResult semanticAssert(String actual, String expected) {
        return semanticAssert(actual, expected, testAiConfig.getDefaultSemanticThreshold());
    }

    /**
     * AI回复内容质量校验
     * 验证AI生成内容是否符合预期格式和关键信息
     *
     * @param aiResponse  AI实际回复
     * @param requirements 预期要求描述
     * @return 校验结果
     */
    public AIResponseQualityResult validateAIResponse(String aiResponse, String requirements) {
        String prompt = """
                请校验以下AI回复内容是否符合预期要求：

                AI回复内容：%s

                预期要求：%s

                请从以下维度进行校验：
                1. 格式正确性：是否符合预期的输出格式
                2. 内容完整性：是否包含必要的信息要素
                3. 准确性：关键信息是否正确
                4. 相关性：是否与问题相关，无明显偏题
                5. 安全性：是否存在敏感或有害内容

                输出JSON格式：
                {
                  "formatValid": true/false,
                  "completenessScore": 0.xx,
                  "accuracyScore": 0.xx,
                  "relevanceScore": 0.xx,
                  "safetyValid": true/false,
                  "overallScore": 0.xx,
                  "issues": ["发现的问题列表"],
                  "suggestions": ["改进建议"]
                }
                """.formatted(aiResponse, requirements);

        String response = aiService.chat(prompt);
        return parseQualityResult(response);
    }

    /**
     * 构建断言生成Prompt
     */
    private String buildAssertionPrompt(String response) {
        return """
                你是一位资深测试工程师，请分析以下API响应结构，设计全面的断言规则。

                响应数据：%s

                请分析响应结构，生成以下类型的断言：
                1. 状态码断言
                2. 必要字段存在性断言（success, code, message, data等）
                3. 数据类型断言
                4. 业务逻辑断言（如success=true时data应存在）
                5. 动态字段处理（时间戳、ID等使用正则或存在性断言）

                输出JSON数组格式：
                [
                  {
                    "assertionId": "AS_xxx",
                    "type": "TYPE_CHECK|EXISTS|RANGE|REGEX",
                    "fieldPath": "JSON路径，如data.list",
                    "expectedValue": "预期值或类型描述",
                    "condition": "EQUALS|CONTAINS|MATCHES",
                    "errorMessage": "断言失败提示"
                  }
                ]
                """.formatted(response);
    }

    /**
     * 解析断言规则
     */
    private List<AssertionRule> parseAssertionRules(String response) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONArray arr = JSON.parseArray(jsonContent);
            List<AssertionRule> rules = new ArrayList<>();

            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                AssertionRule rule = AssertionRule.builder()
                        .assertionId(obj.getString("assertionId"))
                        .type(AssertionRule.AssertionType.valueOf(obj.getString("type")))
                        .fieldPath(obj.getString("fieldPath"))
                        .expectedValue(obj.get("expectedValue"))
                        .condition(AssertionRule.Condition.valueOf(obj.getString("condition")))
                        .errorMessage(obj.getString("errorMessage"))
                        .build();

                if (rule.getAssertionId() == null || rule.getAssertionId().isEmpty()) {
                    rule.setAssertionId("AS_" + IdUtil.fastSimpleUUID());
                }

                rules.add(rule);
            }

            log.info("成功生成 {} 条断言规则", rules.size());
            return rules;

        } catch (Exception e) {
            log.error("解析断言规则失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析语义断言结果
     */
    private SemanticAssertionResult parseSemanticResult(String response, double threshold) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            double score = obj.getDoubleValue("similarityScore");
            boolean equivalent = obj.getBooleanValue("semanticallyEquivalent");

            return SemanticAssertionResult.builder()
                    .similarityScore(score)
                    .passed(score >= threshold)
                    .semanticallyEquivalent(equivalent)
                    .keyPointsMatch(parseList(obj.getJSONArray("keyPointsMatch")))
                    .keyPointsMismatch(parseList(obj.getJSONArray("keyPointsMismatch")))
                    .analysis(obj.getString("analysis"))
                    .threshold(threshold)
                    .build();

        } catch (Exception e) {
            log.error("解析语义结果失败: {}", e.getMessage());
            return SemanticAssertionResult.builder()
                    .passed(false)
                    .analysis("解析失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析AI回复质量校验结果
     */
    private AIResponseQualityResult parseQualityResult(String response) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            return AIResponseQualityResult.builder()
                    .formatValid(obj.getBooleanValue("formatValid"))
                    .completenessScore(obj.getDoubleValue("completenessScore"))
                    .accuracyScore(obj.getDoubleValue("accuracyScore"))
                    .relevanceScore(obj.getDoubleValue("relevanceScore"))
                    .safetyValid(obj.getBooleanValue("safetyValid"))
                    .overallScore(obj.getDoubleValue("overallScore"))
                    .issues(parseList(obj.getJSONArray("issues")))
                    .suggestions(parseList(obj.getJSONArray("suggestions")))
                    .build();

        } catch (Exception e) {
            log.error("解析质量结果失败: {}", e.getMessage());
            return AIResponseQualityResult.builder()
                    .formatValid(false)
                    .issues(List.of("解析失败: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 从响应中提取JSON内容
     */
    private String extractJsonContent(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        // 尝试直接解析
        if (response.trim().startsWith("{") || response.trim().startsWith("[")) {
            return response.trim();
        }
        return response;
    }

    private List<String> parseList(JSONArray arr) {
        if (arr == null) return new ArrayList<>();
        return arr.toList(String.class);
    }

    /**
     * 语义断言结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SemanticAssertionResult {
        private double similarityScore;
        private boolean passed;
        private boolean semanticallyEquivalent;
        private List<String> keyPointsMatch;
        private List<String> keyPointsMismatch;
        private String analysis;
        private double threshold;
    }

    /**
     * AI回复质量校验结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AIResponseQualityResult {
        private boolean formatValid;
        private double completenessScore;
        private double accuracyScore;
        private double relevanceScore;
        private boolean safetyValid;
        private double overallScore;
        private List<String> issues;
        private List<String> suggestions;
    }
}