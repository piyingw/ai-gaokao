package com.gaokao.ai.skill.impl;

import com.gaokao.ai.skill.AbstractSkill;
import com.gaokao.ai.skill.SkillParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 计算技能
 */
@Slf4j
@Component
public class CalculatorSkill extends AbstractSkill {

    public CalculatorSkill() {
        super("calculator-skill", "提供多种计算功能，包括录取概率计算、志愿分配方案、位次换算、性价比评分等");
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String operation = (String) params.getOrDefault("operation", "admission-probability");
        
        switch (operation) {
            case "admission-probability":
                return calculateAdmissionProbability(
                    (Integer) params.get("score"),
                    (Integer) params.get("avgMinScore"),
                    (Integer) params.get("stdDev")
                );
            case "plan-distribution":
                return calculatePlanDistribution(
                    (Integer) params.get("totalCount"),
                    (Boolean) params.get("aggressive")
                );
            case "score-to-rank":
                return convertScoreToRank(
                    (String) params.get("province"),
                    (String) params.get("subjectType"),
                    (Integer) params.get("score")
                );
            case "cost-performance":
                return calculateCostPerformance(
                    (Integer) params.get("levelScore"),
                    (Integer) params.get("majorMatch"),
                    (Integer) params.get("cityPreference"),
                    (Integer) params.get("probability")
                );
            default:
                return "不支持的操作: " + operation;
        }
    }

    @Override
    public List<SkillParameter> getParameters() {
        return Arrays.asList(
            SkillParameter.builder()
                .name("operation")
                .type("string")
                .description("操作类型: admission-probability(录取概率), plan-distribution(计划分布), score-to-rank(分数转位次), cost-performance(性价比)")
                .required(true)
                .defaultValue("admission-probability")
                .build(),
            SkillParameter.builder()
                .name("score")
                .type("integer")
                .description("考生分数")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("avgMinScore")
                .type("integer")
                .description("院校历年平均最低分")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("stdDev")
                .type("integer")
                .description("标准差")
                .required(false)
                .defaultValue(10)
                .build(),
            SkillParameter.builder()
                .name("totalCount")
                .type("integer")
                .description("志愿总数")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("aggressive")
                .type("boolean")
                .description("是否采用激进策略")
                .required(false)
                .defaultValue(false)
                .build(),
            SkillParameter.builder()
                .name("province")
                .type("string")
                .description("省份")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("subjectType")
                .type("string")
                .description("科目类型")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("levelScore")
                .type("integer")
                .description("院校层次得分")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("majorMatch")
                .type("integer")
                .description("专业匹配度")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("cityPreference")
                .type("integer")
                .description("城市偏好度")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("probability")
                .type("integer")
                .description("录取概率")
                .required(false)
                .build()
        );
    }

    public String calculateAdmissionProbability(
            Integer score,
            Integer avgMinScore,
            Integer stdDev
    ) {
        log.info("计算录取概率: score={}, avgMinScore={}", score, avgMinScore);

        if (score == null || avgMinScore == null) {
            return "请提供考生分数和院校历年平均最低分";
        }

        int diff = score - avgMinScore;
        int sigma = stdDev != null ? stdDev : 10;

        double probability;
        String level;

        if (diff >= 2 * sigma) {
            probability = 0.95;
            level = "保底";
        } else if (diff >= sigma) {
            probability = 0.80;
            level = "稳妥";
        } else if (diff >= 0) {
            probability = 0.60;
            level = "较稳妥";
        } else if (diff >= -sigma) {
            probability = 0.40;
            level = "冲刺";
        } else if (diff >= -2 * sigma) {
            probability = 0.20;
            level = "风险较大";
        } else {
            probability = 0.05;
            level = "不建议填报";
        }

        return String.format("录取概率分析：\n您的分数：%d分\n院校平均最低分：%d分\n分差：%+d分\n预估录取概率：%.0f%%\n志愿类型：%s",
                score, avgMinScore, diff, probability * 100, level);
    }

    public String calculatePlanDistribution(Integer totalCount, Boolean aggressive) {
        log.info("计算志愿分配: total={}, aggressive={}", totalCount, aggressive);

        if (totalCount == null || totalCount < 3) {
            return "志愿总数至少为3个";
        }

        int reachCount, stableCount, ensureCount;

        if (Boolean.TRUE.equals(aggressive)) {
            reachCount = (int) (totalCount * 0.4);
            stableCount = (int) (totalCount * 0.35);
            ensureCount = totalCount - reachCount - stableCount;
        } else {
            reachCount = (int) (totalCount * 0.3);
            stableCount = (int) (totalCount * 0.4);
            ensureCount = totalCount - reachCount - stableCount;
        }

        return String.format("志愿分配建议（%s策略）：\n\n冲刺志愿：%d个（录取概率30%%-50%%）\n稳妥志愿：%d个（录取概率50%%-70%%）\n保底志愿：%d个（录取概率70%%以上）\n\n建议按照冲-稳-保顺序填报，确保录取成功率。",
                Boolean.TRUE.equals(aggressive) ? "激进" : "稳健",
                reachCount, stableCount, ensureCount);
    }

    public String convertScoreToRank(
            String province,
            String subjectType,
            Integer score
    ) {
        log.info("位次换算: province={}, subjectType={}, score={}", province, subjectType, score);

        int estimatedRank;

        if (score >= 680) {
            estimatedRank = (750 - score) * 10;
        } else if (score >= 600) {
            estimatedRank = (680 - score) * 100 + 700;
        } else if (score >= 500) {
            estimatedRank = (600 - score) * 500 + 8700;
        } else if (score >= 400) {
            estimatedRank = (500 - score) * 1000 + 58700;
        } else {
            estimatedRank = (400 - score) * 2000 + 158700;
        }

        return String.format("位次估算结果：\n省份：%s\n科类：%s\n分数：%d分\n预估位次：约%d名\n\n注：此为估算值，实际位次请以省教育考试院公布的一分一段表为准。",
                province, subjectType, score, estimatedRank);
    }

    public String calculateCostPerformance(
            Integer levelScore,
            Integer majorMatch,
            Integer cityPreference,
            Integer probability
    ) {
        log.info("计算性价比评分");

        double score = (levelScore * 0.3 + majorMatch * 0.3 + cityPreference * 0.2)
                * (probability / 100.0) * 10;

        String rating;
        if (score >= 8) {
            rating = "强烈推荐";
        } else if (score >= 6) {
            rating = "推荐填报";
        } else if (score >= 4) {
            rating = "可以考虑";
        } else {
            rating = "不推荐";
        }

        return String.format("志愿性价比分析：\n院校层次得分：%d/10\n专业匹配度：%d/10\n城市偏好度：%d/10\n录取概率：%d%%\n综合评分：%.1f/10\n推荐等级：%s",
                levelScore, majorMatch, cityPreference, probability, score, rating);
    }
}