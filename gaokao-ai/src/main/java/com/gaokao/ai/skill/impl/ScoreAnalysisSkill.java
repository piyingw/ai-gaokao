package com.gaokao.ai.skill.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.ai.skill.AbstractSkill;
import com.gaokao.ai.skill.SkillParameter;
import com.gaokao.data.entity.AdmissionScore;
import com.gaokao.data.mapper.AdmissionScoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 分数线分析技能
 */
@Slf4j
@Component
public class ScoreAnalysisSkill extends AbstractSkill {
    private final AdmissionScoreMapper admissionScoreMapper;

    public ScoreAnalysisSkill(AdmissionScoreMapper admissionScoreMapper) {
        super("score-analysis-skill", "分析高考分数线数据，包括历史分数线查询、分数匹配院校、竞争力分析");
        this.admissionScoreMapper = admissionScoreMapper;
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String operation = (String) params.getOrDefault("operation", "history");
        
        switch (operation) {
            case "history":
                return getScoreHistory(
                    (Long) params.get("universityId"),
                    (String) params.get("province"),
                    (String) params.get("subjectType"),
                    (Integer) params.get("years")
                );
            case "by-score":
                return queryByScore(
                    (String) params.get("province"),
                    (String) params.get("subjectType"),
                    (Integer) params.get("score"),
                    (Integer) params.get("range"),
                    (Integer) params.get("limit")
                );
            case "competitiveness":
                return analyzeScoreCompetitiveness(
                    (String) params.get("province"),
                    (String) params.get("subjectType"),
                    (Integer) params.get("score"),
                    (Long) params.get("universityId")
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
                .description("操作类型: history(历史分数线), by-score(按分数查询), competitiveness(竞争力分析)")
                .required(true)
                .defaultValue("history")
                .build(),
            SkillParameter.builder()
                .name("universityId")
                .type("long")
                .description("院校ID")
                .required(false)
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
                .name("years")
                .type("integer")
                .description("查询年数")
                .required(false)
                .defaultValue(3)
                .build(),
            SkillParameter.builder()
                .name("score")
                .type("integer")
                .description("考生成绩")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("range")
                .type("integer")
                .description("分数范围")
                .required(false)
                .defaultValue(30)
                .build(),
            SkillParameter.builder()
                .name("limit")
                .type("integer")
                .description("返回数量限制")
                .required(false)
                .defaultValue(20)
                .build()
        );
    }

    public String getScoreHistory(
            Long universityId,
            String province,
            String subjectType,
            Integer years
    ) {
        log.info("查询分数线历史: universityId={}, province={}, subjectType={}",
                universityId, province, subjectType);

        // 检查必要参数
        if (universityId == null) {
            return "错误：查询分数线历史需要提供universityId（院校ID）";
        }

        LambdaQueryWrapper<AdmissionScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmissionScore::getUniversityId, universityId);

        if (province != null && !province.isEmpty()) {
            wrapper.eq(AdmissionScore::getProvince, province);
        }
        if (subjectType != null && !subjectType.isEmpty()) {
            wrapper.eq(AdmissionScore::getSubjectType, subjectType);
        }

        wrapper.orderByDesc(AdmissionScore::getYear);
        wrapper.last("LIMIT " + (years != null ? years : 3));

        List<AdmissionScore> scores = admissionScoreMapper.selectList(wrapper);

        if (scores.isEmpty()) {
            return "数据库中未找到该院校的分数线数据。请确认院校ID、省份和科目类型是否正确。";
        }

        return scores.stream()
                .map(this::formatScore)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public String queryByScore(
            String province,
            String subjectType,
            Integer score,
            Integer range,
            Integer limit
    ) {
        log.info("按分数查询院校: province={}, subjectType={}, score={}",
                province, subjectType, score);

        int actualRange = range != null ? range : 30;
        int minScore = score - actualRange;
        int maxScore = score + actualRange;

        LambdaQueryWrapper<AdmissionScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmissionScore::getProvince, province)
                .eq(AdmissionScore::getSubjectType, subjectType)
                .eq(AdmissionScore::getYear, 2023)
                .between(AdmissionScore::getMinScore, minScore, maxScore)
                .orderByDesc(AdmissionScore::getMinScore)
                .last("LIMIT " + (limit != null ? limit : 20));

        List<AdmissionScore> scores = admissionScoreMapper.selectList(wrapper);

        if (scores.isEmpty()) {
            return String.format("未找到%d分左右可报考的院校，建议扩大搜索范围", score);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("为您找到%d分（±%d分）可报考的院校：\n\n", score, actualRange));

        for (AdmissionScore s : scores) {
            sb.append(String.format("- 院校ID：%d：最低分%d，最低位次%d\n",
                    s.getUniversityId(), s.getMinScore(), s.getMinRank()));
        }

        return sb.toString();
    }

    public String analyzeScoreCompetitiveness(
            String province,
            String subjectType,
            Integer score,
            Long universityId
    ) {
        log.info("分析分数竞争力: score={}, universityId={}", score, universityId);

        LambdaQueryWrapper<AdmissionScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmissionScore::getUniversityId, universityId)
                .eq(AdmissionScore::getProvince, province)
                .eq(AdmissionScore::getSubjectType, subjectType)
                .orderByDesc(AdmissionScore::getYear)
                .last("LIMIT 3");

        List<AdmissionScore> historyScores = admissionScoreMapper.selectList(wrapper);

        if (historyScores.isEmpty()) {
            return "未找到该院校的历年分数线数据，无法分析竞争力";
        }

        double avgScore = historyScores.stream()
                .mapToInt(AdmissionScore::getMinScore)
                .average()
                .orElse(0);

        int diff = score - (int) avgScore;
        String level;
        String suggestion;

        if (diff >= 20) {
            level = "保底";
            suggestion = "您的分数明显高于该校历年分数线，录取概率很高，建议作为保底院校。";
        } else if (diff >= 0) {
            level = "稳妥";
            suggestion = "您的分数略高于该校历年分数线，录取概率较大，可作为稳妥选择。";
        } else if (diff >= -20) {
            level = "冲刺";
            suggestion = "您的分数略低于该校历年分数线，有一定录取机会，可以冲刺尝试。";
        } else {
            level = "风险较大";
            suggestion = "您的分数明显低于该校历年分数线，录取难度较大，建议谨慎填报。";
        }

        return String.format("竞争力分析结果：%s\n\n您的分数：%d分\n该校历年平均最低分：%.0f分\n分差：%+d分\n\n建议：%s",
                level, score, avgScore, diff, suggestion);
    }

    private String formatScore(AdmissionScore s) {
        return String.format("%d年 - 最低分：%d，最低位次：%d，平均分：%s",
                s.getYear(),
                s.getMinScore(),
                s.getMinRank(),
                s.getAvgScore() != null ? s.getAvgScore().toString() : "暂无");
    }
}