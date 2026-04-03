package com.gaokao.ai.skill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 技能注册初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillRegistrar {
    private final SkillRegistry skillRegistry;
    private final List<GaokaoSkill> availableSkills;

    @PostConstruct
    public void registerSkills() {
        log.info("开始注册技能...");
        
        for (GaokaoSkill skill : availableSkills) {
            skillRegistry.registerSkill(skill);
        }
        
        log.info("技能注册完成，共注册 {} 个技能", availableSkills.size());
    }
}