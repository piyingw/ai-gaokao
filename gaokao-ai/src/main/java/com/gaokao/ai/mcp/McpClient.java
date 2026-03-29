package com.gaokao.ai.mcp;

/**
 * MCP客户端接口
 */
public interface McpClient {
    /**
     * 发送请求到MCP服务器
     */
    Object sendRequest(McpRequest request);

    /**
     * 连接到MCP服务器
     */
    void connect();

    /**
     * 断开与MCP服务器的连接
     */
    void disconnect();

    /**
     * 检查连接状态
     */
    boolean isConnected();
}