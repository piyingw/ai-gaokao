package com.gaokao.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.data.dto.MajorQueryDTO;
import com.gaokao.data.entity.Major;
import com.gaokao.data.mapper.MajorMapper;
import com.gaokao.data.service.MajorService;
import com.gaokao.data.vo.MajorDetailVO;
import com.gaokao.data.vo.MajorVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专业服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {

    private final MajorMapper majorMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Page<MajorVO> pageList(MajorQueryDTO dto) {
        Page<Major> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(dto.getName() != null, Major::getName, dto.getName())
                .eq(dto.getCode() != null, Major::getCode, dto.getCode())
                .eq(dto.getCategory() != null, Major::getCategory, dto.getCategory())
                .eq(dto.getSubCategory() != null, Major::getSubCategory, dto.getSubCategory())
                .eq(dto.getDegreeType() != null, Major::getDegreeType, dto.getDegreeType())
                .orderByDesc(Major::getEmploymentRating)
                .orderByAsc(Major::getCategory, Major::getSubCategory, Major::getName);
        
        Page<Major> majorPage = majorMapper.selectPage(page, wrapper);
        
        // 转换为VO
        Page<MajorVO> voPage = new Page<>(majorPage.getCurrent(), majorPage.getSize(), majorPage.getTotal());
        voPage.setRecords(majorPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public MajorDetailVO getDetail(Long id) {
        Major major = majorMapper.selectById(id);
        if (major == null) {
            return null;
        }
        return convertToDetailVO(major);
    }

    @Override
    public List<MajorVO> listByCategory(String category) {
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Major::getCategory, category)
                .orderByAsc(Major::getSubCategory, Major::getName);
        
        List<Major> majors = majorMapper.selectList(wrapper);
        return majors.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MajorVO> listByUniversityId(Long universityId) {
        // 查询该校开设的专业
        // 实际应该关联 university_major 表，这里简化处理
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Major::getEmploymentRating)
                .last("LIMIT 50");
        
        List<Major> majors = majorMapper.selectList(wrapper);
        return majors.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MajorVO> search(String keyword, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Major::getName, keyword)
                .or()
                .like(Major::getCode, keyword)
                .orderByDesc(Major::getEmploymentRating)
                .last("LIMIT " + limit);
        
        List<Major> majors = majorMapper.selectList(wrapper);
        return majors.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MajorVO> recommendByPersonality(String personalityType) {
        // 根据性格类型推荐专业
        Map<String, List<String>> personalityMajorMap = getPersonalityMajorMap();
        
        List<String> recommendedCategories = personalityMajorMap.getOrDefault(
                personalityType.toUpperCase(), 
                Collections.emptyList()
        );
        
        if (recommendedCategories.isEmpty()) {
            // 默认推荐热门专业
            LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Major::getEmploymentRating, 4)
                    .orderByDesc(Major::getAvgSalary)
                    .last("LIMIT 20");
            
            List<Major> majors = majorMapper.selectList(wrapper);
            return majors.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
        }
        
        // 根据推荐的学科门类查询专业
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Major::getCategory, recommendedCategories)
                .ge(Major::getEmploymentRating, 3)
                .orderByDesc(Major::getEmploymentRating, Major::getAvgSalary)
                .last("LIMIT 20");
        
        List<Major> majors = majorMapper.selectList(wrapper);
        return majors.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listCategories() {
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Major::getCategory)
                .groupBy(Major::getCategory)
                .orderByAsc(Major::getCategory);
        
        List<Major> majors = majorMapper.selectList(wrapper);
        return majors.stream()
                .map(Major::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listSubCategories(String category) {
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Major::getSubCategory)
                .eq(Major::getCategory, category)
                .groupBy(Major::getSubCategory)
                .orderByAsc(Major::getSubCategory);
        
        List<Major> majors = majorMapper.selectList(wrapper);
        return majors.stream()
                .map(Major::getSubCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private MajorVO convertToVO(Major major) {
        MajorVO vo = new MajorVO();
        vo.setId(major.getId());
        vo.setName(major.getName());
        vo.setCode(major.getCode());
        vo.setCategory(major.getCategory());
        vo.setSubCategory(major.getSubCategory());
        vo.setDegreeType(major.getDegreeType());
        vo.setDuration(major.getDuration());
        vo.setEmploymentRating(major.getEmploymentRating());
        vo.setAvgSalary(major.getAvgSalary());
        return vo;
    }

    /**
     * 转换为详情VO
     */
    private MajorDetailVO convertToDetailVO(Major major) {
        MajorDetailVO vo = new MajorDetailVO();
        vo.setId(major.getId());
        vo.setName(major.getName());
        vo.setCode(major.getCode());
        vo.setCategory(major.getCategory());
        vo.setSubCategory(major.getSubCategory());
        vo.setDegreeType(major.getDegreeType());
        vo.setDuration(major.getDuration());
        vo.setIntro(major.getIntro());
        vo.setEmploymentRating(major.getEmploymentRating());
        vo.setAvgSalary(major.getAvgSalary());
        vo.setGenderRatio(major.getGenderRatio());
        
        // 解析JSON字段
        try {
            if (major.getCourses() != null) {
                vo.setCourses(objectMapper.readValue(major.getCourses(), new TypeReference<List<String>>() {}));
            }
            if (major.getEmployment() != null) {
                vo.setEmployment(objectMapper.readValue(major.getEmployment(), new TypeReference<List<String>>() {}));
            }
            if (major.getSubjectRequirement() != null) {
                vo.setSubjectRequirement(objectMapper.readValue(major.getSubjectRequirement(), Object.class));
            }
        } catch (Exception e) {
            log.warn("解析专业JSON字段失败: {}", e.getMessage());
        }
        
        return vo;
    }

    /**
     * 性格类型与专业门类映射
     */
    private Map<String, List<String>> getPersonalityMajorMap() {
        Map<String, List<String>> map = new HashMap<>();
        
        // MBTI 类型映射
        map.put("INTJ", Arrays.asList("工学", "理学", "经济学"));
        map.put("INTP", Arrays.asList("理学", "工学", "哲学"));
        map.put("ENTJ", Arrays.asList("管理学", "经济学", "法学"));
        map.put("ENTP", Arrays.asList("经济学", "管理学", "文学"));
        map.put("INFJ", Arrays.asList("教育学", "文学", "心理学"));
        map.put("INFP", Arrays.asList("文学", "艺术学", "教育学"));
        map.put("ENFJ", Arrays.asList("教育学", "管理学", "文学"));
        map.put("ENFP", Arrays.asList("文学", "艺术学", "传播学"));
        map.put("ISTJ", Arrays.asList("工学", "管理学", "会计学"));
        map.put("ISFJ", Arrays.asList("医学", "教育学", "管理学"));
        map.put("ESTJ", Arrays.asList("管理学", "法学", "经济学"));
        map.put("ESFJ", Arrays.asList("教育学", "医学", "社会学"));
        map.put("ISTP", Arrays.asList("工学", "理学", "农学"));
        map.put("ISFP", Arrays.asList("艺术学", "文学", "设计学"));
        map.put("ESTP", Arrays.asList("经济学", "管理学", "体育学"));
        map.put("ESFP", Arrays.asList("艺术学", "传播学", "旅游管理"));
        
        // 霍兰德代码映射
        map.put("R", Arrays.asList("工学", "农学", "理学"));  // 现实型
        map.put("I", Arrays.asList("理学", "工学", "医学"));  // 研究型
        map.put("A", Arrays.asList("艺术学", "文学", "设计学"));  // 艺术型
        map.put("S", Arrays.asList("教育学", "医学", "社会学"));  // 社会型
        map.put("E", Arrays.asList("管理学", "经济学", "法学"));  // 企业型
        map.put("C", Arrays.asList("管理学", "会计学", "统计学"));  // 常规型
        
        return map;
    }
}