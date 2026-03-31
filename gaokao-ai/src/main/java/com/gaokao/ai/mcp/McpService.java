package com.gaokao.ai.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCP服务实现
 * 提供通过MCP协议访问外部服务的能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpService {
    private final McpClient mcpClient;

    /**
     * 通过MCP协议调用外部服务
     */
    public Object callExternalService(String method, String resource, Map<String, Object> params) {
        log.info("通过MCP调用外部服务: method={}, resource={}", method, resource);

        if (!mcpClient.isConnected()) {
            log.warn("MCP客户端未连接，尝试重新连接...");
            mcpClient.connect();
        }

        McpRequest request = McpRequest.builder()
                .method(method)
                .resource(resource)
                .params(params)
                .protocolVersion("1.0")
                .build();

        Object response = mcpClient.sendRequest(request);

        log.info("MCP服务调用完成: resource={}", resource);
        return response;
    }

    /**
     * 获取MCP连接状态
     */
    public boolean isConnected() {
        return mcpClient.isConnected();
    }

    /**
     * 连接MCP服务
     */
    public void connect() {
        mcpClient.connect();
    }

    /**
     * 断开MCP连接
     */
    public void disconnect() {
        mcpClient.disconnect();
    }
}