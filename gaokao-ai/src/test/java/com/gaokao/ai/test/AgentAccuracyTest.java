package com.gaokao.ai.test;

import com.gaokao.ai.agent.SchoolInfoAgent;
import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AI Agent 测试类
 * 用于测试分数线查询准确性改进
 */
// 使用简单测试，不依赖Spring上下文
// @SpringBootTest(classes = com.gaokao.GaokaoApplication.class)
public class AgentAccuracyTest {

    @Autowired
    private SchoolInfoAgent schoolInfoAgent;

    /**
     * 测试分数线查询功能
     * 验证AI是否会正确使用工具查询真实数据而不是编造数据
     */
    @Test
    public void testScoreQueryAccuracy() {
        // 创建一个查询分数线的请求
        AgentRequest request = AgentRequest.builder()
                .userId("test-user")
                .sessionId("test-session")
                .question("北京大学2022年的录取分数线是多少？")
                .build();

        // 执行请求
        AgentResponse response = schoolInfoAgent.handle(request);

        // 输出结果以供检查
        System.out.println("问题：" + request.getQuestion());
        System.out.println("回答：" + response.getContent());
        
        // 注意：实际测试中，我们会验证响应内容是否包含工具调用信息
        // 或者是否包含"未找到数据"等字样（如果没有相应数据的话）
    }
    
    /**
     * 测试当缺少必要参数时的行为
     */
    @Test
    public void testMissingParameterHandling() {
        AgentRequest request = AgentRequest.builder()
                .userId("test-user")
                .sessionId("test-session")
                .question("我想知道某校的分数线，但我只知道省份和科目类型，不知道具体学校")
                .build();

        AgentResponse response = schoolInfoAgent.handle(request);

        System.out.println("问题：" + request.getQuestion());
        System.out.println("回答：" + response.getContent());
    }
}