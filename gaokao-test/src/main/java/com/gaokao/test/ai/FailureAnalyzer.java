package com.gaokao.test.ai;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.test.config.TestAiConfig;
import com.gaokao.test.model.FailureReport;
import com.gaokao.test.config.AiServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试失败根因分析器
 * AI自动分析失败日志，定位根因并分类
 * 提供修复建议，加速问题解决
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailureAnalyzer {

    private final AiServiceProvider aiService;
    private final TestAiConfig testAiConfig;

    /**
     * 分析单个测试失败
     *
     * @param testName  测试名称
     * @param errorLog  错误日志
     * @param request   请求参数
     * @param response  实际响应
     * @return 失败分析报告
     */
    public FailureReport analyze(String testName, String errorLog,
                                 String request, String response) {
        log.info("开始分析测试失败: {}", testName);

        String prompt = buildAnalysisPrompt(testName, errorLog, request, response);
        String aiResponse = aiService.chat(prompt);

        FailureReport report = parseFailureReport(aiResponse, testName);
        report.setRequestSnapshot(request);
        report.setResponseSnapshot(response);
        report.setErrorLogSummary(extractErrorSummary(errorLog));

        return report;
    }

    /**
     * 批量分析多个失败测试
     *
     * @param failures 失败测试信息列表
     * @return 失败分析报告列表
     */
    public List<FailureReport> analyzeBatch(List<TestFailureInfo> failures) {
        log.info("批量分析 {} 个失败测试...", failures.size());

        List<FailureReport> reports = new ArrayList<>();
        for (TestFailureInfo failure : failures) {
            FailureReport report = analyze(failure.getTestName(), failure.getErrorLog(),
                    failure.getRequest(), failure.getResponse());
            reports.add(report);
        }

        return reports;
    }

    /**
     * 聚类分析失败测试
     * 将相似根因的失败测试归类，识别共性问题
     *
     * @param reports 失败报告列表
     * @return 聚类结果
     */
    public FailureClusterResult clusterFailures(List<FailureReport> reports) {
        if (reports.isEmpty()) {
            return FailureClusterResult.builder()
                    .totalFailures(0)
                    .clusters(new HashMap<>())
                    .build();
        }

        // 构建聚类分析Prompt
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下测试失败报告，将相似根因的失败归类，识别共性问题：\n\n");
        for (int i = 0; i < reports.size(); i++) {
            FailureReport r = reports.get(i);
            sb.append(String.format("失败%d: %s | 根因类型: %s | 描述: %s\n",
                    i + 1, r.getTestName(), r.getRootCauseType(), r.getRootCauseDescription()));
        }

        String prompt = sb + """

                请输出JSON格式的聚类结果：
                {
                  "clusters": {
                    "DATA_ISSUE": ["失败1", "失败3"],
                    "LOGIC_ISSUE": ["失败2"],
                    "ENVIRONMENT_ISSUE": ["失败4"]
                  },
                  "commonIssues": [
                    {
                      "type": "DATA_ISSUE",
                      "description": "共性问题描述",
                      "affectedTests": ["失败1", "失败3"],
                      "rootFix": "统一修复方案"
                    }
                  ],
                  "priority": "建议优先修复的类别",
                  "recommendation": "整体建议"
                }
                """;

        String response = aiService.chat(prompt);
        return parseClusterResult(response, reports);
    }

    /**
     * 自动生成缺陷报告描述
     * 基于失败分析生成可用于Jira/Bug系统的缺陷描述
     *
     * @param report 失败分析报告
     * @return 缺陷描述文本
     */
    public String generateBugDescription(FailureReport report) {
        String prompt = """
                请基于以下测试失败分析，生成规范的缺陷报告描述：

                测试名称：%s
                根因类型：%s
                根因描述：%s
                严重程度：%s
                建议修复：%s

                请生成包含以下部分的缺陷描述：
                1. 问题摘要（一句话）
                2. 问题详情（详细描述）
                3. 重现步骤
                4. 预期结果
                5. 实际结果
                6. 修复建议

                输出Markdown格式文本。
                """.formatted(report.getTestName(), report.getRootCauseType(),
                report.getRootCauseDescription(), report.getSeverity(),
                JSON.toJSONString(report.getSuggestedFixes()));

        return aiService.chat(prompt);
    }

    /**
     * 构建分析Prompt
     */
    private String buildAnalysisPrompt(String testName, String errorLog,
                                       String request, String response) {
        // 截取日志摘要，避免过长
        String logSummary = truncateLog(errorLog, 2000);
        String reqSummary = truncateLog(request, 500);
        String respSummary = truncateLog(response, 500);

        return """
                你是一位资深测试工程师，请分析以下测试失败信息，准确定位根因。

                测试名称：%s

                错误日志（摘要）：
                %s

                请求参数：
                %s

                实际响应：
                %s

                请进行以下分析：
                1. 根因类型分类：
                   - DATA_ISSUE：测试数据问题（数据缺失、状态异常、数据不一致）
                   - LOGIC_ISSUE：业务逻辑问题（条件判断错误、边界处理不当）
                   - ENVIRONMENT_ISSUE：环境问题（配置错误、服务不可用）
                   - NETWORK_ISSUE：网络问题（超时、连接失败）
                   - PARAMETER_ISSUE：参数问题（格式错误、缺失必填参数）
                   - DEPENDENCY_ISSUE：依赖问题（下游服务异常）
                   - TIMEOUT_ISSUE：超时问题
                   - UNKNOWN：无法确定

                2. 具体原因描述：用一两句话描述核心原因

                3. 建议修复方案：给出具体的修复建议

                4. 严重程度评估：
                   - P0：阻塞级，核心功能不可用
                   - P1：严重级，主要功能受损
                   - P2：一般级，次要功能异常
                   - P3：轻微级，不影响核心功能

                输出JSON格式（必须包含以下字段）：
                {
                  "rootCauseType": "DATA_ISSUE|LOGIC_ISSUE|...",
                  "rootCauseDescription": "具体原因描述",
                  "suggestedFixes": ["修复建议1", "修复建议2"],
                  "severity": "P0|P1|P2|P3",
                  "codeLocation": "可能的代码位置（如类名.方法名）",
                  "knownIssue": false,
                  "aiConfidence": 0.85
                }
                """.formatted(testName, logSummary, reqSummary, respSummary);
    }

    /**
     * 解析失败报告
     */
    private FailureReport parseFailureReport(String response, String testName) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            return FailureReport.builder()
                    .reportId("FR_" + IdUtil.fastSimpleUUID())
                    .testName(testName)
                    .rootCauseType(parseRootCauseType(obj.getString("rootCauseType")))
                    .rootCauseDescription(obj.getString("rootCauseDescription"))
                    .suggestedFixes(parseStringList(obj.getJSONArray("suggestedFixes")))
                    .severity(parseSeverity(obj.getString("severity")))
                    .codeLocation(obj.getString("codeLocation"))
                    .knownIssue(obj.getBooleanValue("knownIssue"))
                    .aiConfidence(obj.getDoubleValue("aiConfidence"))
                    .analyzeTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("解析失败报告失败: {}", e.getMessage());
            return FailureReport.builder()
                    .reportId("FR_" + IdUtil.fastSimpleUUID())
                    .testName(testName)
                    .rootCauseType(FailureReport.RootCauseType.UNKNOWN)
                    .rootCauseDescription("解析失败: " + e.getMessage())
                    .severity(FailureReport.Severity.P2)
                    .analyzeTime(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 解析聚类结果
     */
    private FailureClusterResult parseClusterResult(String response, List<FailureReport> reports) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            Map<String, List<String>> clusters = new HashMap<>();
            JSONObject clusterObj = obj.getJSONObject("clusters");
            if (clusterObj != null) {
                for (String key : clusterObj.keySet()) {
                    clusters.put(key, parseStringList(clusterObj.getJSONArray(key)));
                }
            }

            return FailureClusterResult.builder()
                    .totalFailures(reports.size())
                    .clusters(clusters)
                    .commonIssues(parseCommonIssues(obj.getJSONArray("commonIssues")))
                    .priority(obj.getString("priority"))
                    .recommendation(obj.getString("recommendation"))
                    .build();

        } catch (Exception e) {
            log.error("解析聚类结果失败: {}", e.getMessage());
            return FailureClusterResult.builder()
                    .totalFailures(reports.size())
                    .clusters(new HashMap<>())
                    .build();
        }
    }

    /**
     * 解析共性问题列表
     */
    private List<FailureClusterResult.CommonIssue> parseCommonIssues(JSONArray arr) {
        if (arr == null) return new ArrayList<>();

        List<FailureClusterResult.CommonIssue> issues = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            issues.add(FailureClusterResult.CommonIssue.builder()
                    .type(obj.getString("type"))
                    .description(obj.getString("description"))
                    .affectedTests(parseStringList(obj.getJSONArray("affectedTests")))
                    .rootFix(obj.getString("rootFix"))
                    .build());
        }
        return issues;
    }

    /**
     * 解析根因类型
     */
    private FailureReport.RootCauseType parseRootCauseType(String type) {
        try {
            return FailureReport.RootCauseType.valueOf(type);
        } catch (Exception e) {
            return FailureReport.RootCauseType.UNKNOWN;
        }
    }

    /**
     * 解析严重程度
     */
    private FailureReport.Severity parseSeverity(String severity) {
        try {
            return FailureReport.Severity.valueOf(severity);
        } catch (Exception e) {
            return FailureReport.Severity.P2;
        }
    }

    /**
     * 提取错误日志摘要
     */
    private String extractErrorSummary(String errorLog) {
        if (errorLog == null) return "";

        // 提取关键错误信息
        String[] lines = errorLog.split("\n");
        StringBuilder summary = new StringBuilder();
        for (String line : lines) {
            if (line.contains("ERROR") || line.contains("Exception") ||
                line.contains("Failed") || line.contains("failed")) {
                summary.append(line).append("\n");
                if (summary.length() > 500) break;
            }
        }
        return summary.toString();
    }

    /**
     * 截断日志避免过长
     */
    private String truncateLog(String log, int maxLength) {
        if (log == null) return "";
        if (log.length() <= maxLength) return log;
        return log.substring(0, maxLength) + "...(截断)";
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
        if (response.trim().startsWith("{")) {
            return response.trim();
        }
        return response;
    }

    private List<String> parseStringList(JSONArray arr) {
        if (arr == null) return new ArrayList<>();
        return arr.toList(String.class);
    }

    /**
     * 测试失败信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TestFailureInfo {
        private String testName;
        private String errorLog;
        private String request;
        private String response;
    }

    /**
     * 失败聚类结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FailureClusterResult {
        private int totalFailures;
        private Map<String, List<String>> clusters;
        private List<CommonIssue> commonIssues;
        private String priority;
        private String recommendation;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class CommonIssue {
            private String type;
            private String description;
            private List<String> affectedTests;
            private String rootFix;
        }
    }
}