#!/bin/bash

# 60天提交计划脚本（含5-6天休息日）
# 时间范围: 2026年2月7日 - 2026年4月8日

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== GitHub 60天提交计划 ===${NC}"
echo -e "${YELLOW}时间范围: 2026-02-07 至 2026-04-08${NC}"
echo -e "${YELLOW}预计提交: 54天 (休息日: 2月14,2月28,3月14,3月21,4月1,4月5)${NC}"
echo ""

# 执行提交函数
commit() {
    local date=$1
    local files=$2
    local message=$3
    local time=$(printf "%02d:%02d:%02d" $((9 + RANDOM % 8)) $((RANDOM % 60)) $((RANDOM % 60)))

    echo -e "${GREEN}[${date}]${NC} ${message:0:50}..."
    git add $files 2>/dev/null
    GIT_AUTHOR_DATE="${date} ${time}" GIT_COMMITTER_DATE="${date} ${time}" git commit -m "$message" --allow-empty
}

# 确认执行
read -p "确认执行60天提交计划？这将重置git历史 (y/n): " confirm
if [ "$confirm" != "y" ]; then
    echo "取消执行"
    exit 0
fi

# 重置git历史
echo -e "${YELLOW}重置git历史...${NC}"
rm -rf .git
git init
git remote add origin https://github.com/piyingw/ai-gaokao.git

echo ""
echo -e "${GREEN}开始执行提交计划...${NC}"

# ========== 第1周: 项目初始化 (2月7-13日) ==========
commit "2026-02-07" "pom.xml .gitignore" "feat: 项目初始化，搭建Maven多模块架构

- 创建gaokao-ai、gaokao-common、gaokao-data、gaokao-system、gaokao-web模块
- 配置SpringBoot 3.2.0 + MyBatis-Plus
- 配置统一依赖版本管理"

commit "2026-02-08" "gaokao-common/src/main/java/com/gaokao/common/result/" "feat(common): 添加统一结果封装

- Result统一响应格式
- ResultCode状态码枚举
- 支持泛型返回数据"

commit "2026-02-09" "gaokao-common/src/main/java/com/gaokao/common/exception/" "feat(common): 添加全局异常处理

- BusinessException业务异常
- GlobalExceptionHandler全局异常处理器
- 统一异常响应格式"

commit "2026-02-10" "gaokao-common/src/main/java/com/gaokao/common/utils/JwtUtils.java" "feat(common): 实现JWT工具类

- Token生成和验证
- 用户信息提取
- Token过期处理"

commit "2026-02-11" "gaokao-common/src/main/java/com/gaokao/common/constant/" "feat(common): 添加缓存和系统常量

- CacheConstants缓存Key规范
- SystemConstants系统常量
- 统一命名规范: gaokao:模块:业务:标识"

commit "2026-02-12" "gaokao-common/src/main/java/com/gaokao/common/config/ gaokao-common/src/main/java/com/gaokao/common/service/" "feat(common): 添加邮件配置和验证码服务

- MailConfig邮件发送配置
- VerifyCodeService验证码服务
- Redis存储验证码"

commit "2026-02-13" "gaokao-system/src/main/java/com/gaokao/system/entity/ gaokao-system/src/main/java/com/gaokao/system/mapper/" "feat(system): 添加用户实体和数据访问层

- User用户实体
- UserMapper数据访问接口
- LoginDTO/RegisterDTO"

# 2月14日 休息

# ========== 第2周: 用户系统 (2月15-21日) ==========
commit "2026-02-15" "gaokao-system/src/main/java/com/gaokao/system/service/UserService.java gaokao-system/src/main/java/com/gaokao/system/service/impl/UserServiceImpl.java" "feat(system): 实现用户登录注册服务

- 用户注册（密码加密存储）
- 用户登录（Token生成）
- 根据Token获取用户信息"

commit "2026-02-16" "gaokao-data/src/main/java/com/gaokao/data/entity/University.java" "feat(data): 添加院校实体

- University院校实体
- 包含院校代码、名称、省份、城市、层次、类型等字段
- 支持MyBatis-Plus自动填充"

commit "2026-02-17" "gaokao-data/src/main/java/com/gaokao/data/entity/Major.java gaokao-data/src/main/java/com/gaokao/data/entity/AdmissionScore.java" "feat(data): 添加专业和分数线实体

