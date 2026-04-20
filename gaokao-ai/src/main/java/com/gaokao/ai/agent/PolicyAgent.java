package com.gaokao.ai.agent;

import com.gaokao.ai.agent.model.AgentRequest;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.tool.SkillTool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 政策问答 Agent
 * 基于 RAG 技术，解答高考政策相关问题
 */
@Slf4j
@Component
public class PolicyAgent implements GaokaoAgent {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SkillTool skillTool;

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE_THRESHOLD = 0.6;

    public PolicyAgent(ChatModel chatModel,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore,
                       SkillTool skillTool) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.skillTool = skillTool;
    }

    @Override
    public String getName() {
        return "policy";
    }

    @Override
    public String getDescription() {
        return "政策问答专家：解答高考政策、录取规则、志愿填报政策等问题";
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        log.info("PolicyAgent 处理请求：{}", request.getQuestion());

        try {
            // 先手动查询，检查是否有足够相关的内容
            Response<Embedding> embeddingResponse = embeddingModel.embed(request.getQuestion());
            Embedding queryEmbedding = embeddingResponse.content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(MAX_RESULTS)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            if (matches.isEmpty()) {
                log.warn("向量库无数据");
                return AgentResponse.failure(getName(), "暂无相关政策信息，请稍后再试");
            }

            // 检查最高分数是否满足阈值
            double topScore = matches.get(0).score();
            log.info("RAG检索: topScore={}, 结果数={}", topScore, matches.size());

            if (topScore < MIN_SCORE_THRESHOLD) {
                log.warn("检索结果相似度过低: topScore={}", topScore);
                return AgentResponse.failure(getName(), "暂无相关政策信息，请尝试其他问题或使用网络搜索获取最新政策");
            }

            // 构建 AI Service with RAG
            EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(MAX_RESULTS)
                    .build();

            PolicyAssistant assistant = AiServices.builder(PolicyAssistant.class)
                    .chatModel(chatModel)
                    .tools(skillTool)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .contentRetriever(retriever)
                    .systemMessageProvider(memoryId -> SYSTEM_PROMPT)
                    .build();

            String response = assistant.chat(request.getQuestion());

            return AgentResponse.success(getName(), response);

        } catch (Exception e) {
            log.error("政策问答失败", e);
            return AgentResponse.failure(getName(), "政策问答服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public boolean canHandle(String question) {
        if (question == null) return false;
        String lower = question.toLowerCase();
        return lower.contains("政策") || lower.contains("规则") || lower.contains("规定")
                || lower.contains("录取") || lower.contains("招生") || lower.contains("加分")
                || lower.contains("批次") || lower.contains("投档") || lower.contains("退档")
                || lower.contains("调剂") || lower.contains("平行志愿") || lower.contains("顺序志愿");
    }

    /**
     * AI Service 接口
     */
    interface PolicyAssistant {
        String chat(String message);
    }

    private static final String SYSTEM_PROMPT = """
            你是一位专业的高考政策顾问，精通全国各省高考政策和志愿填报规则。

            你的职责是：
            1. 解答高考政策相关问题，包括录取规则、批次设置、投档规则等
            2. 解释志愿填报政策，如平行志愿、顺序志愿、专业调剂等
            3. 提供各省招生政策解读
            4. 提醒考生注意重要时间节点和事项

            你可以使用以下工具：
            - executeSkill: 通用技能执行工具
              * web-search-skill: 网络搜索技能，搜索最新高考政策信息
            重要约束：
            - 优先使用 RAG 检索本地政策文档
            - 如果本地文档信息不足或过时，使用 web-search-skill 联网搜索最新政策
            - 不要编造政策信息，必须通过检索或搜索获取真实内容
            - searchMemories: 搜索用户的长期记忆，根据查询内容查找最相关的记忆
            - getMemoriesByTag: 按标签检索用户的长期记忆（标签：score, location, subject）
            - getRecentMemories: 获取用户最近的重要记忆
            - rememberInfo: 记住用户提供的信息
            - autoExtractAndRemember: 自动提取并记住对话中的关键信息

            回答要求：
            1. 准确引用政策文件内容
            2. 用通俗易懂的语言解释专业术语
            3. 如果检索到的政策信息不足，明确告知用户
            4. 不要编造政策信息
            5. 建议用户以官方发布的政策文件为准
            6. 可以使用 searchMemories 查看用户的历史偏好（如省份、科类等），提供更精准的政策解读

            请用专业、耐心的语气回答用户问题。
            """;
}