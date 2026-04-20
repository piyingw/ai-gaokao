package com.gaokao.test.ai;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.test.config.TestAiConfig;
import com.gaokao.test.config.AiServiceProvider;
import com.gaokao.test.model.AssertionRule;
import com.gaokao.test.model.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI测试用例生成器
 * 基于LangChain4j实现接口测试用例自动生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseGenerator {

    private final AiServiceProvider aiService;
    private final TestAiConfig testAiConfig;

    /**
     * 从Swagger/OpenAPI文档生成测试用例
     *
     * @param swaggerJson Swagger JSON文档
     * @return 生成的测试用例列表
     */
    public List<TestCase> generateFromSwagger(String swaggerJson) {
        log.info("开始从Swagger文档生成测试用例...");

        String prompt = buildSwaggerPrompt(swaggerJson);
        String response = aiService.chat(prompt);

        return parseTestCases(response);
    }

    /**
     * 为单个API生成测试用例
     *
     * @param apiPath    API路径
     * @param method     HTTP方法
     * @param apiDoc     API文档描述
     * @param parameters 参数定义
     * @return 测试用例列表
     */
    public List<TestCase> generateForApi(String apiPath, String method,
                                         String apiDoc, Map<String, Object> parameters) {
        log.info("为API {} {} 生成测试用例...", method, apiPath);

        String prompt = buildApiPrompt(apiPath, method, apiDoc, parameters);
        String response = aiService.chat(prompt);

        return parseTestCases(response);
    }

    /**
     * 智能生成边界值测试数据
     *
     * @param fieldName  字段名称
     * @param fieldType  字段类型
     * @param constraints 字段约束（min, max, required等）
     * @return 边界值测试数据列表
     */
    public List<Object> generateBoundaryValues(String fieldName, String fieldType,
                                               Map<String, Object> constraints) {
        String prompt = """
                分析以下字段约束，生成边界值测试数据：

                字段名称：%s
                字段类型：%s
                字段约束：%s

                请生成以下类型的边界值：
                1. 最小值边界（min-1, min, min+1）
                2. 最大值边界（max-1, max, max+1）
                3. 类型边界（如字符串空值、超长值）
                4. 特殊值（null, 空字符串, 负数等）

                输出JSON数组格式，每个元素包含：value, description, expectedResult
                """.formatted(fieldName, fieldType, JSON.toJSONString(constraints));

        String response = aiService.chat(prompt);
        return parseBoundaryValues(response);
    }

    /**
     * 构建Swagger文档解析Prompt
     */
    private String buildSwaggerPrompt(String swaggerJson) {
        return """
                你是一位资深测试工程师，请分析以下Swagger/OpenAPI文档，为每个API生成完整的测试用例集。

                Swagger文档：
                %s

                请为每个API生成以下类型的测试用例：
                1. 正向测试（Happy Path）：验证正常业务流程
                2. 边界值测试：针对参数边界条件设计测试
                3. 异常输入测试：验证非法参数、缺失参数的处理
                4. 安全性测试：验证权限、注入攻击防护

                输出格式要求（JSON数组）：
                [
                  {
                    "testCaseId": "TC_xxx",
                    "name": "测试名称",
                    "description": "测试描述",
                    "testType": "HAPPY_PATH|BOUNDARY|EXCEPTION|SECURITY",
                    "priority": "P0|P1|P2|P3",
                    "apiPath": "/api/xxx",
                    "method": "GET|POST|PUT|DELETE",
                    "headers": {"key": "value"},
                    "requestParams": {"key": "value"},
                    "requestBody": "JSON字符串",
                    "expectedStatusCode": 200,
                    "expectedResponse": "预期响应JSON",
                    "assertionRules": [
                      {
                        "assertionId": "AS_xxx",
                        "type": "EXACT_MATCH|TYPE_CHECK|EXISTS|RANGE|SEMANTIC",
                        "fieldPath": "data.field",
                        "expectedValue": "预期值",
                        "condition": "EQUALS|GREATER_THAN|SIMILAR_TO"
                      }
                    ],
                    "tags": ["标签1", "标签2"]
                  }
                ]

                注意：
                1. 测试用例ID使用 TC_ 前缀
                2. 根据API重要性设置优先级
                3. 为必填参数设计缺失测试
                4. 为数值参数设计边界值测试
                """.formatted(swaggerJson);
    }

    /**
     * 构建单API生成Prompt
     */
    private String buildApiPrompt(String apiPath, String method,
                                  String apiDoc, Map<String, Object> parameters) {
        return """
                你是一位资深测试工程师，请为以下API生成完整的测试用例集。

                API信息：
                - 路径：%s
                - 方法：%s
                - 描述：%s
                - 参数定义：%s

                请生成至少包含以下类型的测试用例：
                1. 正向测试（Happy Path）：覆盖正常业务场景
                2. 边界值测试：针对每个数值参数设计边界测试
                3. 异常输入测试：验证参数校验逻辑
                4. 安全性测试：验证权限和数据安全

                输出JSON数组格式（参考Swagger文档生成格式）
                """.formatted(apiPath, method, apiDoc, JSON.toJSONString(parameters));
    }

    /**
     * 解析AI响应中的测试用例
     */
    public List<TestCase> parseTestCases(String response) {
        try {
            // 提取JSON内容
            String jsonContent = extractJsonContent(response);

            JSONArray jsonArray = JSON.parseArray(jsonContent);
            List<TestCase> testCases = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                TestCase testCase = parseTestCase(obj);
                testCases.add(testCase);
            }

            log.info("成功生成 {} 个测试用例", testCases.size());
            return testCases;

        } catch (Exception e) {
            log.error("解析测试用例失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析单个测试用例
     */
    private TestCase parseTestCase(JSONObject obj) {
        TestCase testCase = TestCase.builder()
                .testCaseId(obj.getString("testCaseId"))
                .name(obj.getString("name"))
                .description(obj.getString("description"))
                .testType(TestCase.TestType.valueOf(obj.getString("testType")))
                .priority(obj.getString("priority"))
                .apiPath(obj.getString("apiPath"))
                .method(obj.getString("method"))
                .headers(parseHeaders(obj.getJSONObject("headers")))
                .requestParams(parseRequestParams(obj.getJSONObject("requestParams")))
                .requestBody(obj.getString("requestBody"))
                .expectedStatusCode(obj.getIntValue("expectedStatusCode"))
                .expectedResponse(obj.getString("expectedResponse"))
                .tags(parseList(obj.getJSONArray("tags")))
                .assertionRules(parseAssertionRules(obj.getJSONArray("assertionRules")))
                .build();

        // 如果没有生成ID，自动生成
        if (testCase.getTestCaseId() == null || testCase.getTestCaseId().isEmpty()) {
            testCase.setTestCaseId("TC_" + IdUtil.fastSimpleUUID());
        }

        return testCase;
    }

    /**
     * 解析断言规则列表
     */
    private List<AssertionRule> parseAssertionRules(JSONArray arr) {
        if (arr == null) return new ArrayList<>();

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
            rules.add(rule);
        }
        return rules;
    }

    /**
     * 解析边界值数据
     */
    private List<Object> parseBoundaryValues(String response) {
        String jsonContent = extractJsonContent(response);
        JSONArray arr = JSON.parseArray(jsonContent);
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            values.add(arr.getJSONObject(i).get("value"));
        }
        return values;
    }

    /**
     * 从响应中提取JSON内容
     */
    private String extractJsonContent(String response) {
        // 处理可能的Markdown包裹
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
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']') + 1;
        if (start >= 0 && end > start) {
            return response.substring(start, end);
        }
        return response;
    }

    private Map<String, Object> parseRequestParams(JSONObject obj) {
        if (obj == null) return new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        for (String key : obj.keySet()) {
            result.put(key, obj.get(key));
        }
        return result;
    }

    private Map<String, String> parseHeaders(JSONObject obj) {
        if (obj == null) return new HashMap<>();
        Map<String, String> result = new HashMap<>();
        for (String key : obj.keySet()) {
            result.put(key, obj.getString(key));
        }
        return result;
    }

    private List<String> parseList(JSONArray arr) {
        if (arr == null) return new ArrayList<>();
        return arr.toList(String.class);
    }
}