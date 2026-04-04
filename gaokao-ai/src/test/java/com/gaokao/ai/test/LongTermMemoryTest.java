package com.gaokao.ai.test;

import com.gaokao.ai.entity.LongTermMemory;
import com.gaokao.ai.service.LongTermMemoryService;
import com.gaokao.ai.store.ElasticsearchLongTermMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 长期记忆功能测试类
 */
@SpringBootTest
public class LongTermMemoryTest {

    @Autowired
    private LongTermMemoryService longTermMemoryService;

    @Autowired
    private ElasticsearchLongTermMemoryStore memoryStore;

    /**
     * 测试用户主动记忆功能
     */
    @Test
    public void testUserRequestedMemory() {
        String userId = "test_user_001";
        String content = "我的高考分数是580分，来自北京市";

        // 测试用户主动记忆
        longTermMemoryService.rememberRequestedByUser(userId, content);

        System.out.println("用户主动记忆测试完成: " + content);
    }

    /**
     * 测试自动提取记忆功能
     */
    @Test
    public void testAutoExtractMemory() {
        String userId = "test_user_001";
        String content = "我是一名来自江苏省的理科生，今年高考估分大约600分左右";

        // 测试自动提取记忆
        longTermMemoryService.autoExtractAndRemember(userId, content);

        System.out.println("自动提取记忆测试完成: " + content);
    }

    /**
     * 测试检索用户记忆功能
     */
    @Test
    public void testRetrieveMemories() {
        String userId = "test_user_001";

        // 获取用户的所有记忆
        List<LongTermMemory> memories = longTermMemoryService.getAllMemoriesByUserId(userId);

        System.out.println("用户 " + userId + " 的长期记忆:");
        for (LongTermMemory memory : memories) {
            System.out.println("- " + memory.getType() + ": " + memory.getContent() + 
                             " (重要性: " + memory.getImportanceScore() + ")");
        }
    }

    /**
     * 测试存储和检索功能
     */
    @Test
    public void testStoreAndRetrieve() {
        try {
            // 创建一个测试记忆
            LongTermMemory testMemory = LongTermMemory.builder()
                    .id("test_memory_001")
                    .userId("test_user_001")
                    .content("测试记忆内容：我来自广东省，高考分数620分")
                    .type(LongTermMemory.MemoryType.USER_REQUESTED)
                    .embedding(Collections.nCopies(1024, 0.1f)) // 使用占位向量
                    .tags(List.of("test", "score", "location"))
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .importanceScore(8)
                    .build();

            // 存储记忆
            memoryStore.storeMemory(testMemory);
            System.out.println("记忆存储测试完成");

            // 检索记忆
            List<LongTermMemory> retrieved = memoryStore.retrieveMemoriesByUserId("test_user_001", Collections.nCopies(1024, 0.1f));
            System.out.println("检索到 " + retrieved.size() + " 条相关记忆");
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}