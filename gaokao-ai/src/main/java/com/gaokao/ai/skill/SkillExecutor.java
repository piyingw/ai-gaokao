package com.gaokao.ai.skill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 技能执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillExecutor {
    private final SkillRegistry skillRegistry;

    /**
     * 执行指定名称的技能
     */
    public Object executeSkill(String skillName, Map<String, Object> params) {
        GaokaoSkill skill = skillRegistry.getSkill(skillName);
        if (skill == null) {
            log.error("未找到技能: {}", skillName);
            throw new IllegalArgumentException("未找到技能: " + skillName);
        }

        log.info("执行技能: {}, 参数: {}", skillName, params);
        try {
            Object result = skill.execute(params);
            log.info("技能执行完成: {}", skillName);
            return result;
        } catch (Exception e) {
            log.error("技能执行失败: " + skillName, e);
            throw new RuntimeException("技能执行失败: " + skillName, e);
        }
    }
}