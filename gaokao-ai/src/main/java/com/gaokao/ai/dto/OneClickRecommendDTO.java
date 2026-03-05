package com.gaokao.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 一键生成志愿请求 DTO
 */
@Data
public class OneClickRecommendDTO {

    /**
     * 会话ID（可选，用于多轮对话）
     */
    private String sessionId;

    /**
     * 高考分数
     */
    @NotNull(message = "高考分数不能为空")
    @Min(value = 0, message = "分数不能小于0")
    @Max(value = 750, message = "分数不能大于750")
    private Integer score;

    /**
     * 省份
     */
    @NotBlank(message = "省份不能为空")
    private String province;

    /**
     * 科类（物理类/历史类）
     */
    @NotBlank(message = "科类不能为空")
    private String subjectType;

    /**
     * 性格描述
     */
    @NotBlank(message = "性格描述不能为空")
    private String personalityDescription;

    /**
     * 意向专业（可选）
     */
    private String preferredMajors;

    /**
     * 意向城市（可选）
     */
    private String preferredCities;

    /**
     * 其他偏好说明（可选）
     */
    private String preference;
}