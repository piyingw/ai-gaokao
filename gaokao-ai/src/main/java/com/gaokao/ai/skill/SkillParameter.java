package com.gaokao.ai.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能参数定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillParameter {
    private String name;        // 参数名
    private String type;        // 参数类型
    private String description; // 参数描述
    private boolean required;   // 是否必需
    private Object defaultValue; // 默认值
}