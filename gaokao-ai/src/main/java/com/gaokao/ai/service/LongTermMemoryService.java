package com.gaokao.ai.service;

import com.gaokao.ai.entity.LongTermMemory;
import com.gaokao.ai.store.ElasticsearchLongTermMemoryStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 长期记忆管理服务
 * 负责触发记忆存储和管理记忆生命周期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemoryService {

    private final ElasticsearchLongTermMemoryStore memoryStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 用户主动请求记住信息
     */
    public void rememberRequestedByUser(String userId, String content) {
        try {
            // 生成向量嵌入
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(content).content();
            List<Float> embeddingList = embedding.vectorAsList();

            // 创建记忆对象
            LongTermMemory memory = LongTermMemory.builder()
                    .id(generateMemoryId(userId, System.currentTimeMillis()))
                    .userId(userId)
                    .content(content)
                    .type(LongTermMemory.MemoryType.USER_REQUESTED)
                    .embedding(embeddingList)
                    .tags(extractTags(content))
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .importanceScore(8) // 用户主动记住的信息通常比较重要
                    .build();

            // 存储记忆
            memoryStore.storeMemory(memory);
            
            log.info("用户 {} 主动记住信息: {}", userId, content);
        } catch (IOException e) {
            log.error("存储用户主动记忆失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 自动从对话中提取关键信息
     */
    public void autoExtractAndRemember(String userId, String content) {
        try {
            // 检查内容是否包含需要提取的关键信息
            List<String> extractedInfo = extractKeyInformation(content);
            
            if (!extractedInfo.isEmpty()) {
                for (String info : extractedInfo) {
                    // 生成向量嵌入
                    dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(info).content();
                    List<Float> embeddingList = embedding.vectorAsList();

                    // 创建记忆对象
                    LongTermMemory memory = LongTermMemory.builder()
                            .id(generateMemoryId(userId, System.currentTimeMillis() + extractedInfo.indexOf(info)))
                            .userId(userId)
                            .content(info)
                            .type(LongTermMemory.MemoryType.AUTO_EXTRACTED)
                            .embedding(embeddingList)
                            .tags(Arrays.asList("auto-extracted", "key-info"))
                            .createTime(LocalDateTime.now())
                            .updateTime(LocalDateTime.now())
                            .importanceScore(calculateImportanceScore(info))
                            .build();

                    // 存储记忆
                    memoryStore.storeMemory(memory);
                    
                    log.info("自动提取并记住信息: {}", info);
                }
            }
        } catch (IOException e) {
            log.error("自动提取记忆失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从文本中提取关键信息
     */
    private List<String> extractKeyInformation(String text) {
        // 定义正则表达式来匹配关键信息
        Pattern scorePattern = Pattern.compile("(\\d{2,3})分");
        Pattern locationPattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,6})(省|市|自治区|特别行政区)");
        Pattern subjectPattern = Pattern.compile("(文科|理科|新高考|物理类|历史类|综合类)");
        
        Matcher scoreMatcher = scorePattern.matcher(text);
        Matcher locationMatcher = locationPattern.matcher(text);
        Matcher subjectMatcher = subjectPattern.matcher(text);
        
        java.util.List<String> extractedInfo = new java.util.ArrayList<>();
        
        // 提取分数信息
        while (scoreMatcher.find()) {
            String score = scoreMatcher.group();
            if (!extractedInfo.contains(score)) {
                extractedInfo.add(score);
            }
        }
        
        // 提取地点信息
        while (locationMatcher.find()) {
            String location = locationMatcher.group();
            if (!extractedInfo.contains(location)) {
                extractedInfo.add(location);
            }
        }
        
        // 提取科目类型信息
        while (subjectMatcher.find()) {
            String subject = subjectMatcher.group();
            if (!extractedInfo.contains(subject)) {
                extractedInfo.add(subject);
            }
        }
        
        return extractedInfo;
    }

    /**
     * 计算信息重要性得分
     */
    private int calculateImportanceScore(String content) {
        // 根据内容类型给予不同的重要性评分
        if (content.contains("分")) {
            return 9; // 分数信息通常很重要
        } else if (content.contains("省") || content.contains("市")) {
            return 7; // 地区信息比较重要
        } else if (content.matches(".*(?:文科|理科|物理类|历史类|综合类).*")) {
            return 8; // 科目类型信息很重要
        } else {
            return 5; // 默认重要性
        }
    }

    /**
     * 提取标签
     */
    private List<String> extractTags(String content) {
        java.util.List<String> tags = new java.util.ArrayList<>();
        
        if (content.contains("分")) {
            tags.add("score");
        }
        if (content.contains("省") || content.contains("市")) {
            tags.add("location");
        }
        if (content.matches(".*(?:文科|理科|物理类|历史类|综合类).*")) {
            tags.add("subject");
        }
        
        if (tags.isEmpty()) {
            tags.add("general");
        }
        
        return tags;
    }

    /**
     * 生成记忆ID
     */
    private String generateMemoryId(String userId, long timestamp) {
        return userId + "_" + timestamp;
    }

    /**
     * 获取用户的所有长期记忆
     */
    public List<LongTermMemory> getAllMemoriesByUserId(String userId) {
        try {
            return memoryStore.getAllMemoriesByUserId(userId);
        } catch (IOException e) {
            log.error("获取用户 {} 的长期记忆失败: {}", userId, e.getMessage(), e);
            return List.of(); // 返回空列表而不是抛出异常
        }
    }

    /**
     * 语义搜索记忆
     * 使用 embedding 模型将查询转换为向量，然后检索相似记忆
     */
    public List<LongTermMemory> searchMemories(String userId, String query, int limit) {
        try {
            // 生成查询向量的嵌入
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(query).content();
            List<Float> queryEmbedding = embedding.vectorAsList();

            // 使用语义相似度检索
            return memoryStore.findSimilarMemories(userId, queryEmbedding, limit);
        } catch (IOException e) {
            log.error("语义搜索记忆失败：userId={}, query={}, error={}", userId, query, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 按标签检索记忆
     */
    public List<LongTermMemory> getMemoriesByTag(String userId, String tag) {
        try {
            return memoryStore.retrieveMemoriesByTag(userId, tag);
        } catch (IOException e) {
            log.error("按标签检索记忆失败：userId={}, tag={}, error={}", userId, tag, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取用户最近的记忆（按重要性排序）
     */
    public List<LongTermMemory> getRecentMemories(String userId, int limit) {
        try {
            List<LongTermMemory> allMemories = memoryStore.getAllMemoriesByUserId(userId);
            // 按重要性排序并返回前 N 条
            return allMemories.stream()
                    .sorted((a, b) -> Integer.compare(b.getImportanceScore(), a.getImportanceScore()))
                    .limit(limit)
                    .toList();
        } catch (IOException e) {
            log.error("获取最近记忆失败：userId={}, error={}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 按类型检索记忆
     */
    public List<LongTermMemory> getMemoriesByType(String userId, LongTermMemory.MemoryType type) {
        try {
            return memoryStore.retrieveMemoriesByType(userId, type);
        } catch (IOException e) {
            log.error("按类型检索记忆失败：userId={}, type={}, error={}", userId, type, e.getMessage(), e);
            return List.of();
        }
    }
}