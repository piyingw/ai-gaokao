package com.gaokao.ai.config;

import com.gaokao.ai.mcp.McpClient;
import com.gaokao.ai.mcp.SimpleMcpClient;
import com.gaokao.ai.skill.*;
import com.gaokao.ai.skill.impl.*;
import com.gaokao.ai.tool.SkillTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 使用 OpenAI 兼容模式接入阿里通义千问
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai")
public class LangChain4jConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API Base URL (通义千问 OpenAI 兼容端点)
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * 聊天模型配置
     */
    private ChatModelConfig chatModel = new ChatModelConfig();

    /**
     * Embedding 模型配置
     */
    private EmbeddingModelConfig embeddingModel = new EmbeddingModelConfig();

    @Data
    public static class ChatModelConfig {
        private String modelName = "qwen-plus";
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
    }

    @Data
    public static class EmbeddingModelConfig {
        private String modelName = "text-embedding-v3";
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModel.getModelName())
                .build();
    }

    // 技能相关 Bean
    @Bean
    public SkillRegistry skillRegistry() {
        return new SkillRegistry();
    }

    @Bean
    public SkillExecutor skillExecutor(SkillRegistry skillRegistry) {
        return new SkillExecutor(skillRegistry);
    }

    // SkillTool、DataValidationTool、LongTermMemoryTool 使用 @Component 自动注入
    // 不需要在此手动创建 Bean，避免依赖缺失

    // MCP 客户端由 McpConfig 统一管理，避免 Bean 冲突
}
