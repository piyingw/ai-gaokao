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

import java.util.Arrays;
import java.util.List;

/**
 * 政策问答 Agent
 * 基于 RAG 技术，解答高考政策相关问题
 * 具备自主数据检索决策能力
 */
@Slf4j
@Component
public class PolicyAgent implements GaokaoAgent {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SkillTool skillTool;

    // 增强的关键词匹配库
    private static final List<String> HIGH_CONFIDENCE_KEYWORDS = Arrays.asList(
            "平行志愿", "顺序志愿", "征集志愿", "投档规则", "录取规则",
            "高考政策", "志愿规则", "批次设置", "录取批次", "加分政策",
            "专项计划", "自主招生", "强基计划", "保送生", "艺术类招生"
    );

    private static final List<String> MEDIUM_CONFIDENCE_KEYWORDS = Arrays.asList(
            "政策", "规则", "规定", "录取", "招生", "批次", "投档",
            "退档", "调剂", "服从调剂", "第一志愿", "志愿顺序",
            "分数线", "省控线", "批次线", "提前批", "本科批",
            "专科批", "一本", "二本", "三本", "高职"
    );

    private static final List<String> QUESTION_KEYWORDS = Arrays.asList(
            "是什么", "什么意思", "怎么理解", "解释一下", "帮我理解",
            "如何", "怎样", "能不能", "可以吗", "是否"
    );

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
                log.warn("向量库无数据，尝试网络搜索");
                // 使用网络搜索作为备选
                return handleWithWebSearch(request);
            }

            // 检查最高分数是否满足阈值
            double topScore = matches.get(0).score();
            log.info("RAG检索: topScore={}, 结果数={}", topScore, matches.size());

            if (topScore < MIN_SCORE_THRESHOLD) {
                log.warn("检索结果相似度过低: topScore={}", topScore);
                // RAG结果不够好，尝试网络搜索补充
                return handleWithHybridSearch(request, matches);
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

    /**
     * 使用网络搜索处理
     */
    private AgentResponse handleWithWebSearch(AgentRequest request) {
        try {
            PolicyAssistant assistant = AiServices.builder(PolicyAssistant.class)
                    .chatModel(chatModel)
                    .tools(skillTool)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .systemMessageProvider(memoryId -> SYSTEM_PROMPT_WITH_WEB)
                    .build();

            String response = assistant.chat(request.getQuestion() + "（请通过网络搜索获取最新政策信息）");
            return AgentResponse.success(getName(), response);
        } catch (Exception e) {
            log.error("网络搜索处理失败", e);
            return AgentResponse.failure(getName(), "暂无相关政策信息，请尝试其他问题或访问官方网站查询");
        }
    }

    /**
     * 混合检索处理（RAG + 网络搜索）
     */
    private AgentResponse handleWithHybridSearch(AgentRequest request, List<EmbeddingMatch<TextSegment>> ragMatches) {
        try {
            // 构建RAG上下文
            StringBuilder ragContext = new StringBuilder("本地政策文档检索结果：\n");
            for (EmbeddingMatch<TextSegment> match : ragMatches) {
                ragContext.append("- ").append(match.embedded().text()).append("\n");
            }

            PolicyAssistant assistant = AiServices.builder(PolicyAssistant.class)
                    .chatModel(chatModel)
                    .tools(skillTool)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .systemMessageProvider(memoryId -> SYSTEM_PROMPT_HYBRID)
                    .build();

            String enhancedQuestion = request.getQuestion() + "\n\n本地文档参考：\n" + ragContext;
            String response = assistant.chat(enhancedQuestion);

            return AgentResponse.success(getName(), response);
        } catch (Exception e) {
            log.error("混合检索处理失败", e);
            return handleWithWebSearch(request);
        }
    }

    @Override
    public boolean canHandle(String question) {
        if (question == null) return false;
        String lower = question.toLowerCase();

        // 高置信度关键词直接匹配
        for (String keyword : HIGH_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        // 中置信度关键词 + 问题关键词组合
        boolean hasMediumKeyword = false;
        for (String keyword : MEDIUM_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword)) {
                hasMediumKeyword = true;
                break;
            }
        }

        boolean hasQuestionKeyword = false;
        for (String keyword : QUESTION_KEYWORDS) {
            if (lower.contains(keyword)) {
                hasQuestionKeyword = true;
                break;
            }
        }

        // 中置信度关键词匹配（需要2个以上）
        int mediumCount = 0;
        for (String keyword : MEDIUM_CONFIDENCE_KEYWORDS) {
            if (lower.contains(keyword)) {
                mediumCount++;
            }
        }

        return mediumCount >= 2 || (hasMediumKeyword && hasQuestionKeyword);
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

            ## 数据检索决策指南

            ### 数据源优先级
            1. **RAG向量检索优先**
               - 本地政策文档库已包含常见政策信息
               - 对于政策解释类问题，优先使用RAG检索

            2. **网络搜索补充**（web-search-skill）
               - 当RAG检索结果相似度过低时，使用网络搜索
               - 查询最新政策动态、各省最新招生政策时使用
               - 参数示例：{"query": "2024年XX省高考录取规则", "searchType": "general"}

            ### 典型场景决策
            - "平行志愿是什么" → RAG检索即可
            - "最新高考政策变化" → 网络搜索获取最新信息
            - "XX省2024录取规则" → 先RAG，相似度低则网络搜索

            你可以使用以下工具：
            - executeSkill: 通用技能执行工具
              * web-search-skill: 网络搜索技能，搜索最新高考政策信息
            - searchMemories: 搜索用户的长期记忆
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

    private static final String SYSTEM_PROMPT_WITH_WEB = """
            你是一位专业的高考政策顾问。当前本地政策库数据不足，请通过网络搜索获取信息。

            使用web-search-skill搜索相关政策信息，参数格式：
            {"query": "搜索关键词", "searchType": "general"}

            重要约束：
            - 必须通过工具获取真实信息，不要编造
            - 网络搜索结果需注明来源
            - 提醒用户以官方渠道为准
            """;

    private static final String SYSTEM_PROMPT_HYBRID = """
            你是一位专业的高考政策顾问。当前已有本地政策文档检索结果，但相似度较低。

            请结合本地文档参考内容和网络搜索结果，综合回答用户问题。

            数据使用优先级：
            1. 优先使用本地文档内容
            2. 补充使用网络搜索获取最新信息
            3. 如有冲突，以官方最新发布为准

            回答时请注明信息来源（本地文档/网络搜索）。
            """;
}