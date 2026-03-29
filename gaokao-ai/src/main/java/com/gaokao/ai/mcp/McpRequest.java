package com.gaokao.ai.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP请求类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    private String method;      // 请求方法
    private String resource;    // 资源路径
    private Map<String, Object> params; // 请求参数
    private String protocolVersion; // 协议版本
}