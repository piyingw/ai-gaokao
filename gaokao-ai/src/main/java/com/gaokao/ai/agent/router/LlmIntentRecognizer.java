package com.gaokao.ai.agent.router;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM意图识别器
 * 使用大语言模型进行智能意图识别
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmIntentRecognizer {

    private final ChatModel chatModel;

    /**
     * 使用LLM识别用户意图
     */
    public IntentRouteResult.LlmRecognizeResult recognize(String question, List<String> availableAgents) {
        if (question == null || question.trim().isEmpty()) {
            return IntentRouteResult.LlmRecognizeResult.builder()
                    .agent("recommend")
                    .confidence(0.3)
                    .reasoning("空问题，默认路由")
                    .build();
        }

        try {
            // 创建路由助手
            IntentRecognizerAssistant assistant = AiServices.builder(IntentRecognizerAssistant.class)
                    .chatModel(chatModel)
                    .systemMessageProvider(id -> buildSystemPrompt(availableAgents))
                    .build();

            String responseText = assistant.recognizeIntent(question);
            log.debug("LLM意图识别响应: {}", responseText);

            // 解析JSON结果
            JSONObject json = parseJsonResponse(responseText);
            if (json != null) {
                String agent = json.getString("agent");
                Double confidence = json.getDouble("confidence");
                String reasoning = json.getString("reasoning");

                // 验证agent是否有效
                if (availableAgents.contains(agent) && confidence != null) {
                    return IntentRouteResult.LlmRecognizeResult.builder()
                            .agent(agent)
                            .confidence(confidence)
                            .reasoning(reasoning)
                            .build();
                }
            }

            log.warn("LLM意图识别结果无效，返回默认路由");
            return IntentRouteResult.LlmRecognizeResult.builder()
                    .agent("recommend")
                    .confidence(0.4)
                    .reasoning("LLM识别失败，默认路由到推荐Agent")
                    .build();

        } catch (Exception e) {
            log.error("LLM意图识别异常: {}", e.getMessage());
            return IntentRouteResult.LlmRecognizeResult.builder()
                    .agent("recommend")
                    .confidence(0.3)
                    .reasoning("LLM调用失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 多Agent协作意图识别
     * 判断是否需要多个Agent协作处理
     */
    public CollaborationResult recognizeCollaboration(String question, List<String> availableAgents) {
        try {
            CollaborationRecognizer assistant = AiServices.builder(CollaborationRecognizer.class)
                    .chatModel(chatModel)
                    .systemMessageProvider(id -> buildCollaborationPrompt(availableAgents))
                    .build();

            String responseText = assistant.recognizeCollaboration(question);
            log.debug("协作意图识别响应: {}", responseText);

            JSONObject json = parseJsonResponse(responseText);
            if (json != null) {
                boolean needsCollaboration = json.getBooleanValue("needsCollaboration");
                List<String> agents = json.getList("agents", String.class);
                String reasoning = json.getString("reasoning");

                return new CollaborationResult(needsCollaboration, agents, reasoning);
            }

        } catch (Exception e) {
            log.error("协作意图识别异常: {}", e.getMessage());
        }

        return new CollaborationResult(false, List.of("recommend"), "默认单Agent处理");
    }

    /**
     * 构建意图识别系统提示词
     */
    private String buildSystemPrompt(List<String> availableAgents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是高考志愿填报系统的智能意图识别助手。请根据用户的问题，判断应该由哪个Agent处理。\n\n");
        prompt.append("可用的Agent及其职责：\n");

        prompt.append("1. recommend - 志愿推荐Agent：\n");
        prompt.append("   - 根据分数、位次生成志愿方案\n");
        prompt.append("   - 录取概率分析\n");
        prompt.append("   - 冲稳保策略建议\n");
        prompt.append("   - 志愿填报整体规划\n\n");

        prompt.append("2. school - 学校信息Agent：\n");
        prompt.append("   - 查询学校详情、排名\n");
        prompt.append("   - 院校对比分析\n");
        prompt.append("   - 专业信息查询\n");
        prompt.append("   - 历年分数线查询\n\n");

        prompt.append("3. policy - 政策问答Agent：\n");
        prompt.append("   - 高考政策解读\n");
        prompt.append("   - 录取规则解释\n");
        prompt.append("   - 平行志愿、顺序志愿说明\n");
        prompt.append("   - 批次、投档规则解答\n\n");

        prompt.append("4. personality - 性格分析Agent：\n");
        prompt.append("   - 性格特点分析\n");
        prompt.append("   - 职业兴趣测试\n");
        prompt.append("   - 专业方向推荐\n");
        prompt.append("   - 职业规划建议\n\n");

        prompt.append("请以JSON格式返回（只返回JSON，不要其他内容）：\n");
        prompt.append("{\"agent\": \"agent名称\", \"confidence\": 0.0-1.0之间的数值, \"reasoning\": \"判断理由\"}\n\n");

        prompt.append("置信度标准：\n");
        prompt.append("- 0.9+：非常明确，问题直接指向某个Agent\n");
        prompt.append("- 0.7-0.9：比较明确，问题主要属于某个Agent\n");
        prompt.append("- 0.5-0.7：中等确定，问题可能涉及多个Agent\n");
        prompt.append("- 0.3-0.5：不太明确，需要更多信息\n");

        return prompt.toString();
    }

    /**
     * 构建协作意图识别提示词
     */
    private String buildCollaborationPrompt(List<String> availableAgents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("判断用户问题是否需要多个Agent协作处理。\n\n");
        prompt.append("协作场景示例：\n");
        prompt.append("- \"我是理科生600分，想了解清华大学并推荐志愿\" → 需要school+recommend\n");
        prompt.append("- \"分析我的性格并推荐适合的院校\" → 需要personality+recommend\n");
        prompt.append("- \"解释平行志愿规则并推荐志愿策略\" → 需要policy+recommend\n\n");

        prompt.append("返回JSON格式：\n");
        prompt.append("{\"needsCollaboration\": true/false, \"agents\": [\"agent1\", \"agent2\"], \"reasoning\": \"理由\"}\n");

        return prompt.toString();
    }

    /**
     * 解析JSON响应
     */
    private JSONObject parseJsonResponse(String text) {
        try {
            // 尝试提取JSON部分
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return JSON.parseObject(text.substring(start, end + 1));
            }
        } catch (Exception e) {
            log.debug("JSON解析失败: {}", text);
        }
        return null;
    }

    /**
     * 意图识别助手接口
     */
    interface IntentRecognizerAssistant {
        String recognizeIntent(String question);
    }

    /**
     * 协作识别助手接口
     */
    interface CollaborationRecognizer {
        String recognizeCollaboration(String question);
    }

    /**
     * 协作结果
     */
    public record CollaborationResult(boolean needsCollaboration, List<String> agents, String reasoning) {}
}