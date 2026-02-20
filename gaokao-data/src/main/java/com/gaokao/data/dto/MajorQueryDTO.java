package com.gaokao.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 专业查询 DTO
 */
@Data
@Schema(description = "专业查询条件")
public class MajorQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "20")
    private Integer pageSize = 20;

    @Schema(description = "专业名称（模糊查询）")
    private String name;

    @Schema(description = "专业代码")
    private String code;

    @Schema(description = "学科门类（工学/理学/文学等）")
    private String category;

    @Schema(description = "专业类（计算机类/电子信息类等）")
    private String subCategory;

    @Schema(description = "学位类型")
    private String degreeType;

    @Schema(description = "院校ID（查询该校开设的专业）")
    private Long universityId;
}