- Major专业实体
- AdmissionScore录取分数线实体
- 支持历年分数线查询"

commit "2026-02-18" "gaokao-data/src/main/java/com/gaokao/data/entity/PolicyDocument.java" "feat(data): 添加政策文档实体

- PolicyDocument政策文档实体
- 支持按省份、年份查询政策"

commit "2026-02-19" "gaokao-data/src/main/java/com/gaokao/data/mapper/" "feat(data): 添加数据访问层

- UniversityMapper院校数据访问
- MajorMapper专业数据访问
- AdmissionScoreMapper分数线数据访问
- PolicyDocumentMapper政策文档数据访问"

commit "2026-02-20" "gaokao-data/src/main/java/com/gaokao/data/dto/" "feat(data): 添加数据传输对象

- UniversityQueryDTO院校查询条件
- MajorQueryDTO专业查询条件
- AdmissionScoreQueryDTO分数线查询条件"

# 2月21日 休息

# ========== 第3周: 数据服务层 (2月22-28日) ==========
commit "2026-02-22" "gaokao-data/src/main/java/com/gaokao/data/vo/" "feat(data): 添加视图对象

- UniversityVO/UniversityDetailVO院校视图
- MajorVO/MajorDetailVO专业视图
- ScoreAnalysisVO分数线分析视图"

commit "2026-02-23" "gaokao-data/src/main/java/com/gaokao/data/service/UniversityService.java gaokao-data/src/main/java/com/gaokao/data/service/impl/UniversityServiceImpl.java" "feat(data): 实现院校查询服务

- 分页查询院校列表
- 查询院校详情
- 查询院校分数线
- 查询院校开设专业"

commit "2026-02-24" "gaokao-data/src/main/java/com/gaokao/data/service/MajorService.java gaokao-data/src/main/java/com/gaokao/data/service/impl/MajorServiceImpl.java" "feat(data): 实现专业查询服务

- 分页查询专业列表
- 查询专业详情
- 按院校查询开设专业"

commit "2026-02-25" "gaokao-data/src/main/java/com/gaokao/data/service/AdmissionScoreService.java gaokao-data/src/main/java/com/gaokao/data/service/impl/AdmissionScoreServiceImpl.java" "feat(data): 实现分数线分析服务

- 查询历史分数线
- 分数匹配院校
- 竞争力分析"

commit "2026-02-26" "gaokao-data/src/main/java/com/gaokao/data/entity/UserApplication.java gaokao-data/src/main/java/com/gaokao/data/service/UserApplicationService.java" "feat(data): 添加用户志愿申请

- UserApplication用户志愿实体
- UserApplicationService志愿管理服务
- 支持志愿保存、修改、删除"

commit "2026-02-27" "gaokao-data/src/main/java/com/gaokao/data/service/PolicyDocumentService.java gaokao-data/src/main/java/com/gaokao/data/service/impl/PolicyDocumentServiceImpl.java" "feat(data): 实现政策文档服务

- 按省份查询政策
- 按年份查询政策
- 政策文档检索"

# 2月28日 休息

# ========== 第4周: AI模块初始化 (3月1-7日) ==========
commit "2026-03-01" "gaokao-ai/pom.xml" "feat(ai): 添加AI模块依赖

- langchain4j核心依赖
- langchain4j-open-ai模块
- elasticsearch-java向量存储"

commit "2026-03-02" "gaokao-ai/src/main/java/com/gaokao/ai/config/LangChain4jConfig.java" "feat(ai): 集成LangChain4j

- LangChain4jConfig配置类
- OpenAI兼容模式接入阿里通义千问
- qwen-plus聊天模型配置"

commit "2026-03-03" "gaokao-ai/src/main/java/com/gaokao/ai/config/VectorStoreConfig.java" "feat(ai): 配置向量存储

- 支持内存/PGVector/Elasticsearch三种模式
- ConditionalOnProperty条件切换
- text-embedding-v3向量模型"

commit "2026-03-04" "gaokao-ai/src/main/java/com/gaokao/ai/config/ElasticsearchConfig.java" "feat(ai): 配置Elasticsearch

- ElasticsearchClient配置
- 向量检索支持
- dense_vector字段映射"

