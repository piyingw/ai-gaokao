# 高考志愿填报系统

基于 Spring Boot 3 + LangChain4j 的智能高考志愿填报推荐系统，集成阿里通义千问大模型，提供 AI 志愿推荐、政策问答、学校查询、性格分析等功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | Java 运行环境 |
| Spring Boot | 3.2.5 | 基础框架 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0+ | 主数据库 |
| Elasticsearch | 8.12.2+ | 向量数据库（RAG） |
| Redis | 7.0+ | 缓存/会话存储 |
| LangChain4j | 1.0.0-beta2 | AI 框架 |
| 阿里通义千问 | qwen-plus | 大语言模型 |
| text-embedding-v3 | - | 文本向量化模型 |
| Knife4j | 4.5.0 | API 文档 |
| Hutool | 5.8.26 | 工具库 |
| JWT | 0.12.5 | 认证授权 |

## 项目结构

```
ai-gaokao/
├── gaokao-common/          # 公共模块 - 工具类、异常处理、常量、缓存
├── gaokao-system/          # 系统模块 - 用户、权限管理
├── gaokao-data/            # 数据模块 - 院校、专业、分数线数据服务
├── gaokao-ai/              # AI模块 - LangChain4j Agent 智能服务
└── gaokao-web/             # Web模块 - Controller、配置、启动类
```

### 模块依赖关系

```
gaokao-web
    ├── gaokao-ai
    │   ├── gaokao-data
    │   └── gaokao-common
    ├── gaokao-data
    │   └── gaokao-common
    ├── gaokao-system
    │   └── gaokao-common
    └── gaokao-common
```

## 核心功能

### 1. 用户服务
- 用户注册/登录
- 个人信息管理
- 志愿草稿管理

### 2. 院校服务
- 院校信息查询（分页、筛选）
- 院校详情
- 历年分数线查询
- 院校对比

### 3. 专业服务
- 专业信息查询
- 专业详情
- 就业前景分析

### 4. AI 智能推荐
- AI 一键生成志愿方案
- 冲稳保梯度自动分配
- 录取概率预测
- 推荐方案调整

### 5. 政策问答
- 政策文档管理
- AI 政策问答（RAG）
- 智能客服

### 6. 性格分析
- 基于用户描述进行性格分析
- 推荐适合的专业方向
- 结合志愿推荐系统生成个性化方案

## 核心架构

### AI Agent 架构

项目采用 LangChain4j Agent 架构，实现多 Agent 协作：

```
GaokaoOrchestratorAgent (主协调器)
    ├── RecommendAgent    - 志愿推荐
    ├── PolicyAgent       - 政策问答 (RAG)
    ├── SchoolInfoAgent   - 学校信息查询
    └── PersonalityAgent  - 性格分析
```

**工作流程：**
1. 用户请求进入 `GaokaoOrchestratorAgent`
2. 通过 LLM 进行意图识别和路由
3. 分发到对应的子 Agent 处理
4. 子 Agent 可调用 Tools 查询数据
5. 返回结构化响应

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Elasticsearch 8.12.2+
- Redis 7.0+

### 数据库初始化

```bash
# 初始化 MySQL 数据库
mysql -u root -p < gaokao-web/src/main/resources/db/init.sql

# 启动 Elasticsearch 服务
# 确保 Elasticsearch 服务已启动并运行在配置的主机和端口上
```

### 配置说明

1. 修改配置文件 `application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gaokao
    username: root
    password: ${mysql.password}
  data:
    redis:
      host: ${redis.host}
      port: 6379
      password: ${redis.password}
      database: 0
      timeout: 10000ms

# Elasticsearch 配置
elasticsearch:
  host: ${es.host:localhost}
  port: ${es.port:9200}
  username: ${es.username:}
  password: ${es.password:}
  use-ssl: ${es.use-ssl:false}

# LangChain4j 配置 (阿里通义千问)
langchain4j:
  dashscope:
    api-key: ${dashscope.api-key}
    chat-model:
      model-name: qwen-plus
      temperature: 0.7
      max-tokens: 4096
    embedding-model:
      model-name: text-embedding-v3
```

### 启动项目

```bash
# 编译项目
mvn clean install

# 启动服务
cd gaokao-web
mvn spring-boot:run
```

### 访问地址

- 应用地址: http://localhost:8080
- API 文档: http://localhost:8080/doc.html

## API 接口

### 用户模块 `/api/user`
| 接口 | 方法 | 说明 |
|------|------|------|
| /register | POST | 用户注册 |
| /login | POST | 用户登录 |
| /info | GET | 获取用户信息 |

### 院校模块 `/api/university`
| 接口 | 方法 | 说明 |
|------|------|------|
| /list | GET | 分页查询院校 |
| /{id} | GET | 获取院校详情 |
| /{id}/scores | GET | 获取历年分数线 |

### AI 模块 `/api/ai`
| 接口 | 方法 | 说明 |
|------|------|------|
| /chat | POST | AI 智能对话（多轮） |
| /recommend/one-click | POST | 一键生成志愿 |
| /recommend | POST | 志愿推荐 |
| /policy/qa | POST | 政策问答 |
| /school/info | POST | 学校信息查询 |
| /personality/analyze | POST | 性格分析 |
| /agents | GET | 获取 Agent 列表 |

## AI 功能说明

### 推荐算法

1. **数据检索**: 根据用户分数、省份、科类筛选候选院校
2. **概率计算**: 基于历年分数线计算录取概率
3. **梯度分配**: 按冲稳保策略自动分配志愿
4. **智能推荐**: 结合用户偏好生成个性化方案

### RAG 问答

1. **向量存储**: 使用 Elasticsearch 存储政策文档向量
2. **语义检索**: 根据用户问题检索相关文档
3. **智能回答**: 结合检索结果生成准确回答

### 长期记忆系统

1. **记忆存储**: 使用 Elasticsearch 存储用户交互历史
2. **重要性评分**: 基于用户行为和内容特征计算记忆重要性
3. **时间衰减**: 考虑记忆的时间因素，较旧的记忆重要性逐渐降低
4. **检索机制**: 根据当前上下文检索相关记忆，增强AI对话连贯性

## 部署说明

### 环境变量配置

```bash
# MySQL 密码
export mysql.password=your_mysql_password

# Redis 密码
export redis.password=your_redis_password

# Elasticsearch 配置
export es.host=localhost
export es.port=9200
export es.username=elastic
export es.password=your_elasticsearch_password

# 通义千问 API Key
export dashscope.api-key=your_dashscope_api_key
```

### Docker 部署

```bash
# 构建镜像
docker build -t ai-gaokao .

# 运行容器
docker run -d -p 8080:8080 \
  -e mysql.password=your_mysql_password \
  -e redis.password=your_redis_password \
  -e es.host=your_es_host \
  -e es.port=9200 \
  -e es.username=elastic \
  -e es.password=your_elasticsearch_password \
  -e dashscope.api-key=your_dashscope_api_key \
  ai-gaokao
```

## 开发计划

- [ ] 完善专业服务模块
- [ ] 实现同分去向分析
- [ ] 添加就业去向分析
- [ ] 集成更多大模型（DeepSeek、通义千问）
- [ ] 前端界面开发

## License

MIT License