package com.gaokao.member.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.member.entity.MemberPrivilege;
import com.gaokao.member.mapper.MemberPrivilegeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 会员权益数据初始化器
 *
 * 在应用启动时自动初始化会员权益配置数据
 * 确保不同会员等级的AI对话次数限制生效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPrivilegeInitializer implements CommandLineRunner {

    private final MemberPrivilegeMapper memberPrivilegeMapper;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化会员权益配置...");

        // 检查是否已有数据
        Long count = memberPrivilegeMapper.selectCount(
                new LambdaQueryWrapper<MemberPrivilege>()
                        .eq(MemberPrivilege::getStatus, 1)
        );

        if (count > 0) {
            log.info("会员权益配置已存在，跳过初始化。当前共{}条配置", count);
            return;
        }

        // 初始化权益配置数据
        initFreePrivileges();
        initNormalPrivileges();
        initVipPrivileges();

        log.info("会员权益配置初始化完成！");
    }

    /**
     * 免费用户权益
     * - AI对话：每日10次
     * - 院校/专业查询：无限
     */
    private void initFreePrivileges() {
        createPrivilege(1L, "FREE", "AI_CHAT", "AI智能对话", 10, "每日AI对话次数限制为10次");
        createPrivilege(2L, "FREE", "UNIVERSITY_QUERY", "院校查询", -1, "不限次数查询院校信息");
        createPrivilege(3L, "FREE", "MAJOR_QUERY", "专业查询", -1, "不限次数查询专业信息");
        createPrivilege(4L, "FREE", "BASIC_RECOMMEND", "基础推荐", 3, "每日基础志愿推荐次数限制为3次");
    }

    /**
     * 普通会员权益
     * - AI对话：每日50次
     * - 智能推荐：无限
     */
    private void initNormalPrivileges() {
        createPrivilege(5L, "NORMAL", "AI_CHAT", "AI智能对话", 50, "每日AI对话次数限制为50次");
        createPrivilege(6L, "NORMAL", "UNIVERSITY_QUERY", "院校查询", -1, "不限次数查询院校信息");
        createPrivilege(7L, "NORMAL", "MAJOR_QUERY", "专业查询", -1, "不限次数查询专业信息");
        createPrivilege(8L, "NORMAL", "SMART_RECOMMEND", "智能推荐", -1, "不限次数使用智能推荐功能");
        createPrivilege(9L, "NORMAL", "DETAILED_DATA", "详细录取数据", -1, "查看详细录取分数线数据");
    }

    /**
     * VIP会员权益
     * - AI对话：无限
     * - 一键生成志愿：无限
     */
    private void initVipPrivileges() {
        createPrivilege(10L, "VIP", "AI_CHAT", "AI智能对话", -1, "无限AI对话次数");
        createPrivilege(11L, "VIP", "UNIVERSITY_QUERY", "院校查询", -1, "不限次数查询院校信息");
        createPrivilege(12L, "VIP", "MAJOR_QUERY", "专业查询", -1, "不限次数查询专业信息");
        createPrivilege(13L, "VIP", "SMART_RECOMMEND", "智能推荐", -1, "不限次数使用智能推荐功能");
        createPrivilege(14L, "VIP", "ONE_CLICK_GENERATE", "一键生成志愿", -1, "一键生成完整志愿表");
        createPrivilege(15L, "VIP", "EXPERT_QA", "专家在线答疑", 5, "每月专家答疑次数限制为5次");
        createPrivilege(16L, "VIP", "PRIORITY_SUPPORT", "优先客服支持", -1, "享受优先客服响应");
    }

    private void createPrivilege(Long id, String level, String privilegeCode, String privilegeName, int limitCount, String description) {
        MemberPrivilege privilege = new MemberPrivilege();
        privilege.setId(id);
        privilege.setLevel(level);
        privilege.setPrivilegeCode(privilegeCode);
        privilege.setPrivilegeName(privilegeName);
        privilege.setLimitCount(limitCount);
        privilege.setDescription(description);
        privilege.setStatus(1);

        memberPrivilegeMapper.insert(privilege);
        log.debug("创建权益配置：{} -> {} = {}次/日", level, privilegeCode, limitCount == -1 ? "无限" : limitCount);
    }
}