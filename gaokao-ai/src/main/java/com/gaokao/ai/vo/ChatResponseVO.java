package com.gaokao.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseVO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 处理该请求的Agent名称
     */
    private String agentName;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}