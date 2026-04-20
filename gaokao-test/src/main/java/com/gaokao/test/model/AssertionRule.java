package com.gaokao.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI生成的断言规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssertionRule {

    /**
     * 断言ID
     */
    private String assertionId;

    /**
     * 断言类型
     */
    private AssertionType type;

    /**
     * 断言字段路径（JSON Path）
     */
    private String fieldPath;

    /**
     * 预期值
     */
    private Object expectedValue;

    /**
     * 实际值
     */
    private Object actualValue;

    /**
     * 断言条件
     */
    private Condition condition;

    /**
     * 语义相似度阈值（仅用于语义断言）
     */
    private Double similarityThreshold;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 断言类型枚举
     */
    public enum AssertionType {
        EXACT_MATCH,        // 精确匹配
        TYPE_CHECK,         // 类型检查
        EXISTS,             // 存在性检查
        NOT_NULL,           // 非空检查
        RANGE,              // 范围检查
        REGEX,              // 正则匹配
        SEMANTIC,           // 语义相似度（AI断言）
        SCHEMA,             // JSON Schema校验
        LIST_SIZE,          // 列表长度检查
        LIST_CONTAINS       // 列表包含检查
    }

    /**
     * 断言条件枚举
     */
    public enum Condition {
        EQUALS,             // 等于
        NOT_EQUALS,         // 不等于
        GREATER_THAN,       // 大于
        LESS_THAN,          // 小于
        GREATER_OR_EQUAL,   // 大于等于
        LESS_OR_EQUAL,      // 小于等于
        CONTAINS,           // 包含
        NOT_CONTAINS,       // 不包含
        MATCHES,            // 正则匹配
        SIMILAR_TO          // 语义相似
    }
}