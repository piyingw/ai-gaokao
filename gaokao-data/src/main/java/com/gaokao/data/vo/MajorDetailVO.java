package com.gaokao.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 专业详情 VO
 */
@Data
@Schema(description = "专业详情")
public class MajorDetailVO {

    @Schema(description = "专业ID")
    private Long id;

    @Schema(description = "专业名称")
    private String name;

    @Schema(description = "专业代码")
    private String code;

    @Schema(description = "学科门类")
    private String category;

    @Schema(description = "专业类")
    private String subCategory;

    @Schema(description = "学位类型")
    private String degreeType;

    @Schema(description = "学制（年）")
    private Integer duration;

    @Schema(description = "专业简介")
    private String intro;

    @Schema(description = "主要课程")
    private List<String> courses;

    @Schema(description = "就业方向")
    private List<String> employment;

    @Schema(description = "就业前景评分（1-5）")
    private Integer employmentRating;

    @Schema(description = "平均薪资（元/月）")
    private Integer avgSalary;

    @Schema(description = "男女比例")
    private String genderRatio;

    @Schema(description = "选科要求")
    private Object subjectRequirement;

    @Schema(description = "开设院校数量")
    private Integer universityCount;
}