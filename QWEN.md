# 高考志愿填报系统 (ai-gaokao)

## 项目概述

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

### 包结构规范

```
com.gaokao.{module}/
├── config/          # 配置类
├── controller/      # 控制器 (仅 web 模块)
├── service/         # 服务层
│   └── impl/        # 服务实现
├── mapper/          # MyBatis Mapper
├── entity/          # 数据库实体
├── dto/             # 数据传输对象
├── vo/              # 视图对象
├── agent/           # AI Agent (仅 ai 模块)
├── tool/            # AI Tool (仅 ai 模块)
├── store/           # 存储服务 (仅 ai 模块)
├── service/         # 服务层 (仅 ai 模块)
├── memory/          # 会话记忆 (仅 ai 模块)
├── mcp/             # MCP协议实现 (仅 ai 模块)
└── constant/        # 常量
```

## 构建与运行

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

### 配置文件

主要配置位于 `gaokao-web/src/main/resources/application.yml`：

```yaml
# 关键配置项
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

# 通义千问 API
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

### 构建命令

```bash
# 完整构建
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 仅编译
mvn clean compile
```

### 运行命令

```bash
# 方式一：Maven 运行
cd gaokao-web
mvn spring-boot:run

# 方式二：JAR 运行
java -jar gaokao-web/target/gaokao-web-1.0.0.jar

# 方式三：指定配置运行
java -jar gaokao-web/target/gaokao-web-1.0.0.jar \
  --spring.datasource.password=your_password \
  --langchain4j.dashscope.api-key=your_api_key
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

## 开发规范

### 代码风格

- 使用 Lombok 简化代码（@Data, @Builder, @RequiredArgsConstructor）
- 统一响应格式：`Result<T>`
- 异常处理：使用 `BusinessException` + `GlobalExceptionHandler`
- 日志规范：使用 Slf4j + Lombok @Slf4j

### 命名规范

- 类名：大驼峰（PascalCase）
- 方法名/变量：小驼峰（camelCase）
- 常量：全大写下划线分隔（UPPER_SNAKE_CASE）
- 包名：全小写

### 数据库规范

- 表名：小写下划线分隔（snake_case）
- 主键：使用雪花算法（ASSIGN_ID）
- 逻辑删除：deleted 字段（0-未删除，1-已删除）
- 时间字段：create_time, update_time（自动填充）

### API 规范

- RESTful 风格
- 统一返回 `Result<T>` 包装
- 使用 Knife4j 注解编写 API 文档
- 请求参数使用 `@Valid` 校验

### Agent 开发规范

1. 实现 `GaokaoAgent` 接口
2. 使用 `@Component` 注册为 Spring Bean
3. 在 `GaokaoOrchestratorAgent` 中注册 Agent
4. Tools 使用 `@Tool` 注解定义

### 长期记忆系统开发规范

1. 使用Elasticsearch作为长期记忆存储
2. 实现重要性评分机制
3. 考虑时间衰减因素
4. 提供基于上下文的记忆检索功能

## 数据库表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| university | 院校表 |
| major | 专业表 |
| admission_score | 历年分数线表 |
| user_application | 用户志愿表 |
| policy_document | 政策文档表 |
| gaokao_embeddings | 向量存储表 (Elasticsearch) |

## 常见问题

### Q: 如何添加新的 AI Agent？

1. 在 `gaokao-ai` 模块创建 Agent 类实现 `GaokaoAgent` 接口
2. 添加 `@Component` 注解
3. 在 `GaokaoOrchestratorAgent` 构造函数中注入并注册

### Q: 如何添加新的数据表？

1. 在 `gaokao-data` 模块创建 Entity 类
2. 创建对应 Mapper 接口
3. 创建 Service 接口和实现类
4. 在 `gaokao-web` 创建 Controller

### Q: 如何切换大模型？

修改 `application.yml` 中的 LangChain4j 配置，支持：
- 阿里通义千问（当前）
- OpenAI
- 其他 LangChain4j 支持的模型

### Q: 如何配置Elasticsearch向量数据库？

1. 确保Elasticsearch服务已启动
2. 在 `application.yml` 中配置Elasticsearch连接参数
3. 系统会自动创建所需的索引

### Q: 如何使用长期记忆功能？

1. 通过 `LongTermMemoryService` 进行记忆的增删改查
2. 系统会自动计算记忆的重要性和时间衰减
3. 在AI对话中可以检索相关的长期记忆

## 待开发功能

- [ ] 完善专业服务模块
- [ ] 实现同分去向分析
- [ ] 添加就业去向分析
- [ ] 集成更多大模型（DeepSeek、通义千问）
- [ ] 前端界面开发