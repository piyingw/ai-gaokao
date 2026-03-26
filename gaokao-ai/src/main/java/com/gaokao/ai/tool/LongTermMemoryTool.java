package com.gaokao.ai.tool;

import com.gaokao.ai.service.LongTermMemoryService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 长期记忆工具
 * 供 AI Agent 调用，实现记忆功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LongTermMemoryTool {

    private final LongTermMemoryService longTermMemoryService;

    @Tool("记住用户提供的信息")
    public String rememberInfo(String userId, String content) {
        log.info("收到记住请求：userId={}, content={}", userId, content);

        try {
            longTermMemoryService.rememberRequestedByUser(userId, content);
            return "已成功记住信息：" + content;
        } catch (Exception e) {
            log.error("记住信息失败：{}", e.getMessage(), e);
            return "记住信息失败：" + e.getMessage();
        }
    }

    @Tool("自动提取并记住对话中的关键信息")
    public String autoExtractAndRemember(String userId, String content) {
        log.info("收到自动提取请求：userId={}, content length={}", userId, content.length());

        try {
            longTermMemoryService.autoExtractAndRemember(userId, content);
            return "已自动提取并记住关键信息";
        } catch (Exception e) {
            log.error("自动提取信息失败：{}", e.getMessage(), e);
            return "自动提取信息失败：" + e.getMessage();
        }
    }

    @Tool("语义搜索用户的长期记忆，根据查询内容查找最相关的记忆")
    public String searchMemories(String userId, String query) {
        log.info("收到搜索记忆请求：userId={}, query={}", userId, query);

        try {
            var memories = longTermMemoryService.searchMemories(userId, query, 5);
            if (memories.isEmpty()) {
                return "未找到相关记忆";
            }
            StringBuilder sb = new StringBuilder("找到以下相关记忆：\n");
            for (var mem : memories) {
                sb.append("- [").append(mem.getType())
                  .append("] ").append(mem.getContent())
                  .append(" (重要性：").append(mem.getImportanceScore()).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("搜索记忆失败：{}", e.getMessage(), e);
            return "搜索记忆失败：" + e.getMessage();
        }
    }

    @Tool("按标签检索用户的长期记忆，可用标签：score(分数), location(地区), subject(科目)")
    public String getMemoriesByTag(String userId, String tag) {
        log.info("收到按标签检索请求：userId={}, tag={}", userId, tag);

        try {
            var memories = longTermMemoryService.getMemoriesByTag(userId, tag);
            if (memories.isEmpty()) {
                return "未找到标签为 '" + tag + "' 的记忆";
            }
            StringBuilder sb = new StringBuilder("标签为 '" + tag + "' 的记忆：\n");
            for (var mem : memories) {
                sb.append("- [").append(mem.getType())
                  .append("] ").append(mem.getContent()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("按标签检索记忆失败：{}", e.getMessage(), e);
            return "按标签检索记忆失败：" + e.getMessage();
        }
    }

    @Tool("获取用户最近的重要记忆，limit 为返回数量")
    public String getRecentMemories(String userId, int limit) {
        log.info("收到获取最近记忆请求：userId={}, limit={}", userId, limit);

        try {
            var memories = longTermMemoryService.getRecentMemories(userId, limit);
            if (memories.isEmpty()) {
                return "暂无记忆";
            }
            StringBuilder sb = new StringBuilder("最近的重要记忆：\n");
            for (var mem : memories) {
                sb.append("- [").append(mem.getType())
                  .append("] ").append(mem.getContent())
                  .append(" (重要性：").append(mem.getImportanceScore()).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取最近记忆失败：{}", e.getMessage(), e);
            return "获取最近记忆失败：" + e.getMessage();
        }
    }
}
