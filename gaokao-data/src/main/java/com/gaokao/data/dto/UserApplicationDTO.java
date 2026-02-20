package com.gaokao.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户志愿 DTO
 */
@Data
@Schema(description = "用户志愿")
public class UserApplicationDTO {

    @Schema(description = "志愿ID（更新时必填）")
    private Long id;

    @Schema(description = "志愿方案名称")
    private String name;

    @Schema(description = "高考分数")
    private Integer score;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "科类（物理类/历史类/理科/文科）")
    private String subjectType;

    @Schema(description = "志愿列表")
    private List<ApplicationItem> applications;

    @Schema(description = "备注")
    private String remark;

    /**
     * 志愿项
     */
    @Data
    @Schema(description = "志愿项")
    public static class ApplicationItem {
        
        @Schema(description = "志愿序号（1-96）")
        private Integer order;
        
        @Schema(description = "院校ID")
        private Long universityId;
        
        @Schema(description = "院校名称")
        private String universityName;
        
        @Schema(description = "专业ID")
        private Long majorId;
        
        @Schema(description = "专业名称")
        private String majorName;
        
        @Schema(description = "志愿类型（冲/稳/保）")
        private String type;
        
        @Schema(description = "录取概率")
        private Double probability;
    }
}