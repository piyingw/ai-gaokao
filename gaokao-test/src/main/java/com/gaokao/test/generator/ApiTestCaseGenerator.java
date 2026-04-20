package com.gaokao.test.generator;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.test.ai.TestCaseGenerator;
import com.gaokao.test.model.AssertionRule;
import com.gaokao.test.model.TestCase;
import com.gaokao.test.config.AiServiceProvider;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API测试用例生成器
 * 结合AI生成能力，生成可直接执行的测试代码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTestCaseGenerator {

    private final TestCaseGenerator aiGenerator;
    private final AiServiceProvider aiService;

    /**
     * 从Swagger文档生成完整测试用例集
     *
     * @param swaggerUrl Swagger文档URL或JSON内容
     * @param baseUrl    测试目标服务基础URL
     * @return 测试用例列表
     */
    public List<TestCase> generateTestSuite(String swaggerUrl, String baseUrl) {
        log.info("开始生成API测试套件，目标服务: {}", baseUrl);

        // 获取Swagger文档
        String swaggerJson = fetchSwaggerDoc(swaggerUrl);

        // AI生成测试用例
        List<TestCase> testCases = aiGenerator.generateFromSwagger(swaggerJson);

        // 补充测试用例的执行信息
        for (TestCase tc : testCases) {
            enrichTestCase(tc, baseUrl);
        }

        return testCases;
    }

    /**
     * 为指定Controller生成测试用例
     *
     * @param controllerClass Controller类名
     * @param apiDocs         API文档描述
     * @return 测试用例列表
     */
    public List<TestCase> generateForController(String controllerClass, String apiDocs) {
        String prompt = """
                请为以下Controller类生成完整的接口测试用例：

                Controller类：%s
                API文档：%s

                请分析每个接口方法，生成测试用例覆盖：
                1. 正常业务场景
                2. 参数校验场景（必填参数缺失、参数格式错误）
                3. 权限校验场景（需要登录、会员权限）
                4. 边界值场景

                输出JSON数组格式的测试用例列表。
                """.formatted(controllerClass, apiDocs);

        String response = aiService.chat(prompt);
        return aiGenerator.parseTestCases(response);
    }

    /**
     * 执行单个测试用例
     *
     * @param testCase 测试用例
     * @param baseUrl  服务基础URL
     * @return 测试执行结果
     */
    public TestExecutionResult executeTestCase(TestCase testCase, String baseUrl) {
        log.info("执行测试用例: {}", testCase.getName());

        try {
            RequestSpecification request = RestAssured.given()
                    .baseUri(baseUrl)
                    .contentType("application/json");

            // 设置请求头
            if (testCase.getHeaders() != null) {
                testCase.getHeaders().forEach(request::header);
            }

            // 设置请求参数
            if (testCase.getRequestParams() != null) {
                testCase.getRequestParams().forEach((k, v) -> request.param(k, v.toString()));
            }

            // 设置请求体
            if (testCase.getRequestBody() != null && !testCase.getRequestBody().isEmpty()) {
                request.body(testCase.getRequestBody());
            }

            // 执行请求
            Response response;
            String method = testCase.getMethod().toUpperCase();
            switch (method) {
                case "GET":
                    response = request.get(testCase.getApiPath());
                    break;
                case "POST":
                    response = request.post(testCase.getApiPath());
                    break;
                case "PUT":
                    response = request.put(testCase.getApiPath());
                    break;
                case "DELETE":
                    response = request.delete(testCase.getApiPath());
                    break;
                default:
                    throw new IllegalArgumentException("不支持的方法: " + method);
            }

            // 验证响应
            boolean passed = verifyResponse(response, testCase);

            return TestExecutionResult.builder()
                    .testCaseId(testCase.getTestCaseId())
                    .testName(testCase.getName())
                    .passed(passed)
                    .statusCode(response.getStatusCode())
                    .responseBody(response.getBody().asString())
                    .executionTime(response.getTime())
                    .errorMessage(passed ? null : "断言失败")
                    .build();

        } catch (Exception e) {
            log.error("测试执行异常: {}", e.getMessage());
            return TestExecutionResult.builder()
                    .testCaseId(testCase.getTestCaseId())
                    .testName(testCase.getName())
                    .passed(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 执行测试套件
     *
     * @param testCases 测试用例列表
     * @param baseUrl   服务基础URL
     * @return 测试执行报告
     */
    public TestSuiteReport executeTestSuite(List<TestCase> testCases, String baseUrl) {
        log.info("开始执行测试套件，共 {} 个测试用例", testCases.size());

        TestSuiteReport report = TestSuiteReport.builder()
                .suiteId("TS_" + IdUtil.fastSimpleUUID())
                .totalTests(testCases.size())
                .build();

        long startTime = System.currentTimeMillis();

        for (TestCase tc : testCases) {
            TestExecutionResult result = executeTestCase(tc, baseUrl);
            report.addResult(result);
        }

        long endTime = System.currentTimeMillis();
        report.setTotalDuration(endTime - startTime);
        report.calculateStatistics();

        log.info("测试套件执行完成: 通过 {} / 失败 {} / 耗时 {}ms",
                report.getPassedCount(), report.getFailedCount(), report.getTotalDuration());

        return report;
    }

    /**
     * 生成JUnit测试代码
     * 将测试用例转换为可执行的JUnit测试类代码
     *
     * @param testCases  测试用例列表
     * @param className  测试类名称
     * @return JUnit测试代码
     */
    public String generateJUnitCode(List<TestCase> testCases, String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.gaokao.test.generated;\n\n");
        sb.append("import io.restassured.RestAssured;\n");
        sb.append("import io.restassured.response.Response;\n");
        sb.append("import org.junit.jupiter.api.*;\n");
        sb.append("import static io.restassured.RestAssured.given;\n");
        sb.append("import static org.hamcrest.Matchers.*;\n\n");
        sb.append("@DisplayName(\"").append(className).append(" - AI Generated Tests\")\n");
        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("    private static final String BASE_URL = \"http://localhost:8088\";\n\n");

        for (TestCase tc : testCases) {
            sb.append(generateTestMethod(tc));
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 生成单个测试方法代码
     */
    private String generateTestMethod(TestCase tc) {
        StringBuilder sb = new StringBuilder();
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(tc.getDescription()).append("\")\n");
        sb.append("    @Tag(\"").append(tc.getTestType().name()).append("\")\n");
        sb.append("    public void test_").append(tc.getTestCaseId()).append("() {\n");

        // 构建请求
        sb.append("        Response response = given()\n");
        sb.append("                .baseUri(BASE_URL)\n");
        sb.append("                .contentType(\"application/json\")\n");

        if (tc.getHeaders() != null && !tc.getHeaders().isEmpty()) {
            sb.append("                .headers(").append(JSON.toJSONString(tc.getHeaders())).append(")\n");
        }

        if (tc.getRequestParams() != null && !tc.getRequestParams().isEmpty()) {
            sb.append("                .params(").append(JSON.toJSONString(tc.getRequestParams())).append(")\n");
        }

        if (tc.getRequestBody() != null && !tc.getRequestBody().isEmpty()) {
            sb.append("                .body(\"").append(escapeJson(tc.getRequestBody())).append("\")\n");
        }

        sb.append("                .when()\n");
        sb.append("                .").append(tc.getMethod().toLowerCase())
                .append("(\"").append(tc.getApiPath()).append("\")\n");

        sb.append("                .then()\n");
        sb.append("                .statusCode(").append(tc.getExpectedStatusCode()).append(")\n");

        // 添加断言
        if (tc.getAssertionRules() != null) {
            for (AssertionRule rule : tc.getAssertionRules()) {
                sb.append(generateAssertionCode(rule));
            }
        }

        sb.append("        ;\n");
        sb.append("    }\n\n");

        return sb.toString();
    }

    /**
     * 生成断言代码
     */
    private String generateAssertionCode(AssertionRule rule) {
        switch (rule.getType()) {
            case EXISTS:
                return "                .body(\"" + rule.getFieldPath() + "\", exists())\n";
            case NOT_NULL:
                return "                .body(\"" + rule.getFieldPath() + "\", notNullValue())\n";
            case EXACT_MATCH:
                return "                .body(\"" + rule.getFieldPath() + "\", equalTo(\"" +
                        rule.getExpectedValue() + "\"))\n";
            case TYPE_CHECK:
                return "                .body(\"" + rule.getFieldPath() + "\", isA(\"" +
                        rule.getExpectedValue() + "\"))\n";
            default:
                return "";
        }
    }

    /**
     * 补充测试用例信息
     */
    private void enrichTestCase(TestCase tc, String baseUrl) {
        // 可根据实际API文档补充更多细节
        if (tc.getHeaders() == null) {
            tc.setHeaders(new HashMap<>());
        }
        // 添加通用请求头
        tc.getHeaders().put("Accept", "application/json");
    }

    /**
     * 验证响应
     */
    private boolean verifyResponse(Response response, TestCase tc) {
        // 状态码验证
        if (response.getStatusCode() != tc.getExpectedStatusCode()) {
            return false;
        }

        // 执行断言规则验证
        if (tc.getAssertionRules() != null) {
            String responseBody = response.getBody().asString();
            JSONObject jsonResponse = JSON.parseObject(responseBody);

            for (AssertionRule rule : tc.getAssertionRules()) {
                if (!verifyAssertion(jsonResponse, rule)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 验证单个断言
     */
    private boolean verifyAssertion(JSONObject response, AssertionRule rule) {
        try {
            Object actualValue = getFieldValue(response, rule.getFieldPath());
            Object expectedValue = rule.getExpectedValue();

            AssertionRule.Condition condition = rule.getCondition();
            if (condition == AssertionRule.Condition.EQUALS) {
                return actualValue != null && actualValue.equals(expectedValue);
            } else if (condition == AssertionRule.Condition.CONTAINS) {
                return actualValue != null && actualValue.toString().contains(expectedValue.toString());
            } else if (condition == AssertionRule.Condition.MATCHES) {
                return actualValue != null && actualValue.toString().matches(expectedValue.toString());
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取字段值（支持嵌套路径）
     */
    private Object getFieldValue(JSONObject json, String path) {
        if (path == null || path.isEmpty()) return json;

        String[] parts = path.split("\\.");
        Object current = json;
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * 获取Swagger文档
     */
    private String fetchSwaggerDoc(String swaggerUrl) {
        // 如果是JSON内容，直接返回
        if (swaggerUrl.startsWith("{") || swaggerUrl.startsWith("[")) {
            return swaggerUrl;
        }

        // 从URL获取
        try {
            Response response = RestAssured.get(swaggerUrl);
            return response.getBody().asString();
        } catch (Exception e) {
            log.warn("无法从URL获取Swagger文档: {}", e.getMessage());
            return swaggerUrl;  // 可能传入的是文件路径
        }
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String json) {
        return json.replace("\"", "\\\"");
    }

    /**
     * 测试执行结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TestExecutionResult {
        private String testCaseId;
        private String testName;
        private boolean passed;
        private int statusCode;
        private String responseBody;
        private long executionTime;
        private String errorMessage;
    }

    /**
     * 测试套件执行报告
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TestSuiteReport {
        private String suiteId;
        private int totalTests;
        private int passedCount;
        private int failedCount;
        private int skippedCount;
        private long totalDuration;
        private java.util.List<TestExecutionResult> results = new java.util.ArrayList<>();

        public void addResult(TestExecutionResult result) {
            results.add(result);
            if (result.isPassed()) {
                passedCount++;
            } else {
                failedCount++;
            }
        }

        public void calculateStatistics() {
            passedCount = (int) results.stream().filter(TestExecutionResult::isPassed).count();
            failedCount = (int) results.stream().filter(r -> !r.isPassed()).count();
        }

        public double getPassRate() {
            return totalTests > 0 ? (double) passedCount / totalTests * 100 : 0;
        }
    }
}