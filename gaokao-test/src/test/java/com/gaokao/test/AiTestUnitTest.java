package com.gaokao.test;

import com.gaokao.test.ai.AssertionAssistant;
import com.gaokao.test.ai.FailureAnalyzer;
import com.gaokao.test.ai.TestCaseGenerator;
import com.gaokao.test.generator.ApiTestCaseGenerator;
import com.gaokao.test.generator.EdgeCaseGenerator;
import com.gaokao.test.model.TestCase;
import com.gaokao.test.model.AssertionRule;
import com.gaokao.test.model.FailureReport;
import com.gaokao.test.config.AiServiceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AI测试模块单元测试
 * 使用Mock方式验证各组件逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AI智能测试模块单元测试")
public class AiTestUnitTest {

    @Mock
    private AiServiceProvider aiService;

    private TestCaseGenerator testCaseGenerator;
    private ApiTestCaseGenerator apiTestCaseGenerator;
    private EdgeCaseGenerator edgeCaseGenerator;
    private AssertionAssistant assertionAssistant;
    private FailureAnalyzer failureAnalyzer;

    @BeforeEach
    void setUp() {
        testCaseGenerator = new TestCaseGenerator(aiService, new com.gaokao.test.config.TestAiConfig());
        apiTestCaseGenerator = new ApiTestCaseGenerator(testCaseGenerator, aiService);
        edgeCaseGenerator = new EdgeCaseGenerator(testCaseGenerator, aiService);
        assertionAssistant = new AssertionAssistant(aiService, new com.gaokao.test.config.TestAiConfig());
        failureAnalyzer = new FailureAnalyzer(aiService, new com.gaokao.test.config.TestAiConfig());
    }

    @Test
    @DisplayName("1. 边界值测试数据生成")
    void testGenerateBoundaryValues() {
        // 生成分数字段的边界值（高考分数0-750）
        List<EdgeCaseGenerator.BoundaryValue> scoreValues = edgeCaseGenerator.generateBoundaryValues("score");

        assertNotNull(scoreValues);
        assertTrue(scoreValues.size() >= 5);  // 至少5个边界值

        System.out.println("分数字段边界值测试数据:");
        for (EdgeCaseGenerator.BoundaryValue bv : scoreValues) {
            System.out.println("值: " + bv.getValue() + " | 类型: " + bv.getCategory() + " | 预期: " + bv.getExpectedResult());
        }
    }

    @Test
    @DisplayName("2. 会员等级边界值生成")
    void testGenerateMemberLevelBoundaryValues() {
        List<EdgeCaseGenerator.BoundaryValue> memberValues = edgeCaseGenerator.generateBoundaryValues("memberLevel");

        assertNotNull(memberValues);
        assertTrue(memberValues.size() >= 3);

        System.out.println("\n会员等级边界值测试数据:");
        for (EdgeCaseGenerator.BoundaryValue bv : memberValues) {
            System.out.println("值: " + bv.getValue() + " | 类型: " + bv.getCategory() + " | 预期: " + bv.getExpectedResult());
        }
    }

    @Test
    @DisplayName("3. Mock AI生成测试用例")
    void testMockGenerateTestCases() {
        // Mock AI响应
        String mockResponse = """
                ```json
                [
                  {
                    "testCaseId": "TC_001",
                    "name": "正常查询会员信息",
                    "description": "验证会员信息查询接口正常返回",
                    "testType": "HAPPY_PATH",
                    "priority": "P0",
                    "apiPath": "/api/member/info",
                    "method": "GET",
                    "expectedStatusCode": 200
                  }
                ]
                ```
                """;
        when(aiService.chat(anyString())).thenReturn(mockResponse);

        // 测试生成
        String swaggerSnippet = "{\"paths\": {\"/api/member/info\": {\"get\": {}}}}";
        List<TestCase> testCases = testCaseGenerator.generateFromSwagger(swaggerSnippet);

        assertNotNull(testCases);
        assertTrue(testCases.size() >= 1);

        TestCase tc = testCases.get(0);
        assertEquals("TC_001", tc.getTestCaseId());
        assertEquals("正常查询会员信息", tc.getName());
        assertEquals(TestCase.TestType.HAPPY_PATH, tc.getTestType());

        System.out.println("Mock生成的测试用例数量: " + testCases.size());
    }

