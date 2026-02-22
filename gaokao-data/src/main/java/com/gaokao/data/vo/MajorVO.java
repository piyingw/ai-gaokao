package com.gaokao.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 专业列表 VO
 */
@Data
@Schema(description = "专业列表项")
public class MajorVO {

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

    @Schema(description = "就业前景评分（1-5）")
    private Integer employmentRating;

    @Schema(description = "平均薪资（元/月）")
    private Integer avgSalary;
}