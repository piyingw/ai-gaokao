package com.gaokao.test.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI服务Bean配置
 * 将项目已有的ChatModel注入到测试模块
 */
@Configuration
public class AiServiceConfig {

    @Autowired(required = false)
    private ChatModel chatModel;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    /**
     * 创建AI服务提供者
     */
    @Bean
    public AiServiceProvider aiServiceProvider() {
        if (chatModel != null) {
            return new AiServiceProvider(chatModel);
        }
        // 如果没有配置ChatModel，返回一个空的实现（用于测试环境）
        return new AiServiceProvider(null);
    }
}