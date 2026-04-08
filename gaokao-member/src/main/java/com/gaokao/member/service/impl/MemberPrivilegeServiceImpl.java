package com.gaokao.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.member.entity.MemberPrivilege;
import com.gaokao.member.mapper.MemberPrivilegeMapper;
import com.gaokao.member.service.MemberPrivilegeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 会员权益配置服务实现类
 *
 * 缓存策略：
 * - 权益配置缓存Key：gaokao:member:privilege:{level}:{privilegeCode}
 * - 权益配置列表缓存Key：gaokao:member:privileges:{level}
 * - 缓存过期时间：1小时（配置变更时主动刷新）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPrivilegeServiceImpl implements MemberPrivilegeService {

    private final MemberPrivilegeMapper memberPrivilegeMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 权益缓存Key前缀
     */
    private static final String PRIVILEGE_CACHE_KEY = "gaokao:member:privilege:";
    private static final String PRIVILEGES_LIST_CACHE_KEY = "gaokao:member:privileges:";

    /**
     * 缓存过期时间（1小时）
     */
    private static final long CACHE_EXPIRE = 60 * 60;

    @Override
    public MemberPrivilege getPrivilege(String level, String privilegeCode) {
        // 1. 先从缓存获取
        String cacheKey = PRIVILEGE_CACHE_KEY + level + ":" + privilegeCode;
        MemberPrivilege privilege = (MemberPrivilege) redisTemplate.opsForValue().get(cacheKey);

        if (privilege != null) {
            return privilege;
        }

        // 2. 从数据库查询
        privilege = memberPrivilegeMapper.selectOne(
                new LambdaQueryWrapper<MemberPrivilege>()
                        .eq(MemberPrivilege::getLevel, level)
                        .eq(MemberPrivilege::getPrivilegeCode, privilegeCode)
                        .eq(MemberPrivilege::getStatus, 1)
        );

        if (privilege != null) {
            // 3. 写入缓存
            redisTemplate.opsForValue().set(cacheKey, privilege, CACHE_EXPIRE, TimeUnit.SECONDS);
        }

        return privilege;
    }

    @Override
    public List<MemberPrivilege> getPrivilegesByLevel(String level) {
        // 1. 先从缓存获取
        String cacheKey = PRIVILEGES_LIST_CACHE_KEY + level;
        List<MemberPrivilege> privileges = (List<MemberPrivilege>) redisTemplate.opsForValue().get(cacheKey);

        if (privileges != null) {
            return privileges;
        }

        // 2. 从数据库查询
        privileges = memberPrivilegeMapper.selectList(
                new LambdaQueryWrapper<MemberPrivilege>()
                        .eq(MemberPrivilege::getLevel, level)
                        .eq(MemberPrivilege::getStatus, 1)
        );

        // 3. 写入缓存
        redisTemplate.opsForValue().set(cacheKey, privileges, CACHE_EXPIRE, TimeUnit.SECONDS);

        return privileges;
    }

    @Override
    public List<MemberPrivilege> getAllPrivileges() {
        return memberPrivilegeMapper.selectList(
                new LambdaQueryWrapper<MemberPrivilege>()
                        .eq(MemberPrivilege::getStatus, 1)
        );
    }

    @Override
    public void refreshCache() {
        log.info("刷新权益配置缓存...");

        // 清除所有权益缓存
        redisTemplate.delete(PRIVILEGE_CACHE_KEY + "*");
        redisTemplate.delete(PRIVILEGES_LIST_CACHE_KEY + "*");

        // 预热缓存
        List<MemberPrivilege> allPrivileges = getAllPrivileges();
        for (MemberPrivilege privilege : allPrivileges) {
            String cacheKey = PRIVILEGE_CACHE_KEY + privilege.getLevel() + ":" + privilege.getPrivilegeCode();
            redisTemplate.opsForValue().set(cacheKey, privilege, CACHE_EXPIRE, TimeUnit.SECONDS);
        }

        log.info("权益配置缓存刷新完成，共{}条", allPrivileges.size());
    }
}