package com.gaokao.ai.tool;

import com.gaokao.ai.skill.SkillExecutor;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能调用工具
 * 使现有Agent能够通过Tool调用新实现的Skill
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillTool {

    private final SkillExecutor skillExecutor;
    private final DataValidationTool dataValidationTool;
    private final LongTermMemoryTool longTermMemoryTool;

    @Tool("调用指定名称的技能")
    public String executeSkill(String skillName, String paramsJson) {
        log.info("通过Tool调用技能: name={}, params={}", skillName, paramsJson);

        try {
            // 将JSON字符串转换为Map
            Map<String, Object> params = parseJsonToMap(paramsJson);

            Object result = skillExecutor.executeSkill(skillName, params);
            return result != null ? result.toString() : "技能执行返回空结果";
        } catch (Exception e) {
            log.error("技能调用失败: " + skillName, e);
            return "技能调用失败: " + e.getMessage();
        }
    }

    @Tool("验证院校信息是否存在")
    public String validateUniversityExists(Long universityId, String universityName) {
        return dataValidationTool.validateUniversityExists(universityId, universityName);
    }

    @Tool("验证分数线数据是否存在")
    public String validateScoreExists(Long universityId, String province, String subjectType, Integer year) {
        return dataValidationTool.validateScoreExists(universityId, province, subjectType, year);
    }

    @Tool("记住用户提供的信息")
    public String rememberInfo(String userId, String content) {
        return longTermMemoryTool.rememberInfo(userId, content);
    }

    @Tool("自动提取并记住对话中的关键信息")
    public String autoExtractAndRemember(String userId, String content) {
        return longTermMemoryTool.autoExtractAndRemember(userId, content);
    }

    /**
     * 简单的JSON解析方法（实际项目中应使用Jackson或Gson等库）
     */
    private Map<String, Object> parseJsonToMap(String jsonStr) {
        // 这是一个简化的JSON解析实现
        // 在实际项目中，应使用Jackson或Gson等库进行完整的JSON解析
        Map<String, Object> map = new HashMap<>();

        if (jsonStr == null || jsonStr.trim().isEmpty() || !jsonStr.startsWith("{") || !jsonStr.endsWith("}")) {
            return map;
        }

        // 移除首尾的大括号
        String content = jsonStr.trim().substring(1, jsonStr.length() - 1);

        // 按逗号分割键值对
        String[] pairs = content.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("\"", "");
                String value = keyValue[1].trim().replaceAll("\"", "");

                // 尝试将值转换为适当的类型
                Object convertedValue = convertValue(value);
                map.put(key, convertedValue);
            }
        }

        return map;
    }

    /**
     * 尝试将字符串值转换为适当的类型
     */
    private Object convertValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.valueOf(value);
        }

        try {
            // 尝试转换为整数
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 如果不是整数，返回原始字符串
            return value;
        }
    }
}