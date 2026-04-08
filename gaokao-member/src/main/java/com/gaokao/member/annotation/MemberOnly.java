package com.gaokao.member.annotation;

import com.gaokao.member.entity.MemberLevel;

import java.lang.annotation.*;

/**
 * 会员权限注解
 *
 * 使用示例：
 * @MemberOnly(level = MemberLevel.NORMAL)
 * public Result smartRecommend() { ... }
 *
 * 设计说明：
 * - 标注在Controller方法上，表示该方法需要会员权限
 * - level指定最低会员等级要求
 * - privilegeCode指定具体的权益代码，用于次数限制校验
 * - 当用户会员等级低于要求时，抛出BusinessException
 * - 当用户超过权益使用次数限制时，抛出BusinessException
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MemberOnly {

    /**
     * 最低会员等级要求
     * 默认为NORMAL（普通会员）
     */
    MemberLevel level() default MemberLevel.NORMAL;

    /**
     * 权益代码（用于次数限制校验）
     * 例如：AI_CHAT, ONE_CLICK_GENERATE
     * 如果不指定，则只校验会员等级，不校验次数限制
     */
    String privilegeCode() default "";

    /**
     * 权限不足时的提示信息
     */
    String message() default "该功能需要会员权限，请升级会员";
}