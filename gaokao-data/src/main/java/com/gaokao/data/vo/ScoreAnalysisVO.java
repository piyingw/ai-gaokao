package com.gaokao.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分数分析响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAnalysisVO {

    /**
     * 院校 ID
     */
    private Long universityId;

    /**
     * 院校名称
     */
    private String universityName;

    /**
     * 省份
     */
    private String province;

    /**
     * 科类
     */
    private String subjectType;

    /**
     * 用户分数
     */
    private Integer userScore;

    /**
     * 历年平均最低分
     */
    private BigDecimal avgMinScore;

    /**
     * 历年最低分范围
     */
    private String scoreRange;

    /**
     * 分差（用户分数 - 平均最低分）
     */
    private Integer scoreDiff;

    /**
     * 竞争力等级（冲刺/稳妥/保底/风险较大）
     */
    private String competitivenessLevel;

    /**
     * 预估录取概率
     */
    private BigDecimal probability;

    /**
     * 历年分数线趋势
     */
    private List<YearScore> yearScores;

    /**
     * 推荐建议
     */
    private String suggestion;

    /**
     * 年度分数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearScore {
        /**
         * 年份
         */
        private Integer year;

        /**
         * 最低分
         */
        private Integer minScore;

        /**
         * 平均分
         */
        private BigDecimal avgScore;

        /**
         * 最高分
         */
        private Integer maxScore;

        /**
         * 最低位次
         */
        private Integer minRank;

        /**
         * 招生人数
         */
        private Integer enrollment;
    }
}