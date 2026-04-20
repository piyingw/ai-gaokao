package com.gaokao.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试失败分析报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureReport {

    /**
     * 报告ID
     */
    private String reportId;

    /**
     * 失败的测试名称
     */
    private String testName;

    /**
     * 根因类型
     */
    private RootCauseType rootCauseType;

    /**
     * 具体原因描述
     */
    private String rootCauseDescription;

    /**
     * 错误日志摘要
     */
    private String errorLogSummary;

    /**
     * 请求参数
     */
    private String requestSnapshot;

    /**
     * 实际响应
     */
    private String responseSnapshot;

    /**
     * 建议修复方案
     */
    private List<String> suggestedFixes;

    /**
     * 严重程度
     */
    private Severity severity;

    /**
     * 相关代码位置
     */
    private String codeLocation;

    /**
     * 是否为已知问题
     */
    private boolean knownIssue;

    /**
     * 关联的缺陷ID
     */
    private String relatedBugId;

    /**
     * 分析时间
     */
    private LocalDateTime analyzeTime;

    /**
     * AI置信度（0-1）
     */
    private double aiConfidence;

    /**
     * 根因类型枚举
     */
    public enum RootCauseType {
        DATA_ISSUE,         // 数据问题：测试数据缺失、数据状态异常
        LOGIC_ISSUE,        // 逻辑问题：业务逻辑错误、边界条件处理不当
        ENVIRONMENT_ISSUE,  // 环境问题：配置错误、服务不可用
        NETWORK_ISSUE,      // 网络问题：超时、连接失败
        PARAMETER_ISSUE,    // 参数问题：参数格式错误、缺失必要参数
        DEPENDENCY_ISSUE,   // 依赖问题：下游服务异常、第三方接口错误
        TIMEOUT_ISSUE,      // 超时问题：响应超时、数据库超时
        UNKNOWN             // 未知原因
    }

    /**
     * 严重程度枚举
     */
    public enum Severity {
        P0,  // 阻塞级：核心功能不可用
        P1,  // 严重：主要功能受损
        P2,  // 一般：次要功能异常
        P3   // 轻微：UI/体验问题
    }
}