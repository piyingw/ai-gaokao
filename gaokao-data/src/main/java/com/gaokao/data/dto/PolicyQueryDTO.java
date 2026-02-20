package com.gaokao.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 政策文档查询 DTO
 */
@Data
@Schema(description = "政策文档查询条件")
public class PolicyQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "关键词搜索")
    private String keyword;

    @Schema(description = "文档类型")
    private String type;

    @Schema(description = "适用省份")
    private String province;

    @Schema(description = "适用年份")
    private Integer year;
}