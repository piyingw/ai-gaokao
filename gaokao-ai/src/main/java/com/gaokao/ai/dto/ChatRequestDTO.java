package com.gaokao.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对话请求 DTO
 */
@Data
public class ChatRequestDTO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户消息
     */
    @NotBlank(message = "消息不能为空")
    private String message;
}