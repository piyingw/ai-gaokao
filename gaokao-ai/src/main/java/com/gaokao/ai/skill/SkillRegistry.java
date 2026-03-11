package com.gaokao.ai.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 技能注册中心
 */
@Slf4j
@Component
public class SkillRegistry {
    private final Map<String, GaokaoSkill> skills = new ConcurrentHashMap<>();

    /**
     * 注册技能
     */
    public void registerSkill(GaokaoSkill skill) {
        if (skills.containsKey(skill.getName())) {
            log.warn("技能 {} 已存在，将被覆盖", skill.getName());
        }
        skills.put(skill.getName(), skill);
        log.info("已注册技能: {}", skill.getName());
    }

    /**
     * 获取技能
     */
    public GaokaoSkill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * 获取所有技能
     */
    public Collection<GaokaoSkill> getAllSkills() {
        return skills.values();
    }

    /**
     * 检查技能是否存在
     */
    public boolean hasSkill(String name) {
        return skills.containsKey(name);
    }

    /**
     * 移除技能
     */
    public void removeSkill(String name) {
        GaokaoSkill removed = skills.remove(name);
        if (removed != null) {
            log.info("已移除技能: {}", name);
        }
    }
}