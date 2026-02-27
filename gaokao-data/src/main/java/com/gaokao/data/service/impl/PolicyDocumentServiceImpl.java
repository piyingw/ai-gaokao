package com.gaokao.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaokao.data.dto.PolicyQueryDTO;
import com.gaokao.data.entity.PolicyDocument;
import com.gaokao.data.mapper.PolicyDocumentMapper;
import com.gaokao.data.service.PolicyDocumentService;
import com.gaokao.data.vo.PolicyDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 政策文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyDocumentServiceImpl implements PolicyDocumentService {

    private final PolicyDocumentMapper policyDocumentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Page<PolicyDocumentVO> pageList(PolicyQueryDTO dto) {
        Page<PolicyDocument> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dto.getKeyword()), PolicyDocument::getTitle, dto.getKeyword())
                .or()
                .like(StringUtils.hasText(dto.getKeyword()), PolicyDocument::getContent, dto.getKeyword())
                .eq(StringUtils.hasText(dto.getType()), PolicyDocument::getType, dto.getType())
                .eq(StringUtils.hasText(dto.getProvince()), PolicyDocument::getProvince, dto.getProvince())
                .eq(dto.getYear() != null, PolicyDocument::getYear, dto.getYear())
                .eq(PolicyDocument::getStatus, 1)
                .orderByDesc(PolicyDocument::getPublishTime);
        
        Page<PolicyDocument> result = policyDocumentMapper.selectPage(page, wrapper);
        
        Page<PolicyDocumentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public PolicyDocumentVO getDetail(Long id) {
        PolicyDocument document = policyDocumentMapper.selectById(id);
        if (document == null || document.getStatus() != 1) {
            return null;
        }
        return convertToVO(document);
    }

    @Override
    public List<PolicyDocumentVO> search(String keyword, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(PolicyDocument::getTitle, keyword)
                .or()
                .like(PolicyDocument::getContent, keyword)
                .or()
                .like(PolicyDocument::getKeywords, keyword)
                .eq(PolicyDocument::getStatus, 1)
                .orderByDesc(PolicyDocument::getPublishTime)
                .last("LIMIT " + limit);
        
        List<PolicyDocument> documents = policyDocumentMapper.selectList(wrapper);
        return documents.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PolicyDocumentVO> listByType(String type) {
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyDocument::getType, type)
                .eq(PolicyDocument::getStatus, 1)
                .orderByDesc(PolicyDocument::getPublishTime);
        
        List<PolicyDocument> documents = policyDocumentMapper.selectList(wrapper);
        return documents.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PolicyDocumentVO> listByProvince(String province) {
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyDocument::getProvince, province)
                .eq(PolicyDocument::getStatus, 1)
                .orderByDesc(PolicyDocument::getPublishTime);
        
        List<PolicyDocument> documents = policyDocumentMapper.selectList(wrapper);
        return documents.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PolicyDocumentVO> getHotPolicies(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyDocument::getStatus, 1)
                .orderByDesc(PolicyDocument::getPublishTime)
                .last("LIMIT " + limit);
        
        List<PolicyDocument> documents = policyDocumentMapper.selectList(wrapper);
        return documents.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listTypes() {
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(PolicyDocument::getType)
                .eq(PolicyDocument::getStatus, 1)
                .groupBy(PolicyDocument::getType);
        
        List<PolicyDocument> documents = policyDocumentMapper.selectList(wrapper);
        return documents.stream()
                .map(PolicyDocument::getType)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private PolicyDocumentVO convertToVO(PolicyDocument document) {
        PolicyDocumentVO vo = new PolicyDocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setType(document.getType());
        vo.setProvince(document.getProvince());
        vo.setYear(document.getYear());
        vo.setSummary(document.getSummary());
        vo.setSource(document.getSource());
        vo.setSourceUrl(document.getSourceUrl());
        vo.setPublishTime(document.getPublishTime());
        vo.setCreateTime(document.getCreateTime());
        
        // 解析关键词
        try {
            if (document.getKeywords() != null) {
                vo.setKeywords(objectMapper.readValue(document.getKeywords(), 
                        new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) {
            log.warn("解析关键词失败: {}", e.getMessage());
            vo.setKeywords(Collections.emptyList());
        }
        
        return vo;
    }
}