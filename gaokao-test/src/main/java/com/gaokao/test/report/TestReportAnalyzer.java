package com.gaokao.test.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.test.ai.FailureAnalyzer;
import com.gaokao.test.ai.RegressionAnalyzer;
import com.gaokao.test.generator.ApiTestCaseGenerator;
import com.gaokao.test.model.FailureReport;
import com.gaokao.test.config.AiServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 测试报告智能分析器
 * 分析测试结果，生成智能测试报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestReportAnalyzer {

    private final FailureAnalyzer failureAnalyzer;
    private final RegressionAnalyzer regressionAnalyzer;
    private final AiServiceProvider aiService;

    /**
     * 分析测试执行结果，生成智能报告
     *
     * @param testResults 测试执行结果列表
     * @return 智能测试报告
     */
    public IntelligentTestReport analyzeTestResults(
            List<ApiTestCaseGenerator.TestExecutionResult> testResults) {

        log.info("开始分析测试结果，共 {} 个测试", testResults.size());

        // 分类测试结果
        List<ApiTestCaseGenerator.TestExecutionResult> passedTests = testResults.stream()
                .filter(ApiTestCaseGenerator.TestExecutionResult::isPassed)
                .collect(Collectors.toList());

        List<ApiTestCaseGenerator.TestExecutionResult> failedTests = testResults.stream()
                .filter(r -> !r.isPassed())
                .collect(Collectors.toList());

        // 生成基础报告
        IntelligentTestReport report = IntelligentTestReport.builder()
                .reportId("TR_" + UUID.randomUUID().toString().substring(0, 8))
                .generateTime(LocalDateTime.now())
                .totalTests(testResults.size())
                .passedCount(passedTests.size())
                .failedCount(failedTests.size())
                .passRate((double) passedTests.size() / testResults.size() * 100)
                .build();

        // 分析失败测试
        if (!failedTests.isEmpty()) {
            analyzeFailures(report, failedTests);
        }

        // 生成改进建议
        generateImprovementSuggestions(report);

        // 生成风险分析
        analyzeRisk(report);

        return report;
    }

    /**
     * 分析失败测试
     */
    private void analyzeFailures(IntelligentTestReport report,
                                 List<ApiTestCaseGenerator.TestExecutionResult> failedTests) {
        List<FailureAnalyzer.TestFailureInfo> failureInfos = failedTests.stream()
                .map(r -> FailureAnalyzer.TestFailureInfo.builder()
                        .testName(r.getTestName())
                        .errorLog(r.getErrorMessage())
                        .response(r.getResponseBody())
                        .build())
                .collect(Collectors.toList());

        // 批量分析失败根因
        List<FailureReport> failureReports = failureAnalyzer.analyzeBatch(failureInfos);
        report.setFailureReports(failureReports);

        // 聚类分析
        FailureAnalyzer.FailureClusterResult clusterResult =
                failureAnalyzer.clusterFailures(failureReports);
        report.setClusterResult(clusterResult);

        // 统计根因分布
        Map<String, Integer> rootCauseDistribution = new HashMap<>();
        for (FailureReport fr : failureReports) {
            String type = fr.getRootCauseType().name();
            rootCauseDistribution.put(type, rootCauseDistribution.getOrDefault(type, 0) + 1);
        }
        report.setRootCauseDistribution(rootCauseDistribution);
    }

    /**
     * 生成改进建议
     */
    private void generateImprovementSuggestions(IntelligentTestReport report) {
        String prompt = """
                请基于以下测试结果，生成测试改进建议：

                测试总数：%d
                通过数：%d
                失败数：%d
                通过率：%.2f%%
                根因分布：%s

                请从以下维度给出建议：
                1. 测试覆盖率改进建议
                2. 失败测试修复优先级建议
                3. 测试用例质量改进建议
                4. 后续测试策略建议

                输出JSON格式：
                {
                  "coverageSuggestions": ["建议1", "建议2"],
                  "fixPriority": ["优先修复项"],
                  "testCaseQualitySuggestions": ["建议"],
                  "testStrategySuggestions": ["建议"],
                  "overallAssessment": "整体评价"
                }
                """.formatted(report.getTotalTests(), report.getPassedCount(),
                report.getFailedCount(), report.getPassRate(),
                JSON.toJSONString(report.getRootCauseDistribution()));

        String response = aiService.chat(prompt);
        parseSuggestions(report, response);
    }

    /**
     * 分析测试风险
     */
    private void analyzeRisk(IntelligentTestReport report) {
        // 基于失败率评估风险
        double failRate = report.getFailedCount() * 100.0 / report.getTotalTests();

        TestRiskLevel riskLevel;
        if (failRate >= 30) {
            riskLevel = TestRiskLevel.HIGH;
        } else if (failRate >= 15) {
            riskLevel = TestRiskLevel.MEDIUM;
        } else if (failRate >= 5) {
            riskLevel = TestRiskLevel.LOW;
        } else {
            riskLevel = TestRiskLevel.MINIMAL;
        }

        report.setRiskLevel(riskLevel);

        // 分析关键失败（P0/P1级别的失败）
        List<FailureReport> criticalFailures = report.getFailureReports().stream()
                .filter(fr -> fr.getSeverity() == FailureReport.Severity.P0 ||
                        fr.getSeverity() == FailureReport.Severity.P1)
                .collect(Collectors.toList());
        report.setCriticalFailures(criticalFailures);

        // 如果有严重失败，升级风险等级
        if (!criticalFailures.isEmpty() && riskLevel != TestRiskLevel.HIGH) {
            report.setRiskLevel(TestRiskLevel.HIGH);
            report.setRiskNote("存在" + criticalFailures.size() + "个严重级别(P0/P1)失败");
        }
    }

    /**
     * 解析改进建议
     */
    private void parseSuggestions(IntelligentTestReport report, String response) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            report.setCoverageSuggestions(parseStringList(obj.getJSONArray("coverageSuggestions")));
            report.setFixPriority(parseStringList(obj.getJSONArray("fixPriority")));
            report.setTestCaseQualitySuggestions(parseStringList(obj.getJSONArray("testCaseQualitySuggestions")));
            report.setTestStrategySuggestions(parseStringList(obj.getJSONArray("testStrategySuggestions")));
            report.setOverallAssessment(obj.getString("overallAssessment"));

        } catch (Exception e) {
            log.error("解析建议失败: {}", e.getMessage());
        }
    }

    /**
     * 生成测试摘要报告（用于团队汇报）
     *
     * @param report 详细测试报告
     * @return 摘要文本
     */
    public String generateSummaryReport(IntelligentTestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 测试执行报告摘要\n\n");
        sb.append("**执行时间**: ").append(report.getGenerateTime()).append("\n");
        sb.append("**测试总数**: ").append(report.getTotalTests()).append("\n");
        sb.append("**通过**: ").append(report.getPassedCount()).append(" | ");
        sb.append("**失败**: ").append(report.getFailedCount()).append("\n");
        sb.append("**通过率**: ").append(String.format("%.2f%%", report.getPassRate())).append("\n");
        sb.append("**风险等级**: ").append(report.getRiskLevel()).append("\n\n");

        if (report.getFailedCount() > 0) {
            sb.append("### 失败分析\n\n");
            sb.append("**根因分布**:\n");
            for (Map.Entry<String, Integer> entry : report.getRootCauseDistribution().entrySet()) {
                sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("个\n");
            }

            sb.append("\n**优先修复项**:\n");
            for (String item : report.getFixPriority()) {
                sb.append("- ").append(item).append("\n");
            }
        }

        sb.append("\n### 整体评价\n");
        sb.append(report.getOverallAssessment()).append("\n");

        return sb.toString();
    }

    /**
     * 生成缺陷追踪报告
     * 可用于对接缺陷管理系统
     *
     * @param report 测试报告
     * @return 缺陷追踪信息列表
     */
    public List<DefectTrackingInfo> generateDefectTrackingList(IntelligentTestReport report) {
        return report.getFailureReports().stream()
                .map(fr -> DefectTrackingInfo.builder()
                        .testName(fr.getTestName())
                        .rootCauseType(fr.getRootCauseType().name())
                        .severity(fr.getSeverity().name())
                        .description(failureAnalyzer.generateBugDescription(fr))
                        .suggestedFix(fr.getSuggestedFixes())
                        .build())
                .collect(Collectors.toList());
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

    private List<String> parseStringList(com.alibaba.fastjson2.JSONArray arr) {
        if (arr == null) return new ArrayList<>();
        return arr.toList(String.class);
    }

    /**
     * 测试风险等级
     */
    public enum TestRiskLevel {
        MINIMAL,  // 几乎无风险（失败率<5%）
        LOW,      // 低风险（失败率5-15%）
        MEDIUM,   // 中风险（失败率15-30%）
        HIGH      // 高风险（失败率>30%或有P0/P1失败）
    }

    /**
     * 智能测试报告
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IntelligentTestReport {
        private String reportId;
        private LocalDateTime generateTime;
        private int totalTests;
        private int passedCount;
        private int failedCount;
        private double passRate;
        private TestRiskLevel riskLevel;
        private String riskNote;

        private List<FailureReport> failureReports;
        private FailureAnalyzer.FailureClusterResult clusterResult;
        private Map<String, Integer> rootCauseDistribution;
        private List<FailureReport> criticalFailures;

        private List<String> coverageSuggestions;
        private List<String> fixPriority;
        private List<String> testCaseQualitySuggestions;
        private List<String> testStrategySuggestions;
        private String overallAssessment;
    }

    /**
     * 缺陷追踪信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DefectTrackingInfo {
        private String testName;
        private String rootCauseType;
        private String severity;
        private String description;
        private List<String> suggestedFix;
    }
}