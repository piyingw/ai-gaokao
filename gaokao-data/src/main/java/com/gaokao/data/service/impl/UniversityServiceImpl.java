package com.gaokao.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.common.cache.CacheService;
import com.gaokao.common.constant.CacheConstants;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.data.dto.UniversityQueryDTO;
import com.gaokao.data.entity.AdmissionScore;
import com.gaokao.data.entity.University;
import com.gaokao.data.mapper.AdmissionScoreMapper;
import com.gaokao.data.mapper.UniversityMapper;
import com.gaokao.data.service.MajorService;
import com.gaokao.data.service.UniversityService;
import com.gaokao.data.vo.MajorVO;
import com.gaokao.data.vo.UniversityDetailVO;
import com.gaokao.data.vo.UniversityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 院校服务实现类
 * 
 * 缓存策略：
 * 1. 院校详情：热点数据，使用分布式锁防止缓存击穿
 * 2. 院校列表：分页查询，根据查询条件生成缓存 Key
 * 3. 分数线数据：高频查询，使用缓存减少数据库压力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniversityServiceImpl implements UniversityService {

    private final UniversityMapper universityMapper;
    private final AdmissionScoreMapper admissionScoreMapper;
    private final CacheService cacheService;
    private final MajorService majorService;

    @Override
    public Page<UniversityVO> pageList(UniversityQueryDTO dto) {
        // 生成缓存 Key（基于查询条件的 MD5）
        String cacheKey = buildListCacheKey(dto);

        // 尝试从缓存获取（列表查询不使用分布式锁，因为查询条件多变）
        Page<UniversityVO> cachedPage = cacheService.get(cacheKey, Page.class);
        if (cachedPage != null) {
            log.debug("院校列表缓存命中: {}", cacheKey);
            return cachedPage;
        }

        // 从数据库查询
        Page<University> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<University> wrapper = new LambdaQueryWrapper<>();

        // 条件查询
        if (StringUtils.hasText(dto.getName())) {
            wrapper.like(University::getName, dto.getName());
        }
        if (StringUtils.hasText(dto.getProvince())) {
            wrapper.eq(University::getProvince, dto.getProvince());
        }
        if (StringUtils.hasText(dto.getCity())) {
            wrapper.eq(University::getCity, dto.getCity());
        }
        if (StringUtils.hasText(dto.getLevel())) {
            wrapper.eq(University::getLevel, dto.getLevel());
        }
        if (StringUtils.hasText(dto.getType())) {
            wrapper.eq(University::getType, dto.getType());
        }
        if (StringUtils.hasText(dto.getNature())) {
            wrapper.eq(University::getNature, dto.getNature());
        }

        // 排序
        if (StringUtils.hasText(dto.getSortField())) {
            boolean isAsc = "asc".equalsIgnoreCase(dto.getSortOrder());
            wrapper.orderBy(true, isAsc, University::getRanking);
        } else {
            wrapper.orderByAsc(University::getRanking);
        }

        Page<University> result = universityMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<UniversityVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .toList());

        // 设置缓存（列表缓存时间较短）
        cacheService.set(cacheKey, voPage, CacheConstants.UNIVERSITY_LIST_TTL);
        log.debug("院校列表缓存设置: {}", cacheKey);

        return voPage;
    }

    @Override
    public UniversityDetailVO getDetail(Long id) {
        // 使用分布式锁防止缓存击穿（院校详情是热点数据）
        String cacheKey = CacheConstants.UNIVERSITY_DETAIL + id;

        return cacheService.getOrLoadWithLock(cacheKey, UniversityDetailVO.class, () -> {
            University university = universityMapper.selectById(id);
            if (university == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "院校不存在");
            }
            return convertToDetailVO(university);
        }, CacheConstants.UNIVERSITY_DETAIL_TTL);
    }

    @Override
    public Object getScores(Long id, String province, String subjectType) {
        // 构建缓存 Key
        String cacheKey = CacheConstants.UNIVERSITY_SCORES + id + ":" + 
                (province != null ? province : "all") + ":" + 
                (subjectType != null ? subjectType : "all");

        // 使用分布式锁（分数线查询是高频操作）
        return cacheService.getOrLoadWithLock(cacheKey, List.class, () -> {
            LambdaQueryWrapper<AdmissionScore> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AdmissionScore::getUniversityId, id);

            if (StringUtils.hasText(province)) {
                wrapper.eq(AdmissionScore::getProvince, province);
            }
            if (StringUtils.hasText(subjectType)) {
                wrapper.eq(AdmissionScore::getSubjectType, subjectType);
            }

            wrapper.orderByDesc(AdmissionScore::getYear);

            return admissionScoreMapper.selectList(wrapper);
        }, CacheConstants.UNIVERSITY_SCORES_TTL);
    }

    @Override
    public Object getMajors(Long id) {
        // 验证院校是否存在
        University university = universityMapper.selectById(id);
        if (university == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "院校不存在");
        }
        
        // 查询该校开设的专业
        List<MajorVO> majors = majorService.listByUniversityId(id);
        return majors;
    }

    @Override
    public Object compare(Long[] ids) {
        if (ids == null || ids.length < 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少选择两所院校进行对比");
        }

        // 院校对比不使用缓存（操作频率较低，且结果多变）
        List<University> universities = universityMapper.selectBatchIds(Arrays.asList(ids));
        return universities.stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 构建列表缓存 Key
     */
    private String buildListCacheKey(UniversityQueryDTO dto) {
        StringBuilder sb = new StringBuilder(CacheConstants.UNIVERSITY_LIST);
        sb.append(dto.getPageNum()).append(":");
        sb.append(dto.getPageSize()).append(":");
        
        if (StringUtils.hasText(dto.getName())) {
            sb.append("name:").append(dto.getName()).append(":");
        }
        if (StringUtils.hasText(dto.getProvince())) {
            sb.append("province:").append(dto.getProvince()).append(":");
        }
        if (StringUtils.hasText(dto.getCity())) {
            sb.append("city:").append(dto.getCity()).append(":");
        }
        if (StringUtils.hasText(dto.getLevel())) {
            sb.append("level:").append(dto.getLevel()).append(":");
        }
        if (StringUtils.hasText(dto.getType())) {
            sb.append("type:").append(dto.getType()).append(":");
        }
        if (StringUtils.hasText(dto.getNature())) {
            sb.append("nature:").append(dto.getNature()).append(":");
        }
        
        // 对 Key 进行 MD5 简化
        String keyStr = sb.toString();
        String md5 = DigestUtils.md5DigestAsHex(keyStr.getBytes(StandardCharsets.UTF_8));
        
        return CacheConstants.UNIVERSITY_LIST + md5;
    }

    private UniversityVO convertToVO(University university) {
        UniversityVO vo = new UniversityVO();
        vo.setId(university.getId());
        vo.setName(university.getName());
        vo.setCode(university.getCode());
        vo.setProvince(university.getProvince());
        vo.setCity(university.getCity());
        vo.setLevel(university.getLevel());
        vo.setType(university.getType());
        vo.setNature(university.getNature());
        vo.setRanking(university.getRanking());
        vo.setTags(university.getTags());
        return vo;
    }

    private UniversityDetailVO convertToDetailVO(University university) {
        UniversityDetailVO vo = new UniversityDetailVO();
        vo.setId(university.getId());
        vo.setName(university.getName());
        vo.setCode(university.getCode());
        vo.setProvince(university.getProvince());
        vo.setCity(university.getCity());
        vo.setLevel(university.getLevel());
        vo.setType(university.getType());
        vo.setNature(university.getNature());
        vo.setRanking(university.getRanking());
        vo.setIntro(university.getIntro());
        vo.setFeatures(university.getFeatures());
        vo.setAdmissionUrl(university.getAdmissionUrl());
        vo.setOfficialUrl(university.getOfficialUrl());
        vo.setTags(university.getTags());
        return vo;
    }
}