package com.gaokao.ai.skill;

import java.util.List;
import java.util.Map;

/**
 * 技能接口
 */
public interface GaokaoSkill {
    /**
     * 获取技能名称
     */
    String getName();

    /**
     * 获取技能描述
     */
    String getDescription();

    /**
     * 执行技能
     */
    Object execute(Map<String, Object> params);

    /**
     * 获取参数定义
     */
    List<SkillParameter> getParameters();
}