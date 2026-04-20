package com.gaokao.test.ai;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.test.config.TestAiConfig;
import com.gaokao.test.model.RegressionPlan;
import com.gaokao.test.config.AiServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能回归测试分析器
 * 基于代码变更分析生成精准回归测试方案
 * 避免全量回归的资源浪费
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegressionAnalyzer {

    private final AiServiceProvider aiService;
    private final TestAiConfig testAiConfig;

    /**
     * 分析Git Diff，生成回归测试计划
     *
     * @param gitDiff Git diff内容
     * @return 回归测试计划
     */
    public RegressionPlan analyzeDiff(String gitDiff) {
        log.info("开始分析Git Diff，生成回归测试计划...");

        String prompt = buildDiffAnalysisPrompt(gitDiff);
        String response = aiService.chat(prompt);

        return parseRegressionPlan(response, gitDiff);
    }

    /**
     * 分析代码变更，识别受影响的API
     *
     * @param changedFiles 变更文件列表
     * @param changeDetails 变更详情（包含每个文件的diff）
     * @return 受影响的API列表
     */
    public RegressionPlan analyzeChanges(List<String> changedFiles, String changeDetails) {
        log.info("分析 {} 个变更文件...", changedFiles.size());

        String prompt = """
                你是一位资深测试工程师和代码分析师，请分析以下代码变更，识别受影响的API接口。

                变更文件列表：%s
                变更详情：%s

                请进行以下分析：
                1. 直接修改的API：哪些Controller接口被直接修改
                2. 间接影响的API：哪些API调用了被修改的Service/Repository层代码
                3. 调用链分析：从Controller向下追踪，识别完整调用链中的变更点
                4. 优先级评估：根据变更范围和重要性确定回归优先级

                输出JSON格式：
                {
                  "planId": "RP_xxx",
                  "directAffectedApis": [
                    {
                      "apiPath": "/api/xxx",
                      "method": "GET|POST",
                      "reason": "修改原因",
                      "modifiedMethod": "修改的方法名"
                    }
                  ],
                  "indirectAffectedApis": [
                    {
                      "apiPath": "/api/xxx",
                      "method": "GET|POST",
                      "reason": "调用链影响原因",
                      "callDepth": 2,
                      "modifiedMethod": "源头修改的方法名"
                    }
                  ],
                  "requiredTestCases": ["需要执行的测试用例ID列表"],
                  "prioritizedTests": [
                    {
                      "testCaseId": "TC_xxx",
                      "testName": "测试名称",
                      "priority": 1-10,
                      "reason": "优先级原因"
                    }
                  ],
                  "estimatedMinutes": 预估测试时长,
                  "summary": "分析摘要"
                }
                """.formatted(JSON.toJSONString(changedFiles), changeDetails);

        String response = aiService.chat(prompt);
        return parseRegressionPlan(response, changeDetails);
    }

    /**
     * 智能选择回归测试用例
     * 从现有测试用例池中筛选需要执行的测试
     *
     * @param allTestCases 全部测试用例
     * @param regressionPlan 回归计划
     * @return 需要执行的测试用例列表
     */
    public List<String> selectRegressionTests(List<String> allTestCases, RegressionPlan regressionPlan) {
        // 合并直接和间接影响的API相关测试
        List<String> selectedTests = new ArrayList<>();

        // 直接影响的API测试优先级最高
        for (RegressionPlan.AffectedApi api : regressionPlan.getDirectAffectedApis()) {
            String apiTestPattern = api.getApiPath().replace("/", "_");
            // 筛选与该API相关的测试用例
            allTestCases.stream()
                    .filter(tc -> tc.contains(apiTestPattern))
                    .forEach(selectedTests::add);
        }

        // 间接影响的API测试次优先
        for (RegressionPlan.AffectedApi api : regressionPlan.getIndirectAffectedApis()) {
            String apiTestPattern = api.getApiPath().replace("/", "_");
            allTestCases.stream()
                    .filter(tc -> tc.contains(apiTestPattern))
                    .filter(tc -> !selectedTests.contains(tc))  // 避免重复
                    .forEach(selectedTests::add);
        }

        log.info("回归测试筛选完成：从 {} 个测试用例中选出 {} 个",
                allTestCases.size(), selectedTests.size());

        return selectedTests;
    }

    /**
     * 代码变更风险评估
     * 评估变更可能带来的风险等级
     *
     * @param gitDiff Git diff内容
     * @return 风险评估结果
     */
    public RiskAssessment assessRisk(String gitDiff) {
        String prompt = """
                请评估以下代码变更的风险等级：

                Git Diff：%s

                请从以下维度评估风险：
                1. 变更范围：核心业务 vs 边缘功能
                2. 变改类型：新增功能 vs 修改现有功能 vs 修复Bug
                3. 影响面：用户可见功能 vs 内部逻辑
                4. 数据安全：是否涉及数据结构变更
                5. 性能影响：是否可能影响性能

                输出JSON格式：
                {
                  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                  "riskScore": 1-10,
                  "riskFactors": ["风险因素列表"],
                  "recommendations": ["测试建议"],
                  "shouldFullRegression": true/false
                }
                """.formatted(gitDiff);

        String response = aiService.chat(prompt);
        return parseRiskAssessment(response);
    }

    /**
     * 构建Diff分析Prompt
     */
    private String buildDiffAnalysisPrompt(String gitDiff) {
        return """
                你是一位资深测试工程师和代码分析师，请分析以下Git Diff内容，
                识别代码变更对API接口的影响，生成精准的回归测试计划。

                Git Diff内容：
                %s

                请进行以下分析：
                1. 识别所有被修改的Java类和方法
                2. 判断这些修改是否影响API接口：
                   - Controller层修改直接影响API
                   - Service层修改可能间接影响多个API
                   - Repository/Mapper修改影响数据层
                3. 分析调用链关系：
                   - 找出调用被修改方法的上层方法
                   - 追踪到Controller层确定最终影响的API
                4. 根据业务重要性设置测试优先级

                输出JSON格式（必须包含以下字段）：
                {
                  "planId": "RP_xxx",
                  "directAffectedApis": [
                    {
                      "apiPath": "/api/xxx",
                      "method": "GET|POST|PUT|DELETE",
                      "reason": "该接口被直接修改",
                      "modifiedMethod": "方法名"
                    }
                  ],
                  "indirectAffectedApis": [
                    {
                      "apiPath": "/api/xxx",
                      "method": "GET|POST",
                      "reason": "调用了被修改的Service方法",
                      "callDepth": 2,
                      "modifiedMethod": "源头修改的方法"
                    }
                  ],
                  "requiredTestCases": ["TC_001", "TC_002"],
                  "prioritizedTests": [
                    {
                      "testCaseId": "TC_xxx",
                      "testName": "测试名称",
                      "priority": 1,
                      "reason": "核心接口变更"
                    }
                  ],
                  "estimatedMinutes": 30,
                  "summary": "本次变更影响X个API，建议执行Y个测试"
                }

                注意：
                1. 如果diff中没有Controller相关修改，不要虚构API路径
                2. 根据实际diff内容如实分析
                3. 如果变更不影响任何API，返回空列表并说明原因
                """.formatted(gitDiff);
    }

    /**
     * 解析回归测试计划
     */
    private RegressionPlan parseRegressionPlan(String response, String gitDiff) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            RegressionPlan plan = RegressionPlan.builder()
                    .planId(obj.getString("planId"))
                    .analyzedDiff(gitDiff)
                    .directAffectedApis(parseAffectedApis(obj.getJSONArray("directAffectedApis")))
                    .indirectAffectedApis(parseAffectedApis(obj.getJSONArray("indirectAffectedApis")))
                    .requiredTestCases(parseStringList(obj.getJSONArray("requiredTestCases")))
                    .prioritizedTests(parsePrioritizedTests(obj.getJSONArray("prioritizedTests")))
                    .estimatedMinutes(obj.getIntValue("estimatedMinutes"))
                    .summary(obj.getString("summary"))
                    .build();

            if (plan.getPlanId() == null || plan.getPlanId().isEmpty()) {
                plan.setPlanId("RP_" + IdUtil.fastSimpleUUID());
            }

            log.info("回归分析完成：直接影响 {} 个API，间接影响 {} 个API",
                    plan.getDirectAffectedApis().size(),
                    plan.getIndirectAffectedApis().size());

            return plan;

        } catch (Exception e) {
            log.error("解析回归计划失败: {}", e.getMessage());
            return RegressionPlan.builder()
                    .planId("RP_" + IdUtil.fastSimpleUUID())
                    .analyzedDiff(gitDiff)
                    .summary("解析失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析受影响API列表
     */
    private List<RegressionPlan.AffectedApi> parseAffectedApis(JSONArray arr) {
        if (arr == null) return new ArrayList<>();

        List<RegressionPlan.AffectedApi> apis = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            RegressionPlan.AffectedApi api = RegressionPlan.AffectedApi.builder()
                    .apiPath(obj.getString("apiPath"))
                    .method(obj.getString("method"))
                    .reason(obj.getString("reason"))
                    .callDepth(obj.getIntValue("callDepth"))
                    .modifiedMethod(obj.getString("modifiedMethod"))
                    .build();
            apis.add(api);
        }
        return apis;
    }

    /**
     * 解析优先级测试列表
     */
    private List<RegressionPlan.PriorityTest> parsePrioritizedTests(JSONArray arr) {
        if (arr == null) return new ArrayList<>();

        List<RegressionPlan.PriorityTest> tests = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            RegressionPlan.PriorityTest test = RegressionPlan.PriorityTest.builder()
                    .testCaseId(obj.getString("testCaseId"))
                    .testName(obj.getString("testName"))
                    .priority(obj.getIntValue("priority"))
                    .reason(obj.getString("reason"))
                    .build();
            tests.add(test);
        }
        return tests;
    }

    /**
     * 解析风险评估结果
     */
    private RiskAssessment parseRiskAssessment(String response) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            return RiskAssessment.builder()
                    .riskLevel(RiskLevel.valueOf(obj.getString("riskLevel")))
                    .riskScore(obj.getIntValue("riskScore"))
                    .riskFactors(parseStringList(obj.getJSONArray("riskFactors")))
                    .recommendations(parseStringList(obj.getJSONArray("recommendations")))
                    .shouldFullRegression(obj.getBooleanValue("shouldFullRegression"))
                    .build();

        } catch (Exception e) {
            log.error("解析风险评估失败: {}", e.getMessage());
            return RiskAssessment.builder()
                    .riskLevel(RiskLevel.MEDIUM)
                    .riskScore(5)
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
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * 风险评估结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RiskAssessment {
        private RiskLevel riskLevel;
        private int riskScore;
        private List<String> riskFactors;
        private List<String> recommendations;
        private boolean shouldFullRegression;
    }
}