package com.gaokao.web.controller;

import com.gaokao.ai.agent.GaokaoOrchestratorAgent;
import com.gaokao.ai.agent.model.AgentResponse;
import com.gaokao.ai.dto.ChatRequestDTO;
import com.gaokao.ai.dto.OneClickRecommendDTO;
import com.gaokao.ai.dto.RecommendRequestDTO;
import com.gaokao.ai.service.AIService;
import com.gaokao.ai.vo.ChatResponseVO;
import com.gaokao.ai.vo.RecommendResultVO;
import com.gaokao.common.annotation.RateLimit;
import com.gaokao.common.annotation.RateLimitType;
import com.gaokao.common.result.Result;
import com.gaokao.member.annotation.MemberOnly;
import com.gaokao.member.entity.MemberLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AI 服务控制器
 * 基于LangChain4j Agent架构
 *
 * 会员权益说明：
 * - AI对话：免费用户每日10次，普通会员50次，VIP无限
 * - 一键生成志愿：VIP专属功能
 * - 志愿推荐：普通会员以上可用
 */
@Tag(name = "AI 服务", description = "智能推荐、政策问答、学校查询、性格分析")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final GaokaoOrchestratorAgent orchestratorAgent;

    @Operation(summary = "AI 智能对话", description = "支持多轮对话，自动识别意图并路由到对应Agent")
    @MemberOnly(privilegeCode = "AI_CHAT", message = "AI对话次数已达上限，请升级会员")
    @RateLimit(permits = 10, timeWindow = 60, type = RateLimitType.USER, message = "对话请求过于频繁，请稍后再试")
    @PostMapping("/chat")
    public Result<ChatResponseVO> chat(
            @RequestAttribute(value = "userId", required = false) String userId,
            @Valid @RequestBody ChatRequestDTO request) {

        // 如果没有sessionId，生成一个
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }

        ChatResponseVO response = aiService.chat(userId, request);
        return Result.success(response);
    }

    @Operation(summary = "一键生成志愿", description = "基于学生性格特点，一键生成个性化志愿方案（VIP专属）")
    @MemberOnly(level = MemberLevel.VIP, message = "一键生成志愿为VIP专属功能，请升级会员")
    @RateLimit(permits = 5, timeWindow = 60, type = RateLimitType.USER, message = "一键生成请求过于频繁，请稍后再试")
    @PostMapping("/recommend/one-click")
    public Result<AgentResponse> oneClickRecommend(
            @RequestAttribute(value = "userId", required = false) String userId,
            @Valid @RequestBody OneClickRecommendDTO dto) {

        AgentResponse response = aiService.oneClickRecommend(userId, dto);
        return Result.success(response);
    }

    @Operation(summary = "志愿推荐", description = "根据分数、偏好等信息推荐志愿（普通会员以上）")
    @MemberOnly(level = MemberLevel.NORMAL, privilegeCode = "SMART_RECOMMEND", message = "智能推荐需要会员权限，请升级会员")
    @RateLimit(permits = 8, timeWindow = 60, type = RateLimitType.USER, message = "推荐请求过于频繁，请稍后再试")
    @PostMapping("/recommend")
    public Result<AgentResponse> recommend(
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestBody RecommendRequestDTO dto) {

        String question = buildRecommendQuestion(dto);
        AgentResponse response = aiService.recommend(userId, question);
        return Result.success(response);
    }

    @Operation(summary = "政策问答", description = "解答高考政策、录取规则等问题")
    @RateLimit(permits = 15, timeWindow = 60, type = RateLimitType.USER, message = "政策问答请求过于频繁，请稍后再试")
    @PostMapping("/policy/qa")
    public Result<AgentResponse> policyQA(
            @RequestAttribute(value = "userId", required = false) String userId,
            @Parameter(description = "问题内容") @RequestParam String question) {

        AgentResponse response = aiService.policyQA(userId, question);
        return Result.success(response);
    }

    @Operation(summary = "学校信息查询", description = "查询院校详情、专业信息、院校对比")
    @RateLimit(permits = 20, timeWindow = 60, type = RateLimitType.USER, message = "学校查询请求过于频繁，请稍后再试")
    @PostMapping("/school/info")
    public Result<AgentResponse> schoolInfo(
            @RequestAttribute(value = "userId", required = false) String userId,
            @Parameter(description = "查询内容") @RequestParam String query) {

        AgentResponse response = aiService.schoolInfo(userId, query);
        return Result.success(response);
    }

    @Operation(summary = "性格分析", description = "分析学生性格特点，推荐适合的专业方向")
    @RateLimit(permits = 5, timeWindow = 60, type = RateLimitType.USER, message = "性格分析请求过于频繁，请稍后再试")
    @PostMapping("/personality/analyze")
    public Result<AgentResponse> personalityAnalysis(
            @RequestAttribute(value = "userId", required = false) String userId,
            @Parameter(description = "性格描述") @RequestParam String description) {

        AgentResponse response = aiService.personalityAnalysis(userId, description);
        return Result.success(response);
    }

    @Operation(summary = "获取Agent列表", description = "获取系统可用的Agent信息")
    @GetMapping("/agents")
    public Result<Object> getAgents() {
        return Result.success(orchestratorAgent.getAgentInfos());
    }

    /**
     * 构建推荐问题
     */
    private String buildRecommendQuestion(RecommendRequestDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("我是%s省%s考生，高考分数%d分。",
                dto.getProvince(), dto.getSubjectType(), dto.getScore()));

        if (dto.getPreferredMajors() != null && !dto.getPreferredMajors().isEmpty()) {
            sb.append("意向专业：").append(String.join("、", dto.getPreferredMajors())).append("。");
        }
        if (dto.getPreferredCities() != null && !dto.getPreferredCities().isEmpty()) {
            sb.append("意向城市：").append(String.join("、", dto.getPreferredCities())).append("。");
        }
        if (dto.getPreferredLevels() != null && !dto.getPreferredLevels().isEmpty()) {
            sb.append("意向院校层次：").append(String.join("、", dto.getPreferredLevels())).append("。");
        }
        if (dto.getPreference() != null && !dto.getPreference().isEmpty()) {
            sb.append("其他偏好：").append(dto.getPreference()).append("。");
        }

        sb.append("请为我推荐").append(dto.getCount()).append("个志愿，按照冲稳保策略分类。");

        return sb.toString();
    }
}