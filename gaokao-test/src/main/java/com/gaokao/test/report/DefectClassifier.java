package com.gaokao.test.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gaokao.test.config.AiServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缺陷自动分类器
 * AI自动将缺陷归类，便于团队分配和处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefectClassifier {

    private final AiServiceProvider aiService;

    /**
     * 分类单个缺陷
     *
     * @param defectInfo 缺陷信息（标题、描述、错误日志等）
     * @return 分类结果
     */
    public DefectClassification classify(DefectInfo defectInfo) {
        String prompt = """
                请对以下缺陷进行分类：

                缺陷标题：%s
                缺陷描述：%s
                错误日志：%s
                影响范围：%s

                请从以下维度进行分类：
                1. 缺陷类型：功能缺陷 | 数据缺陷 | UI缺陷 | 性能缺陷 | 安全缺陷 | 兼容性缺陷
                2. 严重程度：P0(阻塞) | P1(严重) | P2(一般) | P3(轻微)
                3. 优先级：立即修复 | 本周修复 | 下周修复 | 低优先级
                4. 负责团队：后端团队 | 前端团队 | 数据团队 | 运维团队 | AI团队
                5. 修复预估：小时数估计

                输出JSON格式：
                {
                  "defectType": "功能缺陷",
                  "severity": "P1",
                  "priority": "本周修复",
                  "responsibleTeam": "后端团队",
                  "estimatedFixHours": 4,
                  "classificationReason": "分类原因说明",
                  "relatedComponents": ["涉及组件"],
                  "similarDefects": ["可能相似的已知缺陷"]
                }
                """.formatted(defectInfo.getTitle(), defectInfo.getDescription(),
                defectInfo.getErrorLog(), defectInfo.getImpactScope());

        String response = aiService.chat(prompt);
        return parseClassification(response);
    }

    /**
     * 批量分类缺陷
     *
     * @param defectInfos 缺陷信息列表
     * @return 分类结果列表
     */
    public List<DefectClassification> classifyBatch(List<DefectInfo> defectInfos) {
        return defectInfos.stream()
                .map(this::classify)
                .collect(Collectors.toList());
    }

    /**
     * 缺陷聚类分析
     * 将相似缺陷归类，识别批量修复机会
     *
     * @param classifications 分类结果列表
     * @return 聚类分析结果
     */
    public DefectClusterAnalysis clusterAnalysis(List<DefectClassification> classifications) {
        // 按类型统计
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> severityCount = new HashMap<>();
        Map<String, Integer> teamCount = new HashMap<>();

        for (DefectClassification dc : classifications) {
            typeCount.merge(dc.getDefectType(), 1, Integer::sum);
            severityCount.merge(dc.getSeverity(), 1, Integer::sum);
            teamCount.merge(dc.getResponsibleTeam(), 1, Integer::sum);
        }

        // 找出最常见的问题类型
        String mostCommonType = typeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知");

        // 找出严重问题
        int criticalCount = severityCount.getOrDefault("P0", 0) +
                severityCount.getOrDefault("P1", 0);

        // 生成修复建议
        List<String> fixSuggestions = generateFixSuggestions(classifications);

        return DefectClusterAnalysis.builder()
                .totalDefects(classifications.size())
                .typeDistribution(typeCount)
                .severityDistribution(severityCount)
                .teamDistribution(teamCount)
                .mostCommonType(mostCommonType)
                .criticalDefectCount(criticalCount)
                .fixSuggestions(fixSuggestions)
                .build();
    }

    /**
     * 生成批量修复建议
     */
    private List<String> generateFixSuggestions(List<DefectClassification> classifications) {
        // 根据聚类结果生成修复策略建议
        List<String> suggestions = new ArrayList<>();

        // 按团队分组
        Map<String, List<DefectClassification>> byTeam = classifications.stream()
                .collect(Collectors.groupingBy(DefectClassification::getResponsibleTeam));

        for (Map.Entry<String, List<DefectClassification>> entry : byTeam.entrySet()) {
            String team = entry.getKey();
            int count = entry.getValue().size();
            int totalHours = entry.getValue().stream()
                    .mapToInt(DefectClassification::getEstimatedFixHours)
                    .sum();

            suggestions.add(String.format("%s: %d个缺陷，预估修复%d小时", team, count, totalHours));
        }

        // 添加优先处理建议
        List<DefectClassification> critical = classifications.stream()
                .filter(dc -> "P0".equals(dc.getSeverity()) || "P1".equals(dc.getSeverity()))
                .collect(Collectors.toList());

        if (!critical.isEmpty()) {
            suggestions.add("优先处理: " + critical.size() + "个P0/P1级别缺陷需立即处理");
        }

        return suggestions;
    }

    /**
     * 解析分类结果
     */
    private DefectClassification parseClassification(String response) {
        try {
            String jsonContent = extractJsonContent(response);
            JSONObject obj = JSON.parseObject(jsonContent);

            return DefectClassification.builder()
                    .defectType(obj.getString("defectType"))
                    .severity(obj.getString("severity"))
                    .priority(obj.getString("priority"))
                    .responsibleTeam(obj.getString("responsibleTeam"))
                    .estimatedFixHours(obj.getIntValue("estimatedFixHours"))
                    .classificationReason(obj.getString("classificationReason"))
                    .relatedComponents(parseStringList(obj.getJSONArray("relatedComponents")))
                    .similarDefects(parseStringList(obj.getJSONArray("similarDefects")))
                    .build();

        } catch (Exception e) {
            log.error("解析分类结果失败: {}", e.getMessage());
            return DefectClassification.builder()
                    .defectType("未知")
                    .severity("P2")
                    .priority("下周修复")
                    .responsibleTeam("待定")
                    .classificationReason("解析失败")
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

    private List<String> parseStringList(com.alibaba.fastjson2.JSONArray arr) {
        if (arr == null) return new ArrayList<>();
        return arr.toList(String.class);
    }

    /**
     * 缺陷信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DefectInfo {
        private String title;
        private String description;
        private String errorLog;
        private String impactScope;
        private String reporter;
        private String environment;
    }

    /**
     * 缺陷分类结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DefectClassification {
        private String defectType;
        private String severity;
        private String priority;
        private String responsibleTeam;
        private int estimatedFixHours;
        private String classificationReason;
        private List<String> relatedComponents;
        private List<String> similarDefects;
    }

    /**
     * 缺陷聚类分析结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DefectClusterAnalysis {
        private int totalDefects;
        private Map<String, Integer> typeDistribution;
        private Map<String, Integer> severityDistribution;
        private Map<String, Integer> teamDistribution;
        private String mostCommonType;
        private int criticalDefectCount;
        private List<String> fixSuggestions;
    }
}