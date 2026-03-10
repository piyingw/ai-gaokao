package com.gaokao.ai.skill;

import lombok.Getter;
import java.util.List;
import java.util.Map;

/**
 * 抽象技能基类
 */
@Getter
public abstract class AbstractSkill implements GaokaoSkill {
    protected final String name;
    protected final String description;

    public AbstractSkill(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}