package com.gaokao.ai.config;

import com.gaokao.ai.skill.GaokaoSkill;
import com.gaokao.ai.skill.SkillRegistrar;
import com.gaokao.ai.skill.SkillRegistry;
import com.gaokao.ai.skill.impl.*;
import com.gaokao.data.mapper.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 技能配置类
 * 注册所有可用的技能
 */
@Configuration
public class SkillConfig {

    @Bean
    public UniversityQuerySkill universityQuerySkill(UniversityMapper universityMapper) {
        return new UniversityQuerySkill(universityMapper);
    }

    @Bean
    public ScoreAnalysisSkill scoreAnalysisSkill(AdmissionScoreMapper admissionScoreMapper) {
        return new ScoreAnalysisSkill(admissionScoreMapper);
    }

    @Bean
    public CalculatorSkill calculatorSkill() {
        return new CalculatorSkill();
    }

    @Bean
    public MajorQuerySkill majorQuerySkill(MajorMapper majorMapper) {
        return new MajorQuerySkill(majorMapper);
    }

    @Bean
    public List<GaokaoSkill> availableSkills(
            UniversityQuerySkill universityQuerySkill,
            ScoreAnalysisSkill scoreAnalysisSkill,
            CalculatorSkill calculatorSkill,
            MajorQuerySkill majorQuerySkill
    ) {
        return List.of(
                universityQuerySkill,
                scoreAnalysisSkill,
                calculatorSkill,
                majorQuerySkill
        );
    }

    @Bean
    public SkillRegistrar skillRegistrar(
            SkillRegistry skillRegistry,
            List<GaokaoSkill> availableSkills
    ) {
        return new SkillRegistrar(skillRegistry, availableSkills);
    }
}