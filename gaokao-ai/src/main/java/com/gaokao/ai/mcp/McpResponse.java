package com.gaokao.ai.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpResponse {
    private int statusCode;                 // 状态码
    private String message;                 // 响应消息
    private Map<String, Object> data;       // 响应数据
    private String protocolVersion;         // 协议版本
    private boolean success;               // 是否成功
}