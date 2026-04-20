package com.gaokao.test.config;

import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * AI服务提供者 - 封装统一的调用接口
 * 使用项目已配置的 ChatModel
 */
@Slf4j
public class AiServiceProvider {
    private final ChatModel chatModel;

    public AiServiceProvider(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 发送消息并获取响应
     * @param prompt 提示文本
     * @return AI响应文本
     */
    public String chat(String prompt) {
        if (chatModel == null) {
            log.warn("ChatModel未配置，返回模拟响应");
            return "{\"message\": \"ChatModel未配置，请检查AI模块配置\"}";
        }
        // 使用 ChatModel.chat 方法
        return chatModel.chat(prompt);
    }
}