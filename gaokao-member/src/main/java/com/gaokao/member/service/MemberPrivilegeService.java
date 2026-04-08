package com.gaokao.member.service;

import com.gaokao.member.entity.MemberPrivilege;

import java.util.List;

/**
 * 会员权益配置服务接口
 */
public interface MemberPrivilegeService {

    /**
     * 获取指定等级的权益配置
     *
     * @param level 会员等级
     * @param privilegeCode 权益代码
     * @return 权益配置
     */
    MemberPrivilege getPrivilege(String level, String privilegeCode);

    /**
     * 获取指定等级的所有权益配置
     *
     * @param level 会员等级
     * @return 权益配置列表
     */
    List<MemberPrivilege> getPrivilegesByLevel(String level);

    /**
     * 获取所有权益配置
     *
     * @return 权益配置列表
     */
    List<MemberPrivilege> getAllPrivileges();

    /**
     * 刷新权益配置缓存
     */
    void refreshCache();
}