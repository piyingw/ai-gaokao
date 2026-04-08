# AI 模块技术文档

> 版本：1.0.0  
> 更新日期：2026年3月13日  
> 模块路径：`gaokao-ai`

---

## 目录

1. [模块概述](#1-模块概述)
2. [业务流程](#2-业务流程)
3. [技术架构](#3-技术架构)
4. [Agent 功能介绍](#4-agent-功能介绍)
5. [Tool 工具介绍](#5-tool-工具介绍)
6. [配置说明](#6-配置说明)
7. [API 接口](#7-api-接口)
8. [数据模型](#8-数据模型)
9. [扩展指南](#9-扩展指南)

---

## 1. 模块概述

### 1.1 模块定位

`gaokao-ai` 是高考志愿填报系统的核心智能模块，基于 **LangChain4j** 框架构建，集成阿里通义千问大模型，提供以下核心能力：

| 能力 | 说明 |
|------|------|
| **智能对话** | 多轮对话，自动识别意图并路由 |
| **志愿推荐** | 基于分数、偏好生成冲稳保志愿方案 |
| **政策问答** | RAG 检索增强，解答高考政策问题 |
| **学校查询** | 院校信息查询、对比分析 |
| **性格分析** | 分析学生性格，推荐适合的专业方向 |

### 1.2 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| LangChain4j | 1.0.0-beta1 | AI 应用开发框架 |
| 阿里通义千问 | qwen-plus | 大语言模型 |
| text-embedding-v3 | - | 文本向量化模型 |
| PGVector | - | 向量存储（RAG） |
| Redis | 7.0+ | 会话记忆存储 |

### 1.3 模块依赖

```
gaokao-ai
    ├── gaokao-data (数据服务)
    │   └── UniversityMapper, MajorMapper, AdmissionScoreMapper
    └── gaokao-common (公共组件)
        └── Result, BusinessException
```

---

## 2. 业务流程

### 2.1 整体业务流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户请求入口                                    │
│                         AIController (/api/ai/*)                            │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AIService                                       │
│                           (服务门面层)                                        │
│    - chat()          智能对话                                                │
│    - oneClickRecommend()  一键生成志愿                                       │
│    - recommend()     志愿推荐                                                │
│    - policyQA()      政策问答                                                │
│    - schoolInfo()    学校查询                                                │
│    - personalityAnalysis()  性格分析                                         │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        GaokaoOrchestratorAgent                               │
│                            (主协调器)                                         │
│                                                                             │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│   │ 意图识别    │ -> │ 路由分发    │ -> │ Agent执行   │ -> │ 结果聚合    │ │
│   └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│                                                                             │
│   路由策略：关键词匹配 + LLM 意图识别                                        │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┬───────────────────┐
          ▼                       ▼                       ▼                   ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ RecommendAgent  │   │   PolicyAgent   │   │ SchoolInfoAgent │   │PersonalityAgent │
│   (志愿推荐)     │   │   (政策问答)     │   │   (学校查询)     │   │   (性格分析)     │
└────────┬────────┘   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘
         │                     │                     │                     │
         │                     ▼                     │                     │
         │            ┌─────────────────┐            │                     │
         │            │  RAG 检索引擎   │            │                     │
         │            │ (PGVector)      │            │                     │
         │            └─────────────────┘            │                     │
         │                     │                     │                     │
         └─────────────────────┴─────────────────────┴─────────────────────┘
                                       │
                                       ▼
                        ┌─────────────────────────────┐
                        │      LangChain4j AiServices │
                        │      + Tools 调用链          │
                        └──────────────┬──────────────┘
                                       │
                                       ▼
                        ┌─────────────────────────────┐
                        │      通义千问 LLM           │
                        │      (qwen-plus)            │
                        └─────────────────────────────┘
```

### 2.2 智能对话流程

```
用户输入消息
      │
      ▼
┌─────────────────────────────────────┐
│ 1. 生成/获取 sessionId              │
│    - 新会话：UUID 生成              │
│    - 已有会话：从请求获取           │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│ 2. 意图识别与路由                   │
│    - 关键词匹配 (canHandle)         │
│    - 匹配规则：                     │
│      · 推荐/志愿/填报 → recommend   │
│      · 政策/规则/录取 → policy      │
│      · 大学/学院/专业 → school      │
│      · 性格/兴趣/职业 → personality │
│    - 默认路由：recommend            │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│ 3. Agent 处理                       │
│    - 构建 AiServices 实例           │
│    - 注入 Tools                     │
│    - 设置 System Prompt             │
│    - 调用 LLM 生成响应              │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│ 4. 返回响应                         │
│    - AgentResponse 封装             │
│    - 包含：agentName, content,      │
│            success, errorMessage    │
└─────────────────────────────────────┘
```

### 2.3 一键生成志愿流程

```
用户提交：分数 + 省份 + 科类 + 性格描述
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 1: 性格分析                                            │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ PersonalityAgent.handle()                               │ │
│ │ - 分析用户性格特点                                       │ │
│ │ - 推荐适合的专业方向                                     │ │
│ │ - 返回性格分析结果                                       │ │
│ └─────────────────────────────────────────────────────────┘ │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 2: 志愿推荐                                            │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ RecommendAgent.handle()                                 │ │
│ │ - 结合性格分析结果                                       │ │
│ │ - 调用 Tools 查询院校数据                               │ │
│ │ - 计算录取概率                                          │ │
│ │ - 生成冲稳保志愿方案                                    │ │
│ └─────────────────────────────────────────────────────────┘ │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 3: 返回结果                                            │
│ - 冲刺志愿列表 (录取概率 30%-50%)                           │
│ - 稳妥志愿列表 (录取概率 50%-70%)                           │
│ - 保底志愿列表 (录取概率 70%+)                              │
│ - 每个志愿包含：院校、专业、概率、推荐理由                  │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 政策问答流程（RAG）

```
用户提问政策问题
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│ PolicyAgent 处理                                               │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ 1. 问题向量化                                            │  │
│  │    EmbeddingModel.embed(question)                        │  │
│  │    → 生成 1536 维向量                                    │  │
│  └─────────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ 2. 向量相似度检索                                        │  │
│  │    EmbeddingStoreContentRetriever                        │  │
│  │    - maxResults: 5                                       │  │
│  │    - minScore: 0.6                                       │  │
│  │    → 返回最相关的 5 个文档片段                           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ 3. 构建 Prompt                                           │  │
│  │    System Prompt + 检索到的文档 + 用户问题               │  │
│  └─────────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ 4. LLM 生成回答                                          │  │
│  │    ChatLanguageModel.generate(prompt)                    │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                               │
└───────────────────────────────────────────────────────────────┘
        │
        ▼
返回政策问答结果
```

---

## 3. 技术架构

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Controller 层                             │
│                     AIController.java                           │
│                  (REST API 接口定义)                             │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Service 层                                │
│                      AIService.java                             │
│              (业务逻辑封装、服务编排)                             │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Agent 层                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              GaokaoOrchestratorAgent                       │  │
│  │                   (主协调器)                                │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐    │
│  │ Recommend   │   Policy    │ SchoolInfo  │ Personality │    │
│  │   Agent     │   Agent     │   Agent     │   Agent     │    │
│  └─────────────┴─────────────┴─────────────┴─────────────┘    │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Tool 层                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │UniversityQuery  │  │ ScoreAnalysis   │  │   Calculator    │ │
│  │     Tool        │  │     Tool        │  │     Tool        │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│  ┌─────────────────┐                                           │
│  │  MajorQuery     │                                           │
│  │     Tool        │                                           │
│  └─────────────────┘                                           │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Config 层                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ LangChain4j     │  │ ChatMemory      │  │  VectorStore    │ │
│  │   Config        │  │   Config        │  │    Config       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心组件交互

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           LangChain4j 核心组件                            │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────┐      ┌────────────────┐      ┌────────────────┐    │
│  │ChatLanguageModel│      │ EmbeddingModel │      │ EmbeddingStore │    │
│  │  (qwen-plus)   │      │(text-embedding)│      │  (PGVector)    │    │
│  └───────┬────────┘      └───────┬────────┘      └───────┬────────┘    │
│          │                       │                       │              │
│          └───────────────────────┴───────────────────────┘              │
│                                  │                                      │
│                                  ▼                                      │
│                    ┌────────────────────────┐                           │
│                    │      AiServices        │                           │
│                    │    (Agent 构建器)       │                           │
│                    │                        │                           │
│                    │  - chatLanguageModel   │                           │
│                    │  - tools               │                           │
│                    │  - chatMemory          │                           │
│                    │  - contentRetriever    │                           │
│                    │  - systemMessageProvider│                          │
│                    └────────────────────────┘                           │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.3 技术实现方式

#### 3.3.1 Agent 构建模式

使用 LangChain4j 的 **AiServices** 构建 Agent：

```java
RecommendAssistant assistant = AiServices.builder(RecommendAssistant.class)
    .chatLanguageModel(chatModel)                    // 1. 注入 LLM
    .tools(universityQueryTool, scoreAnalysisTool)   // 2. 注入 Tools
    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))  // 3. 会话记忆
    .systemMessageProvider(memoryId -> SYSTEM_PROMPT)         // 4. 系统提示词
    .build();

String response = assistant.chat(request.getQuestion());
```

#### 3.3.2 Tool 调用机制

Tool 使用 `@Tool` 注解定义，LLM 自动识别并调用：

```java
@Tool("根据条件查询院校列表，支持按省份、层次、类型筛选")
public String queryUniversities(
    String province,
    String level,
    String type,
    Integer minScore,
    Integer maxScore,
    Integer limit
) {
    // 实现逻辑
}
```

**调用流程：**
1. LLM 分析用户问题，识别需要调用的 Tool
2. LLM 生成 Tool 调用参数（JSON 格式）
3. LangChain4j 解析参数并执行 Tool 方法
4. Tool 返回结果注入到对话上下文
5. LLM 基于结果生成最终回答

#### 3.3.3 RAG 实现方式

```java
// 构建内容检索器
EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(embeddingStore)    // 向量存储
    .embeddingModel(embeddingModel)    // 向量化模型
    .maxResults(5)                     // 最大返回数量
    .minScore(0.6)                     // 最小相似度阈值
    .build();

// 注入到 Agent
PolicyAssistant assistant = AiServices.builder(PolicyAssistant.class)
    .chatLanguageModel(chatModel)
    .contentRetriever(retriever)       // RAG 检索器
    .build();
```

#### 3.3.4 会话记忆实现

```java
// 内存存储（开发环境）
@Bean
public ChatMemoryStore chatMemoryStore() {
    return new InMemoryChatMemoryStore();
}

// Redis 存储（生产环境）
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {
    
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + memoryId);
        return JSON.parseArray(json, ChatMessage.class);
    }
    
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memoryId, 
            JSON.toJSONString(messages), Duration.ofHours(24));
    }
}
```

---

## 4. Agent 功能介绍

### 4.1 Agent 接口定义

```java
public interface GaokaoAgent {
    
    /**
     * 处理用户请求
     */
    AgentResponse handle(AgentRequest request);
    
    /**
     * 获取 Agent 名称
     */
    String getName();
    
    /**
     * 获取 Agent 描述
     */
    String getDescription();
    
    /**
     * 判断是否可以处理该请求（用于路由）
     */
    default boolean canHandle(String question) {
        return true;
    }
}
```

### 4.2 GaokaoOrchestratorAgent（主协调器）

**职责：** 意图识别、路由分发、结果聚合

| 方法 | 功能 | 说明 |
|------|------|------|
| `process()` | 处理用户请求 | 路由到对应 Agent |
| `oneClickRecommend()` | 一键生成志愿 | 性格分析 + 志愿推荐 |
| `chat()` | 多轮对话 | 保持会话上下文 |
| `route()` | 意图路由 | 关键词匹配 + LLM 路由 |
| `getAgentInfos()` | 获取 Agent 列表 | 返回所有 Agent 信息 |

**路由规则：**

```java
private AgentRoute simpleRoute(String question) {
    String lower = question.toLowerCase();
    
    // 遍历所有 Agent，调用 canHandle 判断
    for (Map.Entry<String, GaokaoAgent> entry : agents.entrySet()) {
        if (entry.getValue().canHandle(question)) {
            return AgentRoute.of(entry.getKey(), 0.7);
        }
    }
    
    // 默认路由到推荐 Agent
    return AgentRoute.of("recommend", 0.5);
}
```

---

### 4.3 RecommendAgent（志愿推荐 Agent）

**职责：** 根据学生分数、偏好等信息，智能生成志愿填报方案

**核心能力：**

| 能力 | 说明 |
|------|------|
| 院校筛选 | 根据分数、省份、科类筛选合适院校 |
| 专业推荐 | 结合性格特点推荐适合专业 |
| 冲稳保策略 | 按录取概率分类生成志愿方案 |
| 概率分析 | 计算每个志愿的录取概率 |

**可用 Tools：**

| Tool | 功能 |
|------|------|
| `queryUniversities` | 查询符合条件的院校列表 |
| `getUniversityDetail` | 获取院校详细信息 |
| `searchUniversities` | 模糊搜索院校 |
| `getScoreHistory` | 查询院校历年分数线 |
| `queryByScore` | 根据分数查询可报考院校 |
| `analyzeScoreCompetitiveness` | 分析分数竞争力 |
| `calculateAdmissionProbability` | 计算录取概率 |
| `calculatePlanDistribution` | 计算冲稳保分配方案 |
| `convertScoreToRank` | 分数位次换算 |
| `calculateCostPerformance` | 计算志愿性价比 |

**System Prompt：**

```
你是一位资深高考志愿填报专家，拥有20年志愿填报经验，精通全国各省高考政策和录取规则。

你的职责是：
1. 根据学生的分数、位次、省份、科类，筛选合适的院校
2. 结合学生的性格特点、兴趣爱好，推荐适合的专业方向
3. 按照冲稳保策略，生成科学合理的志愿方案
4. 为每个志愿提供详细的推荐理由和录取概率分析

回答要求：
1. 先了解学生的基本情况（分数、省份、科类）
2. 主动询问学生的偏好（专业方向、城市偏好、院校层次等）
3. 使用工具查询数据，不要编造信息
4. 给出具体的院校推荐和理由
5. 按冲稳保分类整理推荐结果
```

**关键词匹配规则：**

```java
return lower.contains("推荐") || lower.contains("志愿") || lower.contains("填报")
    || lower.contains("选择") || lower.contains("学校") || lower.contains("专业")
    || lower.contains("录取") || lower.contains("分数");
```

---

### 4.4 PolicyAgent（政策问答 Agent）

**职责：** 基于RAG技术，解答高考政策相关问题

**核心能力：**

| 能力 | 说明 |
|------|------|
| 政策检索 | 向量相似度检索政策文档 |
| 政策解读 | 用通俗语言解释政策内容 |
| 规则说明 | 解答录取规则、批次设置等问题 |

**RAG 配置：**

```java
EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(embeddingStore)
    .embeddingModel(embeddingModel)
    .maxResults(5)      // 返回最相关的 5 个文档片段
    .minScore(0.6)      // 最小相似度 0.6
    .build();
```

**System Prompt：**

```
你是一位专业的高考政策顾问，精通全国各省高考政策和志愿填报规则。

你的职责是：
1. 解答高考政策相关问题，包括录取规则、批次设置、投档规则等
2. 解释志愿填报政策，如平行志愿、顺序志愿、专业调剂等
3. 提供各省招生政策解读
4. 提醒考生注意重要时间节点和事项

回答要求：
1. 准确引用政策文件内容
2. 用通俗易懂的语言解释专业术语
3. 如果检索到的政策信息不足，明确告知用户
4. 不要编造政策信息
5. 建议用户以官方发布的政策文件为准
```

**关键词匹配规则：**

```java
return lower.contains("政策") || lower.contains("规则") || lower.contains("规定")
    || lower.contains("录取") || lower.contains("招生") || lower.contains("加分")
    || lower.contains("批次") || lower.contains("投档") || lower.contains("退档")
    || lower.contains("调剂") || lower.contains("平行志愿") || lower.contains("顺序志愿");
```

---

### 4.5 SchoolInfoAgent（学校信息 Agent）

**职责：** 提供院校信息查询、对比、分析服务

**核心能力：**

| 能力 | 说明 |
|------|------|
| 院校查询 | 按条件查询院校列表 |
| 详情展示 | 展示院校详细信息 |
| 院校对比 | 多个院校对比分析 |
| 专业介绍 | 解答专业相关问题 |

**可用 Tools：**

| Tool | 功能 |
|------|------|
| `queryUniversities` | 查询院校列表 |
| `getUniversityDetail` | 获取院校详情 |
| `searchUniversities` | 搜索院校 |
| `queryMajors` | 查询专业列表 |
| `getMajorDetail` | 获取专业详情 |

**System Prompt：**

```
你是一位专业的院校信息顾问，熟悉全国各类高校的情况。

你的职责是：
1. 解答院校基本信息查询，如院校层次、类型、地理位置等
2. 介绍院校特色专业、优势学科
3. 提供院校对比分析，帮助考生选择
4. 解答专业相关问题，如专业介绍、就业方向等

回答要求：
1. 使用工具查询真实数据，不要编造信息
2. 提供全面、准确的院校信息
3. 对比分析时客观公正，列出优缺点
4. 根据用户需求推荐合适的院校或专业
```

**关键词匹配规则：**

```java
return lower.contains("大学") || lower.contains("学院") || lower.contains("院校")
    || lower.contains("学校") || lower.contains("专业") || lower.contains("排名")
    || lower.contains("简介") || lower.contains("特色") || lower.contains("对比")
    || lower.contains("比较") || lower.contains("区别");
```

---

### 4.6 PersonalityAgent（性格分析 Agent）

**职责：** 分析学生性格特点，推荐适合的专业方向

**核心能力：**

| 能力 | 说明 |
|------|------|
| 性格分析 | MBTI、霍兰德职业兴趣分析 |
| 专业推荐 | 根据性格推荐适合的专业 |
| 职业规划 | 提供职业发展建议 |

**分析框架：**

1. MBTI 性格类型分析
2. 霍兰德职业兴趣分析（RIASEC）
3. 学习风格偏好
4. 价值观和人生目标

**可用 Tools：**

| Tool | 功能 |
|------|------|
| `recommendMajorsByPersonality` | 根据性格推荐专业 |
| `queryMajors` | 查询专业信息 |
| `getMajorDetail` | 获取专业详情 |

**System Prompt：**

```
你是一位专业的职业规划顾问和性格分析师，擅长帮助学生发现自己的潜能和兴趣。

你的职责是：
1. 通过对话了解学生的性格特点、兴趣爱好、学习风格
2. 基于性格分析推荐适合的专业方向
3. 提供职业规划建议
4. 帮助学生认识自己，做出更好的选择

分析框架：
1. MBTI性格类型分析
2. 霍兰德职业兴趣分析（RIASEC）
3. 学习风格偏好
4. 价值观和人生目标

回答要求：
1. 先通过提问了解学生情况，不要急于下结论
2. 分析要客观、全面，避免刻板印象
3. 推荐要具体，说明推荐理由
4. 尊重学生的个人意愿，提供建议而非指令
```

**关键词匹配规则：**

```java
return lower.contains("性格") || lower.contains("兴趣") || lower.contains("爱好")
    || lower.contains("适合") || lower.contains("职业") || lower.contains("规划")
    || lower.contains("测试") || lower.contains("分析") || lower.contains("mbti")
    || lower.contains("霍兰德");
```

---

## 5. Tool 工具介绍

### 5.1 UniversityQueryTool（院校查询工具）

**功能：** 查询院校信息

| 方法 | 功能 | 参数 |
|------|------|------|
| `queryUniversities` | 按条件查询院校列表 | province, level, type, minScore, maxScore, limit |
| `getUniversityDetail` | 获取院校详情 | universityId, universityName |
| `searchUniversities` | 模糊搜索院校 | keyword, limit |

**返回格式示例：**

```
【北京大学】
代码：10001 | 层次：985/211/双一流 | 类型：综合类
所在地：北京市 海淀区 | 性质：公办
排名：1
```

---

### 5.2 ScoreAnalysisTool（分数线分析工具）

**功能：** 分析历年分数线、录取概率

| 方法 | 功能 | 参数 |
|------|------|------|
| `getScoreHistory` | 查询院校历年分数线 | universityId, province, subjectType, years |
| `queryByScore` | 根据分数查询可报考院校 | province, subjectType, score, range, limit |
| `analyzeScoreCompetitiveness` | 分析分数竞争力 | province, subjectType, score, universityId |

**竞争力分析逻辑：**

```java
int diff = score - avgMinScore;

if (diff >= 20) {
    level = "保底";      // 录取概率很高
} else if (diff >= 0) {
    level = "稳妥";      // 录取概率较大
} else if (diff >= -20) {
    level = "冲刺";      // 有一定机会
} else {
    level = "风险较大";   // 录取难度大
}
```

---

### 5.3 CalculatorTool（概率计算工具）

**功能：** 录取概率计算、志愿分配策略

| 方法 | 功能 | 参数 |
|------|------|------|
| `calculateAdmissionProbability` | 计算录取概率 | score, avgMinScore, stdDev |
| `calculatePlanDistribution` | 计算冲稳保分配 | totalCount, aggressive |
| `convertScoreToRank` | 分数位次换算 | province, subjectType, score |
| `calculateCostPerformance` | 计算志愿性价比 | levelScore, majorMatch, cityPreference, probability |

**录取概率计算逻辑：**

```java
if (diff >= 2 * sigma) {
    probability = 0.95;  // 保底
} else if (diff >= sigma) {
    probability = 0.80;  // 稳妥
} else if (diff >= 0) {
    probability = 0.60;  // 较稳妥
} else if (diff >= -sigma) {
    probability = 0.40;  // 冲刺
} else if (diff >= -2 * sigma) {
    probability = 0.20;  // 风险较大
} else {
    probability = 0.05;  // 不建议填报
}
```

**冲稳保分配策略：**

| 策略 | 冲刺 | 稳妥 | 保底 |
|------|------|------|------|
| 激进 | 40% | 35% | 25% |
| 稳健 | 30% | 40% | 30% |

---

### 5.4 MajorQueryTool（专业查询工具）

**功能：** 查询专业信息、根据性格推荐专业

| 方法 | 功能 | 参数 |
|------|------|------|
| `queryMajors` | 查询专业列表 | category, keyword, limit |
| `getMajorDetail` | 获取专业详情 | majorId, majorName |
| `recommendMajorsByPersonality` | 根据性格推荐专业 | personalityType, interests |

**性格-专业匹配规则：**

| 性格类型 | 推荐专业方向 |
|----------|--------------|
| 内向/严谨 | 计算机科学、会计学、数学 |
| 外向/开放 | 市场营销、新闻传播、旅游管理 |
| 兴趣-编程 | 软件工程、人工智能、大数据 |
| 兴趣-写作 | 汉语言文学、新闻学、广告学 |
| 兴趣-设计 | 视觉传达、环境设计、产品设计 |
| 兴趣-研究 | 物理学、化学、生物科学 |

---

## 6. 配置说明

### 6.1 LangChain4j 配置

**配置文件：** `application.yml`

```yaml
langchain4j:
  open-ai:
    api-key: ${DASHSCOPE_API_KEY}
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    chat-model:
      model-name: qwen-plus
      temperature: 0.7
      max-tokens: 4096
    embedding-model:
      model-name: text-embedding-v3
```

**配置类：** `LangChain4jConfig.java`

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai")
public class LangChain4jConfig {
    
    private String apiKey;
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private ChatModelConfig chatModel = new ChatModelConfig();
    private EmbeddingModelConfig embeddingModel = new EmbeddingModelConfig();
    
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .build();
    }
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(embeddingModel.getModelName())
            .build();
    }
}
```

### 6.2 向量存储配置

**开发环境：** 内存存储

```java
@Bean
public EmbeddingStore<TextSegment> embeddingStore() {
    return new InMemoryEmbeddingStore<>();
}
```

**生产环境：** PGVector

```yaml
langchain4j:
  vectorstore:
    pgvector:
      table: gaokao_embeddings
      dimension: 1536
      use-index: true
      index-list-size: 100
      create-table: true
```

### 6.3 会话记忆配置

**开发环境：** 内存存储

```java
@Bean
public ChatMemoryStore chatMemoryStore() {
    return new InMemoryChatMemoryStore();
}
```

**生产环境：** Redis 存储

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 1234
```

**Redis 存储实现：**

```java
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {
    
    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration EXPIRE_TIME = Duration.ofHours(24);
    
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + memoryId);
        return JSON.parseArray(json, ChatMessage.class);
    }
    
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        redisTemplate.opsForValue().set(
            KEY_PREFIX + memoryId, 
            JSON.toJSONString(messages), 
            EXPIRE_TIME
        );
    }
}
```

---

## 7. API 接口

### 7.1 接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/chat` | POST | AI 智能对话（多轮） |
| `/api/ai/recommend/one-click` | POST | 一键生成志愿 |
| `/api/ai/recommend` | POST | 志愿推荐 |
| `/api/ai/policy/qa` | POST | 政策问答 |
| `/api/ai/school/info` | POST | 学校信息查询 |
| `/api/ai/personality/analyze` | POST | 性格分析 |
| `/api/ai/agents` | GET | 获取 Agent 列表 |

### 7.2 接口详情

#### 7.2.1 AI 智能对话

**请求：**

```http
POST /api/ai/chat
Content-Type: application/json

{
    "sessionId": "可选，用于多轮对话",
    "message": "我是河南理科考生，600分，推荐一些学校"
}
```

**响应：**

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "sessionId": "uuid-xxx",
        "content": "根据您的情况，为您推荐以下院校...",
        "agentName": "recommend",
        "success": true,
        "errorMessage": null
    }
}
```

#### 7.2.2 一键生成志愿

**请求：**

```http
POST /api/ai/recommend/one-click
Content-Type: application/json

{
    "sessionId": "可选",
    "score": 600,
    "province": "河南",
    "subjectType": "物理类",
    "personalityDescription": "我喜欢编程，性格比较内向，做事认真仔细",
    "preferredMajors": "计算机",
    "preferredCities": "北京,上海"
}
```

**响应：**

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "agentName": "recommend",
        "content": "根据您的性格分析和分数情况，为您推荐以下志愿方案...",
        "success": true
    }
}
```

#### 7.2.3 志愿推荐

**请求：**

```http
POST /api/ai/recommend
Content-Type: application/json

{
    "score": 600,
    "province": "河南",
    "subjectType": "物理类",
    "preferredMajors": ["计算机", "软件工程"],
    "preferredCities": ["北京", "上海"],
    "preferredLevels": ["985", "211"],
    "acceptPrivate": false,
    "acceptJoint": true,
    "count": 45,
    "preference": "希望去大城市发展"
}
```

#### 7.2.4 政策问答

**请求：**

```http
POST /api/ai/policy/qa?question=平行志愿是怎么录取的
```

**响应：**

```json
{
    "code": 200,
    "data": {
        "agentName": "policy",
        "content": "平行志愿采用\"分数优先，遵循志愿\"的原则...",
        "success": true
    }
}
```

#### 7.2.5 获取 Agent 列表

**请求：**

```http
GET /api/ai/agents
```

**响应：**

```json
{
    "code": 200,
    "data": [
        {"name": "recommend", "description": "志愿推荐专家：根据分数、偏好生成志愿方案"},
        {"name": "policy", "description": "政策问答专家：解答高考政策、录取规则等问题"},
        {"name": "school", "description": "学校信息专家：查询院校详情、专业信息、院校对比分析"},
        {"name": "personality", "description": "性格分析专家：分析学生性格特点，推荐适合的专业方向"}
    ]
}
```

---

## 8. 数据模型

### 8.1 请求模型

#### AgentRequest

```java
@Data
@Builder
public class AgentRequest {
    private String userId;           // 用户ID
    private String sessionId;        // 会话ID
    private String question;         // 用户问题
    private String context;          // 上下文信息
    private Map<String, Object> parameters;  // 额外参数
}
```

#### ChatRequestDTO

```java
@Data
public class ChatRequestDTO {
    private String sessionId;        // 会话ID（可选）
    @NotBlank
    private String message;          // 用户消息
}
```

#### OneClickRecommendDTO

```java
@Data
public class OneClickRecommendDTO {
    private String sessionId;
    @NotNull @Min(0) @Max(750)
    private Integer score;           // 高考分数
    @NotBlank
    private String province;         // 省份
    @NotBlank
    private String subjectType;      // 科类
    @NotBlank
    private String personalityDescription;  // 性格描述
    private String preferredMajors;  // 意向专业
    private String preferredCities;  // 意向城市
    private String preference;       // 其他偏好
}
```

#### RecommendRequestDTO

```java
@Data
public class RecommendRequestDTO {
    @NotNull @Min(0) @Max(750)
    private Integer score;
    @NotBlank
    private String province;
    @NotBlank
    private String subjectType;
    private List<String> preferredMajors;
    private List<String> preferredCities;
    private List<String> preferredLevels;
    private Boolean acceptPrivate = true;
    private Boolean acceptJoint = true;
    @Min(1) @Max(96)
    private Integer count = 45;
    private String preference;
}
```

### 8.2 响应模型

#### AgentResponse

```java
@Data
@Builder
public class AgentResponse {
    private String agentName;        // 处理的 Agent 名称
    private String content;          // 响应内容
    @Builder.Default
    private boolean success = true;  // 是否成功
    private String errorMessage;     // 错误信息
    private Object data;             // 结构化数据
    private Map<String, Object> metadata;  // 元数据
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
```

#### ChatResponseVO

```java
@Data
@Builder
public class ChatResponseVO {
    private String sessionId;
    private String content;
    private String agentName;
    private boolean success;
    private String errorMessage;
}
```

#### RecommendResultVO

```java
@Data
public class RecommendResultVO {
    private String planId;
    private List<ApplicationItem> reachList;   // 冲刺志愿
    private List<ApplicationItem> stableList;  // 稳妥志愿
    private List<ApplicationItem> ensureList;  // 保底志愿
    private String reason;
    
    @Data
    public static class ApplicationItem {
        private Integer order;
        private Long universityId;
        private String universityName;
        private Long majorId;
        private String majorName;
        private Integer minScore;
        private Integer minRank;
        private Integer probability;
        private String reason;
    }
}
```

### 8.3 路由模型

#### AgentRoute

```java
@Data
@Builder
public class AgentRoute {
    private String agent;       // 目标 Agent 名称
    private Double confidence;  // 路由置信度 (0.0-1.0)
    private String reason;      // 路由理由
}
```

---

## 9. 扩展指南

### 9.1 添加新的 Agent

**步骤 1：创建 Agent 类**

```java
@Slf4j
@Component
public class NewAgent implements GaokaoAgent {
    
    private final ChatLanguageModel chatModel;
    private final SomeTool someTool;
    
    public NewAgent(ChatLanguageModel chatModel, SomeTool someTool) {
        this.chatModel = chatModel;
        this.someTool = someTool;
    }
    
    @Override
    public String getName() {
        return "new-agent";
    }
    
    @Override
    public String getDescription() {
        return "新Agent描述";
    }
    
    @Override
    public AgentResponse handle(AgentRequest request) {
        NewAssistant assistant = AiServices.builder(NewAssistant.class)
            .chatLanguageModel(chatModel)
            .tools(someTool)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .systemMessageProvider(memoryId -> SYSTEM_PROMPT)
            .build();
        
        String response = assistant.chat(request.getQuestion());
        return AgentResponse.success(getName(), response);
    }
    
    @Override
    public boolean canHandle(String question) {
        String lower = question.toLowerCase();
        return lower.contains("关键词1") || lower.contains("关键词2");
    }
    
    interface NewAssistant {
        String chat(String message);
    }
    
    private static final String SYSTEM_PROMPT = """
        你的角色定义...
        """;
}
```

**步骤 2：注册到 OrchestratorAgent**

```java
public GaokaoOrchestratorAgent(ChatLanguageModel chatModel,
                               RecommendAgent recommendAgent,
                               PolicyAgent policyAgent,
                               SchoolInfoAgent schoolInfoAgent,
                               PersonalityAgent personalityAgent,
                               NewAgent newAgent) {  // 注入新 Agent
    this.chatModel = chatModel;
    this.agents = new HashMap<>();
    this.agents.put("recommend", recommendAgent);
    this.agents.put("policy", policyAgent);
    this.agents.put("school", schoolInfoAgent);
    this.agents.put("personality", personalityAgent);
    this.agents.put("new-agent", newAgent);  // 注册新 Agent
}
```

### 9.2 添加新的 Tool

**步骤 1：创建 Tool 类**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class NewTool {
    
    private final SomeMapper someMapper;
    
    @Tool("工具功能描述，LLM 会根据此描述决定是否调用")
    public String newFunction(String param1, Integer param2) {
        log.info("调用新工具: param1={}, param2={}", param1, param2);
        
        // 实现逻辑
        return "工具返回结果";
    }
}
```

**步骤 2：注入到 Agent**

```java
public NewAgent(ChatLanguageModel chatModel, NewTool newTool) {
    this.chatModel = chatModel;
    this.newTool = newTool;
}

@Override
public AgentResponse handle(AgentRequest request) {
    NewAssistant assistant = AiServices.builder(NewAssistant.class)
        .chatLanguageModel(chatModel)
        .tools(newTool)  // 注入 Tool
        .build();
    // ...
}
```

### 9.3 切换大模型

**方式一：修改配置文件**

```yaml
langchain4j:
  open-ai:
    base-url: https://api.openai.com/v1  # OpenAI
    # base-url: https://api.deepseek.com/v1  # DeepSeek
    api-key: ${API_KEY}
    chat-model:
      model-name: gpt-4  # 或 deepseek-chat
```

**方式二：自定义 Model 配置**

```java
@Bean
public ChatLanguageModel chatLanguageModel() {
    // 使用其他 LangChain4j 支持的模型
    return QwenChatModel.builder()
        .apiKey(apiKey)
        .modelName("qwen-max")
        .build();
}
```

### 9.4 切换向量数据库

**从 InMemory 切换到 PGVector：**

```java
@Bean
public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
    return PgVectorEmbeddingStore.builder()
        .dataSource(dataSource)
        .table("gaokao_embeddings")
        .dimension(1536)
        .useIndex(true)
        .createTable(true)
        .build();
}
```

**切换到 Milvus：**

```java
@Bean
public EmbeddingStore<TextSegment> embeddingStore() {
    return MilvusEmbeddingStore.builder()
        .host("localhost")
        .port(19530)
        .collectionName("gaokao_embeddings")
        .dimension(1536)
        .build();
}
```

---

## 附录

### A. 包结构

```
com.gaokao.ai/
├── config/
│   ├── LangChain4jConfig.java      # LangChain4j 配置
│   ├── ChatMemoryConfig.java       # 会话记忆配置
│   └── VectorStoreConfig.java      # 向量存储配置
├── agent/
│   ├── GaokaoAgent.java            # Agent 接口
│   ├── GaokaoOrchestratorAgent.java # 主协调器
│   ├── RecommendAgent.java         # 志愿推荐 Agent
│   ├── PolicyAgent.java            # 政策问答 Agent
│   ├── SchoolInfoAgent.java        # 学校信息 Agent
│   ├── PersonalityAgent.java       # 性格分析 Agent
│   └── model/
│       ├── AgentRequest.java       # 请求模型
│       ├── AgentResponse.java      # 响应模型
│       └── AgentRoute.java         # 路由模型
├── tool/
│   ├── UniversityQueryTool.java    # 院校查询工具
│   ├── ScoreAnalysisTool.java      # 分数分析工具
│   ├── CalculatorTool.java         # 概率计算工具
│   └── MajorQueryTool.java         # 专业查询工具
├── memory/
│   └── RedisChatMemoryStore.java   # Redis 会话存储
├── service/
│   └── AIService.java              # AI 服务门面
├── dto/
│   ├── ChatRequestDTO.java         # 对话请求 DTO
│   ├── OneClickRecommendDTO.java   # 一键推荐 DTO
│   └── RecommendRequestDTO.java    # 推荐请求 DTO
└── vo/
    ├── ChatResponseVO.java         # 对话响应 VO
    └── RecommendResultVO.java      # 推荐结果 VO
```

### B. 关键依赖

```xml
<!-- LangChain4j Core -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>1.0.0-beta1</version>
</dependency>

<!-- LangChain4j OpenAI (兼容通义千问) -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>1.0.0-beta1</version>
</dependency>

<!-- LangChain4j PGVector -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>1.0.0-beta1</version>
</dependency>
```

### C. 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [阿里通义千问 API 文档](https://help.aliyun.com/zh/dashscope/)
- [PGVector 官方文档](https://github.com/pgvector/pgvector)