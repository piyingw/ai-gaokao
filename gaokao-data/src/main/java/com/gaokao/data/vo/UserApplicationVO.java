package com.gaokao.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户志愿 VO
 */
@Data
@Schema(description = "用户志愿")
public class UserApplicationVO {

    @Schema(description = "志愿ID")
    private Long id;

    @Schema(description = "志愿方案名称")
    private String name;

    @Schema(description = "高考分数")
    private Integer score;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "科类")
    private String subjectType;

    @Schema(description = "志愿列表")
    private List<ApplicationItem> applications;

    @Schema(description = "状态（0-草稿 1-已提交）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 志愿项
     */
    @Data
    @Schema(description = "志愿项")
    public static class ApplicationItem {
        
        @Schema(description = "志愿序号")
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