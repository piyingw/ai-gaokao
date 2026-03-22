package com.gaokao.ai.memory;

import com.alibaba.fastjson2.JSON;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis 的对话记忆存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    
    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration EXPIRE_TIME = Duration.ofHours(24);

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = getKey(memoryId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return List.of();
        }
        return JSON.parseArray(json, ChatMessage.class);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = getKey(memoryId);
        String json = JSON.toJSONString(messages);
        redisTemplate.opsForValue().set(key, json, EXPIRE_TIME);
        log.debug("Updated chat memory for session: {}", memoryId);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = getKey(memoryId);
        redisTemplate.delete(key);
        log.debug("Deleted chat memory for session: {}", memoryId);
    }

    private String getKey(Object memoryId) {
        return KEY_PREFIX + memoryId.toString();
    }
}