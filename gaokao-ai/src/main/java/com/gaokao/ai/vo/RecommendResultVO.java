package com.gaokao.ai.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 志愿推荐响应 VO
 */
@Data
public class RecommendResultVO {

    /**
     * 推荐方案 ID
     */
    private String planId;

    /**
     * 冲一冲志愿（录取概率 30%-50%）
     */
    private List<ApplicationItem> reachList;

    /**
     * 稳一稳志愿（录取概率 50%-70%）
     */
    private List<ApplicationItem> stableList;

    /**
     * 保一保志愿（录取概率 70%以上）
     */
    private List<ApplicationItem> ensureList;

    /**
     * AI 推荐理由
     */
    private String reason;

    /**
     * 志愿项
     */
    @Data
    public static class ApplicationItem {
        /**
         * 志愿序号
         */
        private Integer order;

        /**
         * 院校 ID
         */
        private Long universityId;

        /**
         * 院校名称
         */
        private String universityName;

        /**
         * 专业 ID
         */
        private Long majorId;

        /**
         * 专业名称
         */
        private String majorName;

        /**
         * 历年最低分
         */
        private Integer minScore;

        /**
         * 历年最低位次
         */
        private Integer minRank;

        /**
         * 预估录取概率
         */
        private Integer probability;

        /**
         * 推荐理由
         */
        private String reason;
    }
}