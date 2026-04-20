package com.gaokao.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI生成的测试用例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    /**
     * 测试用例ID
     */
    private String testCaseId;

    /**
     * 测试名称
     */
    private String name;

    /**
     * 测试描述
     */
    private String description;

    /**
     * 测试类型：HAPPY_PATH(正向), BOUNDARY(边界值), EXCEPTION(异常), SECURITY(安全)
     */
    private TestType testType;

    /**
     * 优先级：P0-P3
     */
    private String priority;

    /**
     * 目标API路径
     */
    private String apiPath;

    /**
     * HTTP方法
     */
    private String method;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 请求参数
     */
    private Map<String, Object> requestParams;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 预期响应状态码
     */
    private int expectedStatusCode;

    /**
     * 预期响应体
     */
    private String expectedResponse;

    /**
     * 断言规则列表
     */
    private List<AssertionRule> assertionRules;

    /**
     * 前置条件
     */
    private List<String> preconditions;

    /**
     * 后置清理
     */
    private List<String> postCleanup;

    /**
     * 标签（用于分类）
     */
    private List<String> tags;

    /**
     * 测试类型枚举
     */
    public enum TestType {
        HAPPY_PATH,     // 正向测试
        BOUNDARY,       // 边界值测试
        EXCEPTION,      // 异常输入测试
        SECURITY,       // 安全性测试
        PERFORMANCE,    // 性能测试
        INTEGRATION     // 集成测试
    }
}