package com.gaokao.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 回归测试计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegressionPlan {

    /**
     * 计划ID
     */
    private String planId;

    /**
     * 分析的Git Diff内容
     */
    private String analyzedDiff;

    /**
     * 直接修改的接口列表
     */
    private List<AffectedApi> directAffectedApis;

    /**
     * 间接影响的接口列表（调用链分析）
     */
    private List<AffectedApi> indirectAffectedApis;

    /**
     * 需要执行的测试用例列表
     */
    private List<String> requiredTestCases;

    /**
     * 测试优先级排序
     */
    private List<PriorityTest> prioritizedTests;

    /**
     * 预估测试时间（分钟）
     */
    private int estimatedMinutes;

    /**
     * 分析摘要
     */
    private String summary;

    /**
     * 受影响API信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AffectedApi {
        private String apiPath;
        private String method;
        private String reason;
        private int callDepth;  // 调用深度
        private String modifiedMethod;
    }

    /**
     * 优先级测试项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityTest {
        private String testCaseId;
        private String testName;
        private int priority;  // 1-10
        private String reason;
    }
}