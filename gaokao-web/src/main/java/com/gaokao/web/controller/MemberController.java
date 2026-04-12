package com.gaokao.web.controller;

import com.gaokao.common.result.Result;
import com.gaokao.member.annotation.MemberOnly;
import com.gaokao.member.entity.Member;
import com.gaokao.member.entity.MemberLevel;
import com.gaokao.member.entity.MemberPrivilege;
import com.gaokao.member.service.MemberService;
import com.gaokao.member.service.MemberPrivilegeService;
import com.gaokao.member.vo.MemberPrivilegeVO;
import com.gaokao.member.vo.MemberVO;
import com.gaokao.member.vo.MemberProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 会员控制器
 *
 * API设计：
 * - GET  /api/member/info          - 获取会员信息
 * - GET  /api/member/privileges    - 获取会员权益列表
 * - GET  /api/member/products      - 获取会员商品列表（用于购买）
 * - GET  /api/member/usage         - 获取今日权益使用情况
 */
@Tag(name = "会员管理", description = "会员信息、权益查询、会员商品")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberPrivilegeService memberPrivilegeService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 使用次数缓存Key前缀
     */
    private static final String USAGE_KEY_PREFIX = "gaokao:member:usage:";

    @Operation(summary = "获取会员信息")
    @GetMapping("/info")
    public Result<MemberVO> getMemberInfo(@RequestAttribute("userId") Long userId) {
        Member member = memberService.getMemberByUserId(userId);
        if (member == null) {
            // 创建免费会员
            member = memberService.createFreeMember(userId);
        }

        MemberVO vo = convertToVO(member);
        return Result.success(vo);
    }

    @Operation(summary = "获取会员权益列表")
    @GetMapping("/privileges")
    public Result<List<MemberPrivilegeVO>> getMemberPrivileges(@RequestAttribute("userId") Long userId) {
        Member member = memberService.getMemberByUserId(userId);
        if (member == null) {
            member = memberService.createFreeMember(userId);
        }

        String level = member.getLevel();
        List<MemberPrivilege> privileges = memberPrivilegeService.getPrivilegesByLevel(level);

        // 获取今日使用次数
        String today = LocalDate.now().toString();
        List<MemberPrivilegeVO> voList = new ArrayList<>();
        for (MemberPrivilege privilege : privileges) {
            MemberPrivilegeVO vo = new MemberPrivilegeVO();
            vo.setPrivilegeCode(privilege.getPrivilegeCode());
            vo.setPrivilegeName(privilege.getPrivilegeName());
            vo.setLimitCount(privilege.getLimitCount());
            vo.setDescription(privilege.getDescription());

            // 获取今日已使用次数
            String usageKey = USAGE_KEY_PREFIX + userId + ":" + privilege.getPrivilegeCode() + ":" + today;
            int usedCount = memberService.getUsageCount(usageKey);
            vo.setUsedCount(usedCount);

            voList.add(vo);
        }

        return Result.success(voList);
    }

    @Operation(summary = "获取会员商品列表")
    @GetMapping("/products")
    public Result<List<MemberProductVO>> getMemberProducts() {
        List<MemberProductVO> products = new ArrayList<>();

        // 商品ID对应会员等级
        products.add(createProductVO(1L, "普通会员（年卡）", 98.00, 128.00, 365, "智能推荐、详细录取数据、每日50次AI对话"));
        products.add(createProductVO(2L, "VIP会员（年卡）", 298.00, 398.00, 365, "一键生成志愿、专家答疑、无限AI对话"));
        products.add(createProductVO(3L, "普通会员（月卡）", 19.00, 28.00, 30, "智能推荐、详细录取数据、每日50次AI对话"));
        products.add(createProductVO(4L, "VIP会员（月卡）", 49.00, 68.00, 30, "一键生成志愿、专家答疑、无限AI对话"));

        return Result.success(products);
    }

    @Operation(summary = "获取今日权益使用情况")
    @GetMapping("/usage")
    public Result<List<MemberPrivilegeVO>> getTodayUsage(@RequestAttribute("userId") Long userId) {
        Member member = memberService.getMemberByUserId(userId);
        if (member == null) {
            member = memberService.createFreeMember(userId);
        }

        String level = member.getLevel();
        List<MemberPrivilege> privileges = memberPrivilegeService.getPrivilegesByLevel(level);

        String today = LocalDate.now().toString();
        List<MemberPrivilegeVO> voList = new ArrayList<>();
        for (MemberPrivilege privilege : privileges) {
            MemberPrivilegeVO vo = new MemberPrivilegeVO();
            vo.setPrivilegeCode(privilege.getPrivilegeCode());
            vo.setPrivilegeName(privilege.getPrivilegeName());
            vo.setLimitCount(privilege.getLimitCount());

            String usageKey = USAGE_KEY_PREFIX + userId + ":" + privilege.getPrivilegeCode() + ":" + today;
            vo.setUsedCount(memberService.getUsageCount(usageKey));

            voList.add(vo);
        }

        return Result.success(voList);
    }

    @Operation(summary = "获取AI对话使用次数", description = "返回今日AI对话已用次数和总限制")
    @GetMapping("/ai-usage")
    public Result<AiUsageVO> getAiUsage(@RequestAttribute("userId") Long userId) {
        Member member = memberService.getMemberByUserId(userId);
        if (member == null) {
            member = memberService.createFreeMember(userId);
        }

        String level = member.getLevel();
        MemberPrivilege privilege = memberPrivilegeService.getPrivilege(level, "AI_CHAT");

        if (privilege == null) {
            // 默认配置
            return Result.success(new AiUsageVO(0, 10, 10 - 0, false));
        }

        String today = LocalDate.now().toString();
        String usageKey = USAGE_KEY_PREFIX + userId + ":AI_CHAT:" + today;
        int usedCount = memberService.getUsageCount(usageKey);
        int limitCount = privilege.getLimitCount();
        int remainingCount = limitCount == -1 ? Integer.MAX_VALUE : limitCount - usedCount;
        boolean unlimited = limitCount == -1;

        return Result.success(new AiUsageVO(usedCount, limitCount, remainingCount, unlimited));
    }

    /**
     * AI使用情况VO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AiUsageVO {
        /** 今日已使用次数 */
        private int usedCount;
        /** 每日限制次数，-1表示无限 */
        private int limitCount;
        /** 剩余次数 */
        private int remainingCount;
        /** 是否无限 */
        private boolean unlimited;
    }

    /**
     * 转换为VO
     */
    private MemberVO convertToVO(Member member) {
        MemberVO vo = new MemberVO();
        vo.setUserId(member.getUserId());
        vo.setLevelCode(member.getLevel());
        vo.setLevelName(MemberLevel.fromCode(member.getLevel()).getName());
        vo.setStartTime(member.getStartTime());
        vo.setEndTime(member.getEndTime());
        vo.setStatus(member.getStatus());
        vo.setTotalSpent(member.getTotalSpent());

        // 计算是否有效
        boolean valid = member.getStatus() == 1;
        if (valid && member.getEndTime() != null) {
            valid = member.getEndTime().isAfter(LocalDateTime.now());
        }
        vo.setValid(valid);

        // 计算剩余天数
        if (member.getEndTime() != null && member.getEndTime().isAfter(LocalDateTime.now())) {
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), member.getEndTime());
            vo.setRemainingDays(days);
        } else {
            vo.setRemainingDays(0L);
        }

        return vo;
    }

    /**
     * 创建商品VO
     */
    private MemberProductVO createProductVO(Long id, String name, double price, double originalPrice, int days, String desc) {
        MemberProductVO vo = new MemberProductVO();
        vo.setId(id);
        vo.setName(name);
        vo.setPrice(java.math.BigDecimal.valueOf(price));
        vo.setOriginalPrice(java.math.BigDecimal.valueOf(originalPrice));
        vo.setDurationDays(days);
        vo.setDescription(desc);
        return vo;
    }
}