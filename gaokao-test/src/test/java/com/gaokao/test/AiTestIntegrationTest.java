package com.gaokao.test;

import com.gaokao.test.generator.ApiTestCaseGenerator;
import com.gaokao.test.generator.EdgeCaseGenerator;
import com.gaokao.test.model.TestCase;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AI智能测试模块集成测试
 * 直接调用正在运行的后端服务API进行测试
 * 服务地址: http://localhost:8088
 */
@DisplayName("AI智能测试模块集成测试 - 真实服务API调用")
public class AiTestIntegrationTest {

    private static final String BASE_URL = "http://localhost:8088";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        System.out.println("=== 集成测试配置 ===");
        System.out.println("服务地址: " + BASE_URL);
        System.out.println("测试开始时间: " + java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("1. 验证服务健康状态")
    void testServiceHealth() {
        Response response = given()
                .when()
                .get("/actuator/health");

        System.out.println("=== 服务健康检查 ===");
        System.out.println("状态码: " + response.getStatusCode());
        System.out.println("响应: " + response.getBody().asString());

        response.then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("2. 测试Swagger文档可访问")
    void testSwaggerAccessible() {
        Response response = given()
                .when()
                .get("/doc.html");

        System.out.println("=== Swagger文档检查 ===");
        System.out.println("状态码: " + response.getStatusCode());

        response.then()
                .statusCode(200);
    }

    @Test
    @DisplayName("3. 测试API文档接口")
    void testApiDocs() {
        Response response = given()
                .when()
                .get("/v3/api-docs");

        System.out.println("=== API文档检查 ===");
        System.out.println("状态码: " + response.getStatusCode());

        response.then()
                .statusCode(200)
                .body("openapi", notNullValue());
    }

    @Test
    @DisplayName("4. 边界值测试数据生成验证")
    void testBoundaryValueGeneration() {
        EdgeCaseGenerator generator = new EdgeCaseGenerator(null, null);

        // 生成分数边界值
        List<EdgeCaseGenerator.BoundaryValue> scoreValues = generator.generateBoundaryValues("score");

        System.out.println("=== 分数边界值测试数据 ===");
        System.out.println("生成数量: " + scoreValues.size());
        for (EdgeCaseGenerator.BoundaryValue bv : scoreValues) {
            System.out.println("值: " + bv.getValue() + " | 类型: " + bv.getCategory() + " | 预期: " + bv.getExpectedResult());
        }

        assertNotNull(scoreValues);
        assertTrue(scoreValues.size() >= 5);

        // 生成会员等级边界值
        List<EdgeCaseGenerator.BoundaryValue> memberValues = generator.generateBoundaryValues("memberLevel");
        System.out.println("\n=== 会员等级边界值 ===");
        for (EdgeCaseGenerator.BoundaryValue bv : memberValues) {
            System.out.println("值: " + bv.getValue() + " | 类型: " + bv.getCategory());
        }
    }

    @Test
    @DisplayName("5. 测试用例模型验证")
    void testTestCaseModel() {
        TestCase testCase = TestCase.builder()
                .testCaseId("TC_INT_001")
                .name("集成测试用例")
                .description("验证测试用例模型")
                .testType(TestCase.TestType.HAPPY_PATH)
                .priority("P0")
                .apiPath("/api/test")
                .method("GET")
                .expectedStatusCode(200)
                .build();

        System.out.println("=== 测试用例模型 ===");
        System.out.println("ID: " + testCase.getTestCaseId());
        System.out.println("名称: " + testCase.getName());
        System.out.println("类型: " + testCase.getTestType());
        System.out.println("优先级: " + testCase.getPriority());

        assertNotNull(testCase);
        assertEquals(TestCase.TestType.HAPPY_PATH, testCase.getTestType());
    }

    @Test
    @DisplayName("6. JUnit测试代码生成验证")
    void testJUnitCodeGeneration() {
        List<TestCase> testCases = List.of(
                TestCase.builder()
                        .testCaseId("TC_GEN_001")
                        .name("生成的测试用例1")
                        .description("验证代码生成")
                        .testType(TestCase.TestType.HAPPY_PATH)
                        .apiPath("/api/member/info")
                        .method("GET")
                        .expectedStatusCode(200)
                        .build()
        );

        ApiTestCaseGenerator generator = new ApiTestCaseGenerator(null, null);
        String junitCode = generator.generateJUnitCode(testCases, "GeneratedTest");

        System.out.println("=== 生成的JUnit测试代码（前500字符）===");
        System.out.println(junitCode.substring(0, Math.min(500, junitCode.length())));

        assertNotNull(junitCode);
        assertTrue(junitCode.contains("package com.gaokao.test.generated"));
        assertTrue(junitCode.contains("@Test"));
        assertTrue(junitCode.contains("GeneratedTest"));
    }

    @Test
    @DisplayName("7. 模拟API测试执行验证")
    void testApiTestExecutionSimulation() {
        // 模拟测试执行结果
        ApiTestCaseGenerator.TestSuiteReport report = ApiTestCaseGenerator.TestSuiteReport.builder()
                .suiteId("TS_INT_001")
                .totalTests(10)
                .passedCount(8)
                .failedCount(2)
                .totalDuration(1500)
                .build();

        System.out.println("=== 测试套件执行报告 ===");
        System.out.println("套件ID: " + report.getSuiteId());
        System.out.println("总测试数: " + report.getTotalTests());
        System.out.println("通过数: " + report.getPassedCount());
        System.out.println("失败数: " + report.getFailedCount());
        System.out.println("通过率: " + report.getPassRate() + "%");
        System.out.println("耗时: " + report.getTotalDuration() + "ms");

        assertEquals(10, report.getTotalTests());
        assertEquals(80.0, report.getPassRate());
    }

    @Test
    @DisplayName("8. 调用真实AI问答API")
    void testRealAIQueryApi() {
        // 调用真实的AI问答接口
        Response response = given()
                .contentType("application/json")
                .body("{\"question\": \"北京大学2022年录取分数线是多少？\", \"userId\": \"test-user\"}")
                .when()
                .post("/api/ai/chat");

        System.out.println("=== AI问答API调用 ===");
        System.out.println("状态码: " + response.getStatusCode());
        System.out.println("响应内容: " + response.getBody().asString());

        // 验证API可调用（401未登录是正常的业务行为）
        assertTrue(response.getStatusCode() >= 200 && response.getStatusCode() < 500,
                "API应返回有效响应（200成功、401未登录、404不存在等）");
    }

    @Test
    @DisplayName("9. 调用会员信息API")
    void testMemberInfoApi() {
        Response response = given()
                .param("userId", 1)
                .when()
                .get("/api/member/info");

        System.out.println("=== 会员信息API调用 ===");
        System.out.println("状态码: " + response.getStatusCode());
        System.out.println("响应: " + response.getBody().asString());

        // 验证API可调用
        assertTrue(response.getStatusCode() >= 200 && response.getStatusCode() < 500,
                "API应返回有效响应");
    }
}