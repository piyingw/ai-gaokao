package com.gaokao.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 政策文档 VO
 */
@Data
@Schema(description = "政策文档")
public class PolicyDocumentVO {

    @Schema(description = "文档ID")
    private Long id;

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "文档类型")
    private String type;

    @Schema(description = "适用省份")
    private String province;

    @Schema(description = "适用年份")
    private Integer year;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "关键词")
    private List<String> keywords;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "来源URL")
    private String sourceUrl;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}