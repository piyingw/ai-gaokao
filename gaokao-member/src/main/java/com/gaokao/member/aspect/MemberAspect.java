package com.gaokao.member.aspect;

import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.member.annotation.MemberOnly;
import com.gaokao.member.entity.Member;
import com.gaokao.member.entity.MemberLevel;
import com.gaokao.member.entity.MemberPrivilege;
import com.gaokao.member.service.MemberService;
import com.gaokao.member.service.MemberPrivilegeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员权限校验切面
 *
 * 实现原理：
 * 1. 拦截所有标注了@MemberOnly的方法
 * 2. 从请求中获取用户ID（由AuthInterceptor设置）
 * 3. 查询用户会员信息（优先从Redis缓存获取）
 * 4. 校验会员等级是否满足要求
 * 5. 如果指定了privilegeCode，校验权益使用次数是否超限
 * 6. 记录权益使用次数（Redis计数器）
 *
 * 技术亮点：
 * - AOP实现权限校验，避免代码侵入
 * - Redis缓存会员状态，减少数据库查询
 * - Redis原子计数器实现次数限制
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MemberAspect {

    private final MemberService memberService;
    private final MemberPrivilegeService memberPrivilegeService;

    /**
     * 会员状态缓存Key前缀
     */
    private static final String MEMBER_CACHE_KEY = "gaokao:member:user:";

    /**
     * 会员每日使用次数缓存Key前缀
     */
    private static final String MEMBER_USAGE_KEY = "gaokao:member:usage:";

    /**
     * 环绕通知，拦截所有标注了@MemberOnly的方法
     */
    @Around("@annotation(memberOnly)")
    public Object around(ProceedingJoinPoint point, MemberOnly memberOnly) throws Throwable {
        // 1. 获取用户ID
        Long userId = getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        // 2. 获取会员信息（从缓存获取）
        Member member = memberService.getMemberByUserId(userId);
        if (member == null) {
            // 没有会员记录，创建默认免费会员
            member = memberService.createFreeMember(userId);
        }

        // 3. 校验会员状态
        if (member.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "会员状态异常，请联系客服");
        }

        // 4. 校验会员有效期
        if (member.getEndTime() != null && member.getEndTime().isBefore(LocalDateTime.now())) {
            // 会员已过期，自动降级为免费用户
            memberService.downgradeExpiredMember(userId);
            throw new BusinessException(ResultCode.FORBIDDEN, "会员已过期，请续费");
        }

        // 5. 校验会员等级
        MemberLevel requiredLevel = memberOnly.level();
        MemberLevel currentLevel = MemberLevel.fromCode(member.getLevel());
        if (currentLevel.ordinal() < requiredLevel.ordinal()) {
            // 会员等级不足
            log.warn("会员等级不足：userId={}, current={}, required={}",
                    userId, currentLevel.getCode(), requiredLevel.getCode());
            throw new BusinessException(ResultCode.FORBIDDEN, memberOnly.message());
        }

        // 6. 校验权益使用次数（如果指定了privilegeCode）
        String privilegeCode = memberOnly.privilegeCode();
        if (!privilegeCode.isEmpty()) {
            checkAndRecordPrivilegeUsage(userId, privilegeCode, currentLevel);
        }

        // 7. 执行原方法
        log.debug("会员权限校验通过：userId={}, level={}", userId, currentLevel.getCode());
        return point.proceed();
    }

    /**
     * 校验并记录权益使用次数
     */
    private void checkAndRecordPrivilegeUsage(Long userId, String privilegeCode, MemberLevel level) {
        // 1. 获取权益配置
        MemberPrivilege privilege = memberPrivilegeService.getPrivilege(level.getCode(), privilegeCode);
        if (privilege == null) {
            log.warn("权益配置不存在：level={}, privilegeCode={}", level.getCode(), privilegeCode);
            return;  // 权益不存在，不做限制
        }

        // 2. 检查次数限制
        int limitCount = privilege.getLimitCount();
        if (limitCount == -1) {
            // 无限制
            return;
        }

        // 3. 获取今日已使用次数（Redis计数器）
        String today = LocalDate.now().toString();
        String usageKey = MEMBER_USAGE_KEY + userId + ":" + privilegeCode + ":" + today;
        int usageCount = memberService.getUsageCount(usageKey);

        if (usageCount >= limitCount) {
            log.warn("权益使用次数超限：userId={}, privilege={}, used={}, limit={}",
                    userId, privilegeCode, usageCount, limitCount);
            throw new BusinessException(ResultCode.FORBIDDEN,
                    String.format("今日%s使用次数已达上限（%d次），请明日再试或升级会员",
                            privilege.getPrivilegeName(), limitCount));
        }

        // 4. 增加使用次数
        memberService.incrementUsageCount(usageKey);
    }

    /**
     * 从请求中获取用户ID
     */
    private Long getUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object userId = request.getAttribute("userId");
        return userId != null ? Long.parseLong(userId.toString()) : null;
    }
}