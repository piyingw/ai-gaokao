package com.gaokao.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.data.dto.UserApplicationDTO;
import com.gaokao.data.entity.UserApplication;
import com.gaokao.data.mapper.UserApplicationMapper;
import com.gaokao.data.service.UserApplicationService;
import com.gaokao.data.vo.UserApplicationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户志愿服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationServiceImpl implements UserApplicationService {

    private final UserApplicationMapper userApplicationMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Page<UserApplicationVO> pageList(Long userId, Integer pageNum, Integer pageSize) {
        Page<UserApplication> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<UserApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserApplication::getUserId, userId)
                .orderByDesc(UserApplication::getUpdateTime);
        
        Page<UserApplication> result = userApplicationMapper.selectPage(page, wrapper);
        
        Page<UserApplicationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public UserApplicationVO getDetail(Long id, Long userId) {
        UserApplication application = userApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "志愿不存在");
        }
        if (!application.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该志愿");
        }
        return convertToVO(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, UserApplicationDTO dto) {
        UserApplication application = new UserApplication();
        application.setUserId(userId);
        application.setName(dto.getName());
        application.setScore(dto.getScore());
        application.setProvince(dto.getProvince());
        application.setSubjectType(dto.getSubjectType());
        application.setRemark(dto.getRemark());
        application.setStatus(0); // 草稿状态
        
        // 序列化志愿列表
        try {
            if (dto.getApplications() != null) {
                application.setApplications(objectMapper.writeValueAsString(dto.getApplications()));
            }
        } catch (Exception e) {
            log.error("序列化志愿列表失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "保存志愿失败");
        }
        
        userApplicationMapper.insert(application);
        return application.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long userId, UserApplicationDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "志愿ID不能为空");
        }
        
        UserApplication application = userApplicationMapper.selectById(dto.getId());
        if (application == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "志愿不存在");
        }
        if (!application.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权修改该志愿");
        }
        
        // 更新字段
        if (dto.getName() != null) {
            application.setName(dto.getName());
        }
        if (dto.getScore() != null) {
            application.setScore(dto.getScore());
        }
        if (dto.getProvince() != null) {
            application.setProvince(dto.getProvince());
        }
        if (dto.getSubjectType() != null) {
            application.setSubjectType(dto.getSubjectType());
        }
        if (dto.getRemark() != null) {
            application.setRemark(dto.getRemark());
        }
        
        // 更新志愿列表
        try {
            if (dto.getApplications() != null) {
                application.setApplications(objectMapper.writeValueAsString(dto.getApplications()));
            }
        } catch (Exception e) {
            log.error("序列化志愿列表失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "保存志愿失败");
        }
        
        userApplicationMapper.updateById(application);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id, Long userId) {
        UserApplication application = userApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "志愿不存在");
        }
        if (!application.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该志愿");
        }
        
        userApplicationMapper.deleteById(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submit(Long id, Long userId) {
        UserApplication application = userApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "志愿不存在");
        }
        if (!application.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该志愿");
        }
        
        application.setStatus(1); // 已提交状态
        userApplicationMapper.updateById(application);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id, Long userId) {
        UserApplication original = userApplicationMapper.selectById(id);
        if (original == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "志愿不存在");
        }
        
        // 创建副本
        UserApplication copy = new UserApplication();
        copy.setUserId(userId);
        copy.setName(original.getName() + " (副本)");
        copy.setScore(original.getScore());
        copy.setProvince(original.getProvince());
        copy.setSubjectType(original.getSubjectType());
        copy.setApplications(original.getApplications());
        copy.setStatus(0); // 草稿状态
        copy.setRemark(original.getRemark());
        
        userApplicationMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public UserApplicationVO getLatest(Long userId) {
        LambdaQueryWrapper<UserApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserApplication::getUserId, userId)
                .orderByDesc(UserApplication::getUpdateTime)
                .last("LIMIT 1");
        
        UserApplication application = userApplicationMapper.selectOne(wrapper);
        return application != null ? convertToVO(application) : null;
    }

    @Override
    public Object analyze(Long id, Long userId) {
        UserApplicationVO vo = getDetail(id, userId);
        if (vo == null || vo.getApplications() == null || vo.getApplications().isEmpty()) {
            return null;
        }
        
        // 分析志愿方案
        Map<String, Object> analysis = new HashMap<>();
        
        // 统计冲稳保数量
        int chongCount = 0, wenCount = 0, baoCount = 0;
        double totalProbability = 0;
        
        for (UserApplicationVO.ApplicationItem item : vo.getApplications()) {
            if (item.getType() != null) {
                switch (item.getType()) {
                    case "冲": chongCount++; break;
                    case "稳": wenCount++; break;
                    case "保": baoCount++; break;
                }
            }
            if (item.getProbability() != null) {
                totalProbability += item.getProbability();
            }
        }
        
        analysis.put("totalCount", vo.getApplications().size());
        analysis.put("chongCount", chongCount);
        analysis.put("wenCount", wenCount);
        analysis.put("baoCount", baoCount);
        analysis.put("avgProbability", vo.getApplications().isEmpty() ? 0 : 
                totalProbability / vo.getApplications().size());
        
        // 评估建议
        List<String> suggestions = new ArrayList<>();
        if (chongCount > vo.getApplications().size() * 0.4) {
            suggestions.add("冲刺院校比例较高，建议增加保底院校");
        }
        if (baoCount < 3) {
            suggestions.add("保底院校数量不足，建议至少保留3所保底院校");
        }
        if (wenCount < vo.getApplications().size() * 0.3) {
            suggestions.add("稳妥院校比例较低，可能影响录取稳定性");
        }
        
        analysis.put("suggestions", suggestions);
        
        return analysis;
    }

    /**
     * 转换为VO
     */
    private UserApplicationVO convertToVO(UserApplication application) {
        UserApplicationVO vo = new UserApplicationVO();
        vo.setId(application.getId());
        vo.setName(application.getName());
        vo.setScore(application.getScore());
        vo.setProvince(application.getProvince());
        vo.setSubjectType(application.getSubjectType());
        vo.setStatus(application.getStatus());
        vo.setRemark(application.getRemark());
        vo.setCreateTime(application.getCreateTime());
        vo.setUpdateTime(application.getUpdateTime());
        
        // 解析志愿列表
        try {
            if (application.getApplications() != null) {
                vo.setApplications(objectMapper.readValue(application.getApplications(), 
                        new TypeReference<List<UserApplicationVO.ApplicationItem>>() {}));
            }
        } catch (Exception e) {
            log.warn("解析志愿列表失败: {}", e.getMessage());
        }
        
        return vo;
    }
}