    @Test
    @DisplayName("4. Mock AI断言规则生成")
    void testMockGenerateAssertionRules() {
        String mockResponse = """
                ```json
                [
                  {
                    "assertionId": "AS_001",
                    "type": "EXISTS",
                    "fieldPath": "data.userId",
                    "condition": "EQUALS",
                    "errorMessage": "userId字段不存在"
                  }
                ]
                ```
                """;
        when(aiService.chat(anyString())).thenReturn(mockResponse);

        String mockApiResponse = "{\"success\": true, \"data\": {\"userId\": 1001}}";
        List<AssertionRule> rules = assertionAssistant.generateAssertions(mockApiResponse);

        assertNotNull(rules);
        assertTrue(rules.size() >= 1);

        AssertionRule rule = rules.get(0);
        assertEquals("AS_001", rule.getAssertionId());
        assertEquals(AssertionRule.AssertionType.EXISTS, rule.getType());

        System.out.println("Mock生成的断言规则数量: " + rules.size());
    }

    @Test
    @DisplayName("5. Mock失败根因分析")
    void testMockFailureAnalysis() {
        String mockResponse = """
                ```json
                {
                  "rootCauseType": "DATA_ISSUE",
                  "rootCauseDescription": "测试数据缺失，会员不存在",
                  "suggestedFixes": ["创建测试会员数据", "检查数据库连接"],
                  "severity": "P2",
                  "aiConfidence": 0.85
                }
                ```
                """;
        when(aiService.chat(anyString())).thenReturn(mockResponse);

        FailureReport report = failureAnalyzer.analyze(
                "会员查询测试",
                "AssertionError: Expected userId but was null",
                "{}",
                "{\"success\": false}"
        );

        assertNotNull(report);
        assertEquals(FailureReport.RootCauseType.DATA_ISSUE, report.getRootCauseType());
        assertEquals("P2", report.getSeverity().name());
        assertEquals(0.85, report.getAiConfidence());

        System.out.println("Mock分析的根因类型: " + report.getRootCauseType());
        System.out.println("Mock分析的严重程度: " + report.getSeverity());
    }

    @Test
    @DisplayName("7. 测试用例模型验证")
    void testTestCaseModel() {
        TestCase testCase = TestCase.builder()
                .testCaseId("TC_TEST_001")
                .name("测试用例")
                .description("测试描述")
                .testType(TestCase.TestType.HAPPY_PATH)
                .priority("P0")
                .apiPath("/api/test")
                .method("GET")
                .expectedStatusCode(200)
                .build();

        assertNotNull(testCase);
        assertEquals("TC_TEST_001", testCase.getTestCaseId());
        assertEquals(TestCase.TestType.HAPPY_PATH, testCase.getTestType());

        // 验证枚举类型
        TestCase.TestType[] types = TestCase.TestType.values();
        assertTrue(types.length >= 4);  // HAPPY_PATH, BOUNDARY, EXCEPTION, SECURITY
    }

    @Test
    @DisplayName("8. 失败报告模型验证")
    void testFailureReportModel() {
        FailureReport report = FailureReport.builder()
                .reportId("FR_TEST_001")
                .testName("测试失败")
                .rootCauseType(FailureReport.RootCauseType.LOGIC_ISSUE)
                .rootCauseDescription("业务逻辑错误")
                .severity(FailureReport.Severity.P1)
                .aiConfidence(0.90)
                .build();

        assertNotNull(report);
        assertEquals(FailureReport.RootCauseType.LOGIC_ISSUE, report.getRootCauseType());
        assertEquals(FailureReport.Severity.P1, report.getSeverity());

        // 验证枚举类型
        FailureReport.RootCauseType[] causeTypes = FailureReport.RootCauseType.values();
        assertTrue(causeTypes.length >= 6);  // DATA_ISSUE, LOGIC_ISSUE, etc.
    }
}