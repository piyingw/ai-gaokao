package com.gaokao.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 长期记忆实体
 * 用于存储用户的重要信息和对话历史
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LongTermMemory {

    /**
     * 记忆ID
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 记忆类型
     * USER_REQUESTED: 用户主动要求记住的信息
     * AUTO_EXTRACTED: 系统自动提取的信息
     */
    private MemoryType type;

    /**
     * 记忆内容
     */
    private String content;

    /**
     * 内容的向量表示
     */
    private List<Float> embedding;

    /**
     * 记忆标签（用于分类）
     */
    private List<String> tags;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 有效期（可选，用于临时记忆）
     */
    private LocalDateTime expireTime;

    /**
     * 记忆重要性评分（0-10）
     */
    private Integer importanceScore;

    public enum MemoryType {
        USER_REQUESTED,    // 用户主动要求记住
        AUTO_EXTRACTED     // 系统自动提取
    }
}