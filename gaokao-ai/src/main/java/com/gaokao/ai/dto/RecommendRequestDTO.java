package com.gaokao.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI 志愿推荐请求 DTO
 */
@Data
public class RecommendRequestDTO {

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
     * 意向专业（可选）
     */
    private List<String> preferredMajors;

    /**
     * 意向城市（可选）
     */
    private List<String> preferredCities;

    /**
     * 意向院校层次（可选）
     */
    private List<String> preferredLevels;

    /**
     * 是否接受民办院校
     */
    private Boolean acceptPrivate = true;

    /**
     * 是否接受中外合作
     */
    private Boolean acceptJoint = true;

    /**
     * 志愿数量（默认 45 个）
     */
    @Min(value = 1, message = "志愿数量至少为1")
    @Max(value = 96, message = "志愿数量最多为96")
    private Integer count = 45;

    /**
     * 其他偏好说明
     */
    private String preference;
}