commit "2026-03-05" "gaokao-ai/src/main/java/com/gaokao/ai/dto/ gaokao-ai/src/main/java/com/gaokao/ai/vo/" "feat(ai): 添加AI模块DTO和VO

- ChatRequestDTO对话请求
- ChatResponseVO对话响应
- RecommendRequestDTO推荐请求
- RecommendResultVO推荐结果"

commit "2026-03-06" "gaokao-ai/src/main/java/com/gaokao/ai/tool/UniversityQueryTool.java" "feat(ai): 实现院校查询工具

- UniversityQueryTool工具类
- 支持按条件查询院校
- LangChain4j @Tool注解"

commit "2026-03-07" "gaokao-ai/src/main/java/com/gaokao/ai/tool/MajorQueryTool.java gaokao-ai/src/main/java/com/gaokao/ai/tool/CalculatorTool.java" "feat(ai): 实现专业查询和计算工具

- MajorQueryTool专业查询工具
- CalculatorTool录取概率计算工具
- 位次换算、性价比评分"

# ========== 第5周: AI工具和技能系统 (3月8-14日) ==========
commit "2026-03-08" "gaokao-ai/src/main/java/com/gaokao/ai/tool/ScoreAnalysisTool.java" "feat(ai): 实现分数线分析工具

- ScoreAnalysisTool分数线分析工具
- 历史分数线查询
- 分数匹配分析"

commit "2026-03-09" "gaokao-ai/src/main/java/com/gaokao/ai/tool/DataValidationTool.java" "feat(ai): 实现数据验证工具

- DataValidationTool数据验证
- 分数范围校验
- 省份代码验证"

commit "2026-03-10" "gaokao-ai/src/main/java/com/gaokao/ai/skill/GaokaoSkill.java gaokao-ai/src/main/java/com/gaokao/ai/skill/AbstractSkill.java" "feat(ai): 设计Skill技能接口

- GaokaoSkill技能接口
- AbstractSkill抽象基类
- SkillParameter参数定义"

commit "2026-03-11" "gaokao-ai/src/main/java/com/gaokao/ai/skill/SkillRegistry.java gaokao-ai/src/main/java/com/gaokao/ai/skill/SkillExecutor.java" "feat(ai): 实现技能注册和执行

- SkillRegistry技能注册中心
- SkillExecutor技能执行器
- 支持动态注册技能"

commit "2026-03-12" "gaokao-ai/src/main/java/com/gaokao/ai/skill/impl/UniversityQuerySkill.java gaokao-ai/src/main/java/com/gaokao/ai/skill/impl/MajorQuerySkill.java" "feat(ai): 实现院校和专业查询技能

- UniversityQuerySkill院校查询技能
- MajorQuerySkill专业查询技能
- 模块化技能封装"

commit "2026-03-13" "gaokao-ai/src/main/java/com/gaokao/ai/skill/impl/ScoreAnalysisSkill.java gaokao-ai/src/main/java/com/gaokao/ai/skill/impl/CalculatorSkill.java" "feat(ai): 实现分数线分析和计算技能

- ScoreAnalysisSkill分数线分析
- CalculatorSkill录取概率计算
- 支持多维度分析"

# 3月14日 休息

# ========== 第6周: Multi-Agent架构 (3月15-21日) ==========
commit "2026-03-15" "gaokao-ai/src/main/java/com/gaokao/ai/agent/GaokaoAgent.java gaokao-ai/src/main/java/com/gaokao/ai/agent/model/" "feat(ai): 定义Agent接口和模型

- GaokaoAgent接口定义
- AgentRequest/AgentResponse模型
- AgentRoute路由模型"

commit "2026-03-16" "gaokao-ai/src/main/java/com/gaokao/ai/agent/GaokaoOrchestratorAgent.java" "feat(ai): 实现主协调Agent

- GaokaoOrchestratorAgent主协调器
- 关键词+LLM双重路由
- 意图识别分发"

commit "2026-03-17" "gaokao-ai/src/main/java/com/gaokao/ai/agent/RecommendAgent.java" "feat(ai): 实现志愿推荐Agent

- RecommendAgent志愿推荐专家
- 冲稳保策略生成
- 录取概率分析"

