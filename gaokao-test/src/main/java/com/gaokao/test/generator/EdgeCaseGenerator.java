package com.gaokao.test.generator;

import com.alibaba.fastjson2.JSON;
import com.gaokao.test.ai.TestCaseGenerator;
import com.gaokao.test.config.AiServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 边界值测试用例生成器
 * 智能分析字段约束，生成边界值测试数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeCaseGenerator {

    private final TestCaseGenerator aiGenerator;
    private final AiServiceProvider aiService;

    /**
     * 项目特定的字段约束定义
     * 高考志愿填报系统的业务边界值
     */
    private static final Map<String, FieldConstraint> BUSINESS_CONSTRAINTS = new HashMap<>();

    static {
        // 分数相关
        BUSINESS_CONSTRAINTS.put("score", FieldConstraint.builder()
                .fieldName("score").fieldType("Integer")
                .min(0).max(750).required(true)
                .description("高考分数").build());

        // 排名相关
        BUSINESS_CONSTRAINTS.put("rank", FieldConstraint.builder()
                .fieldName("rank").fieldType("Integer")
                .min(1).max(100000).required(true)
                .description("省内排名").build());

        // 会员等级
        BUSINESS_CONSTRAINTS.put("memberLevel", FieldConstraint.builder()
                .fieldName("memberLevel").fieldType("Integer")
                .min(0).max(2).required(false)
                .description("会员等级：0免费 1普通 2VIP").build());

        // 优惠券数量
        BUSINESS_CONSTRAINTS.put("couponCount", FieldConstraint.builder()
                .fieldName("couponCount").fieldType("Integer")
                .min(1).max(100).required(true)
                .description("优惠券数量").build());

        // 订单金额
        BUSINESS_CONSTRAINTS.put("amount", FieldConstraint.builder()
                .fieldName("amount").fieldType("BigDecimal")
                .min(0).max(9999.99).required(true)
                .description("订单金额").build());

        // 年份
        BUSINESS_CONSTRAINTS.put("year", FieldConstraint.builder()
                .fieldName("year").fieldType("Integer")
                .min(2020).max(2030).required(true)
                .description("年份").build());
    }

    /**
     * 为指定字段生成边界值测试数据
     *
     * @param fieldName 字段名称
     * @return 边界值测试数据列表
     */
    public List<BoundaryValue> generateBoundaryValues(String fieldName) {
        FieldConstraint constraint = BUSINESS_CONSTRAINTS.get(fieldName);
        if (constraint == null) {
            // 使用AI智能分析生成
            return generateWithAI(fieldName);
        }

        return generateFromConstraint(constraint);
    }

    /**
     * 为所有业务字段生成边界值测试集
     *
     * @return 所有字段的边界值测试数据
     */
    public Map<String, List<BoundaryValue>> generateAllBoundaryValues() {
        Map<String, List<BoundaryValue>> allValues = new HashMap<>();

        for (String fieldName : BUSINESS_CONSTRAINTS.keySet()) {
            allValues.put(fieldName, generateBoundaryValues(fieldName));
        }

        return allValues;
    }

    /**
     * 根据约束生成边界值
     */
    private List<BoundaryValue> generateFromConstraint(FieldConstraint constraint) {
        List<BoundaryValue> values = new ArrayList<>();

        String type = constraint.getFieldType();

        if ("Integer".equals(type) || "int".equals(type)) {
            // 数值边界
            if (constraint.getMin() != null) {
                int minVal = ((Number) constraint.getMin()).intValue();
                values.add(BoundaryValue.builder()
                        .value(minVal - 1)
                        .category("MIN_MINUS_1")
                        .description("最小值-1（预期失败）")
                        .expectedResult("FAIL").build());
                values.add(BoundaryValue.builder()
                        .value(minVal)
                        .category("MIN")
                        .description("最小值边界")
                        .expectedResult("PASS").build());
                values.add(BoundaryValue.builder()
                        .value(minVal + 1)
                        .category("MIN_PLUS_1")
                        .description("最小值+1")
                        .expectedResult("PASS").build());
            }

            if (constraint.getMax() != null) {
                int maxVal = ((Number) constraint.getMax()).intValue();
                values.add(BoundaryValue.builder()
                        .value(maxVal - 1)
                        .category("MAX_MINUS_1")
                        .description("最大值-1")
                        .expectedResult("PASS").build());
                values.add(BoundaryValue.builder()
                        .value(maxVal)
                        .category("MAX")
                        .description("最大值边界")
                        .expectedResult("PASS").build());
                values.add(BoundaryValue.builder()
                        .value(maxVal + 1)
                        .category("MAX_PLUS_1")
                        .description("最大值+1（预期失败）")
                        .expectedResult("FAIL").build());
            }

            // 特殊值
            values.add(BoundaryValue.builder()
                    .value(null)
                    .category("NULL")
                    .description("空值")
                    .expectedResult(constraint.isRequired() ? "FAIL" : "PASS").build());

            values.add(BoundaryValue.builder()
                    .value(-1)
                    .category("NEGATIVE")
                    .description("负数")
                    .expectedResult("FAIL").build());

            values.add(BoundaryValue.builder()
                    .value(0)
                    .category("ZERO")
                    .description("零值")
                    .expectedResult(constraint.getMin() != null && ((Number) constraint.getMin()).intValue() > 0 ? "FAIL" : "PASS")
                    .build());

        } else if ("BigDecimal".equals(type) || "Double".equals(type)) {
            // 浮点数边界
            if (constraint.getMin() != null) {
                values.add(BoundaryValue.builder()
                        .value(((Number) constraint.getMin()).doubleValue() - 0.01)
                        .category("MIN_MINUS")
                        .description("最小值-0.01")
                        .expectedResult("FAIL").build());
            }
            if (constraint.getMax() != null) {
                values.add(BoundaryValue.builder()
                        .value(((Number) constraint.getMax()).doubleValue() + 0.01)
                        .category("MAX_PLUS")
                        .description("最大值+0.01")
                        .expectedResult("FAIL").build());
            }
            // 金额特殊值
            values.add(BoundaryValue.builder()
                    .value(0.00)
                    .category("ZERO")
                    .description("零金额")
                    .expectedResult(constraint.getMin() != null && ((Number) constraint.getMin()).doubleValue() > 0 ? "FAIL" : "PASS")
                    .build());

        } else if ("String".equals(type)) {
            // 字符串边界
            values.add(BoundaryValue.builder()
                    .value("")
                    .category("EMPTY")
                    .description("空字符串")
                    .expectedResult(constraint.isRequired() ? "FAIL" : "PASS").build());

            values.add(BoundaryValue.builder()
                    .value(null)
                    .category("NULL")
                    .description("null值")
                    .expectedResult(constraint.isRequired() ? "FAIL" : "PASS").build());

            if (constraint.getMaxLength() != null) {
                // 超长字符串
                String longString = "a".repeat(constraint.getMaxLength() + 1);
                values.add(BoundaryValue.builder()
                        .value(longString)
                        .category("OVER_LENGTH")
                        .description("超长字符串")
                        .expectedResult("FAIL").build());
            }
        }

        return values;
    }

    /**
     * 使用AI生成未知字段的边界值
     */
    private List<BoundaryValue> generateWithAI(String fieldName) {
        String prompt = """
                请为字段 "%s" 生成边界值测试数据。

                请推测该字段可能的：
                1. 数据类型（Integer/String/BigDecimal等）
                2. 业务约束（范围、是否必填）
                3. 边界值测试场景

                输出JSON数组格式：
                [
                  {
                    "value": "边界值",
                    "category": "MIN|MAX|NULL|EMPTY|NEGATIVE等",
                    "description": "边界描述",
                    "expectedResult": "PASS|FAIL"
                  }
                ]
                """.formatted(fieldName);

        String response = aiService.chat(prompt);
        return parseBoundaryValues(response);
    }

    /**
     * 解析AI生成的边界值
     */
    private List<BoundaryValue> parseBoundaryValues(String response) {
        try {
            String jsonContent = extractJsonContent(response);
            List<BoundaryValue> values = new ArrayList<>();
            com.alibaba.fastjson2.JSONArray arr = JSON.parseArray(jsonContent);

            for (int i = 0; i < arr.size(); i++) {
                com.alibaba.fastjson2.JSONObject obj = arr.getJSONObject(i);
                values.add(BoundaryValue.builder()
                        .value(obj.get("value"))
                        .category(obj.getString("category"))
                        .description(obj.getString("description"))
                        .expectedResult(obj.getString("expectedResult"))
                        .build());
            }

            return values;
        } catch (Exception e) {
            log.error("解析边界值失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 生成组合边界值测试
     * 多个字段同时取边界值的组合场景
     *
     * @param fieldNames 参与组合的字段列表
     * @return 组合测试场景
     */
    public List<CombinationTestCase> generateCombinationTests(List<String> fieldNames) {
        List<CombinationTestCase> combinations = new ArrayList<>();

        // 生成两两组合的边界值测试
        for (int i = 0; i < fieldNames.size(); i++) {
            for (int j = i + 1; j < fieldNames.size(); j++) {
                String field1 = fieldNames.get(i);
                String field2 = fieldNames.get(j);

                List<BoundaryValue> values1 = generateBoundaryValues(field1);
                List<BoundaryValue> values2 = generateBoundaryValues(field2);

                // 选取边界值进行组合
                for (BoundaryValue v1 : values1) {
                    for (BoundaryValue v2 : values2) {
                        // 只组合预期失败的边界值，测试系统稳定性
                        if ("FAIL".equals(v1.getExpectedResult()) || "FAIL".equals(v2.getExpectedResult())) {
                            combinations.add(CombinationTestCase.builder()
                                    .fields(List.of(field1, field2))
                                    .values(Map.of(field1, v1.getValue(), field2, v2.getValue()))
                                    .description(field1 + "=" + v1.getDescription() + ", " +
                                            field2 + "=" + v2.getDescription())
                                    .expectedResult("FAIL")
                                    .build());
                        }
                    }
                }
            }
        }

        return combinations;
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
        if (response.trim().startsWith("[")) {
            return response.trim();
        }
        return response;
    }

    /**
     * 字段约束定义
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FieldConstraint {
        private String fieldName;
        private String fieldType;
        private Object min;
        private Object max;
        private Integer maxLength;
        private boolean required;
        private String description;
    }

    /**
     * 边界值测试数据
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BoundaryValue {
        private Object value;
        private String category;
        private String description;
        private String expectedResult;
    }

    /**
     * 组合测试场景
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CombinationTestCase {
        private List<String> fields;
        private Map<String, Object> values;
        private String description;
        private String expectedResult;
    }
}