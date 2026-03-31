package com.gaokao.ai.config;

import com.gaokao.ai.mcp.McpClient;
import com.gaokao.ai.mcp.SimpleMcpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP (Model Context Protocol) 配置类
 */
@Configuration
public class McpConfig {

    @Bean
    @ConfigurationProperties(prefix = "gaokao.mcp")
    public McpProperties mcpProperties() {
        return new McpProperties();
    }

    @Bean
    public McpClient mcpClient(McpProperties mcpProperties) {
        // 这里可以根据配置创建不同的MCP客户端实现
        SimpleMcpClient client = new SimpleMcpClient();
        // 可以根据属性配置客户端
        return client;
    }

    /**
     * MCP配置属性
     */
    public static class McpProperties {
        private String serverUrl = "http://localhost:8081/mcp";
        private int connectionTimeout = 5000;
        private int readTimeout = 10000;
        private boolean enabled = true;

        // getters and setters
        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}