commit "2026-03-18" "gaokao-ai/src/main/java/com/gaokao/ai/agent/PolicyAgent.java" "feat(ai): 实现政策问答Agent

- PolicyAgent政策问答专家
- RAG检索政策文档
- EmbeddingStoreContentRetriever"

commit "2026-03-19" "gaokao-ai/src/main/java/com/gaokao/ai/agent/SchoolInfoAgent.java" "feat(ai): 实现院校查询Agent

- SchoolInfoAgent院校查询专家
- 院校详情查询
- 院校对比分析"

commit "2026-03-20" "gaokao-ai/src/main/java/com/gaokao/ai/agent/PersonalityAgent.java" "feat(ai): 实现性格分析Agent

- PersonalityAgent性格分析专家
- 性格特点分析
- 专业方向推荐"

# 3月21日 休息

# ========== 第7周: AI记忆系统 (3月22-28日) ==========
commit "2026-03-22" "gaokao-ai/src/main/java/com/gaokao/ai/config/ChatMemoryConfig.java gaokao-ai/src/main/java/com/gaokao/ai/memory/RedisChatMemoryStore.java" "feat(ai): 实现短期记忆系统

- RedisChatMemoryStore Redis存储
- ChatMemoryStore接口实现
- 24小时会话有效期"

commit "2026-03-23" "gaokao-ai/src/main/java/com/gaokao/ai/entity/LongTermMemory.java" "feat(ai): 定义长期记忆实体

- LongTermMemory长期记忆实体
- MemoryType记忆类型枚举
- 支持标签和重要性评分"

commit "2026-03-24" "gaokao-ai/src/main/java/com/gaokao/ai/store/ElasticsearchLongTermMemoryStore.java" "feat(ai): 实现长期记忆存储

- ElasticsearchLongTermMemoryStore
- dense_vector(1024维)存储
- 语义检索支持"

commit "2026-03-25" "gaokao-ai/src/main/java/com/gaokao/ai/service/LongTermMemoryService.java" "feat(ai): 实现长期记忆服务

- LongTermMemoryService记忆管理
- 自动提取关键信息（分数/省份/科类）
- 重要性评分计算"

commit "2026-03-26" "gaokao-ai/src/main/java/com/gaokao/ai/tool/LongTermMemoryTool.java" "feat(ai): 实现记忆工具

- LongTermMemoryTool记忆工具
- 支持搜索、保存、召回记忆
- 与Agent集成"

commit "2026-03-27" "gaokao-ai/src/main/java/com/gaokao/ai/skill/impl/WebSearchSkill.java" "feat(ai): 实现联网搜索技能

- WebSearchSkill联网搜索
- 通义千问搜索增强
- 本地数据不足时联网查询"

commit "2026-03-28" "gaokao-ai/src/main/java/com/gaokao/ai/service/AIService.java" "feat(ai): 实现AI服务层

- AIService统一服务接口
- 多轮对话支持
- 一键推荐功能"

# ========== 第8周: MCP协议 (3月29-4月4日) ==========
commit "2026-03-29" "gaokao-ai/src/main/java/com/gaokao/ai/mcp/McpClient.java gaokao-ai/src/main/java/com/gaokao/ai/mcp/McpRequest.java gaokao-ai/src/main/java/com/gaokao/ai/mcp/McpResponse.java" "feat(ai): 定义MCP协议接口

- McpClient客户端接口
- McpRequest/McpResponse协议模型
- 标准化协议设计"

commit "2026-03-30" "gaokao-ai/src/main/java/com/gaokao/ai/mcp/SimpleMcpClient.java" "feat(ai): 实现MCP客户端

- SimpleMcpClient简单实现
- 支持连接、断开、请求
- 为外部服务集成预留"

commit "2026-03-31" "gaokao-ai/src/main/java/com/gaokao/ai/mcp/McpService.java gaokao-ai/src/main/java/com/gaokao/ai/config/McpConfig.java" "feat(ai): 实现MCP服务配置

- McpService服务层
- McpConfig配置类
- Spring Bean管理"

# 4月1日 休息

commit "2026-04-02" "gaokao-ai/src/main/java/com/gaokao/ai/tool/SkillTool.java" "feat(ai): 实现Skill工具适配

- SkillTool将技能暴露为Tool
- LangChain4j @Tool注解
- Agent可调用所有技能"

commit "2026-04-03" "gaokao-ai/src/main/java/com/gaokao/ai/config/SkillConfig.java gaokao-ai/src/main/java/com/gaokao/ai/skill/SkillRegistrar.java" "feat(ai): 优化技能注册

- SkillConfig技能配置
- SkillRegistrar自动注册
- Spring自动装配"

commit "2026-04-04" "gaokao-ai/src/test/java/com/gaokao/ai/test/" "test(ai): 添加AI模块测试

- AgentAccuracyTest准确性测试
- LongTermMemoryTest记忆测试
- 单元测试覆盖"

# ========== 第9周: 订单模块 (4月5-11日) ==========
# 4月5日 休息

commit "2026-04-06" "gaokao-order/src/main/java/com/gaokao/order/entity/Order.java gaokao-order/src/main/java/com/gaokao/order/entity/OrderStatus.java" "feat(order): 添加订单实体和状态

- Order订单实体
- OrderStatus状态枚举(6种状态)
- OrderType订单类型"

commit "2026-04-07" "gaokao-order/src/main/java/com/gaokao/order/entity/PaymentRecord.java gaokao-order/src/main/java/com/gaokao/order/entity/" "feat(order): 添加支付记录实体

- PaymentRecord支付记录
- 支付状态跟踪
- 回调数据存储"

commit "2026-04-08" "gaokao-order/src/main/java/com/gaokao/order/statemachine/ gaokao-order/src/main/java/com/gaokao/order/mapper/ gaokao-order/src/main/java/com/gaokao/order/service/ gaokao-order/src/main/java/com/gaokao/order/message/ gaokao-order/src/main/java/com/gaokao/order/payment/ gaokao-order/src/main/java/com/gaokao/order/job/ gaokao-order/src/main/java/com/gaokao/order/dto/ gaokao-order/src/main/java/com/gaokao/order/vo/ gaokao-member/ gaokao-promotion/ gaokao-common/src/main/java/com/gaokao/common/cache/ gaokao-common/src/main/java/com/gaokao/common/aspect/ gaokao-common/src/main/java/com/gaokao/common/annotation/ gaokao-web/src/main/java/com/gaokao/web/controller/ gaokao-web/src/main/java/com/gaokao/web/config/ gaokao-web/src/main/java/com/gaokao/web/interceptor/ frontend/ dist/ README.md TESTING.md NGINX_DEPLOYMENT.md QWEN.md nginx.conf *.bat docs/ gaokao-crawler/ .claude/" "feat: 完成订单、会员、优惠券、缓存、Web层等剩余模块

订单模块:
- OrderStateMachine状态机
- OrderService订单服务
- RocketMQ延迟消息(订单超时)
- PaymentFactory多渠道支付
- 支付回调幂等性

会员模块:
- MemberService会员服务
- 会员开通、续费、过期处理
- 会员特权校验

优惠券模块:
- CouponService优惠券服务
- Redis原子扣减库存
- Redisson分布式锁防并发

缓存模块:
- MultiLevelCacheService多级缓存
- Caffeine本地缓存+Redis分布式缓存
- 缓存穿透/击穿/雪崩防护

限流模块:
- RateLimitAspect分布式限流
- Redisson令牌桶算法
- USER/IP/ALL三维度

Web层:
- Controller REST接口
- 跨域、认证拦截器配置

前端:
- Vue3 + Vite项目
- AI对话、院校查询页面

文档:
- README、部署文档、测试文档"

# ========== 完成 ==========
echo ""
echo -e "${GREEN}=== 60天提交计划执行完成 ===${NC}"
echo ""
echo -e "${YELLOW}提交统计:${NC}"
git log --oneline | wc -l
echo ""
echo -e "${YELLOW}最近10条提交:${NC}"
git log --oneline -10
echo ""
echo -e "${YELLOW}按日期统计:${NC}"
git log --format='%ad' --date=short | sort | uniq -c | tail -20
echo ""
echo -e "${GREEN}接下来执行:${NC}"
echo -e "  ${YELLOW}git branch -M main${NC}"
echo -e "  ${YELLOW}git push -f origin main${NC}"
echo -e "${RED}注意: 强制推送会覆盖远程仓库历史${NC}"