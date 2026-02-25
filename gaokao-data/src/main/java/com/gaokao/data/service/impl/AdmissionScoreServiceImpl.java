package com.gaokao.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.common.cache.CacheService;
import com.gaokao.common.constant.CacheConstants;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.data.dto.AdmissionScoreQueryDTO;
import com.gaokao.data.entity.AdmissionScore;
import com.gaokao.data.mapper.AdmissionScoreMapper;
import com.gaokao.data.service.AdmissionScoreService;
import com.gaokao.data.vo.AdmissionScoreVO;
import com.gaokao.data.vo.ScoreAnalysisVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 历年分数线服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdmissionScoreServiceImpl implements AdmissionScoreService {

    private final AdmissionScoreMapper admissionScoreMapper;
    private final CacheService cacheService;

    @Override
    public Page<AdmissionScoreVO> pageList(AdmissionScoreQueryDTO dto) {
        // 生成缓存 Key
        String cacheKey = buildListCacheKey(dto);

        // 尝试从缓存获取
        Page<AdmissionScoreVO> cachedPage = cacheService.get(cacheKey, Page.class);
        if (cachedPage != null) {
            log.debug("分数线列表缓存命中: {}", cacheKey);
            return cachedPage;
        }

        // 从数据库查询
        Page<AdmissionScoreVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        page = admissionScoreMapper.selectScorePage(
                page,
                dto.getUniversityId(),
                dto.getProvince(),
                dto.getSubjectType(),
                dto.getYear(),
                dto.getMinScoreFrom(),
                dto.getMinScoreTo()
        );

        // 设置缓存
        cacheService.set(cacheKey, page, CacheConstants.UNIVERSITY_SCORES_TTL);
        log.debug("分数线列表缓存设置: {}", cacheKey);

        return page;
    }

    @Override
    public List<AdmissionScoreVO> getUniversityScores(Long universityId, String province, String subjectType, List<Integer> years) {
        // 构建缓存 Key
        String cacheKey = CacheConstants.UNIVERSITY_SCORES + universityId + ":" +
                (province != null ? province : "all") + ":" +
                (subjectType != null ? subjectType : "all") + ":" +
                (years != null ? years.toString() : "all");

        // 使用分布式锁防止缓存击穿
        return cacheService.getOrLoadWithLock(cacheKey, List.class, () -> {
            List<AdmissionScoreVO> scores = admissionScoreMapper.selectScoreTrend(
                    universityId, province, subjectType, years
            );

            if (CollectionUtils.isEmpty(scores)) {
                return Collections.emptyList();
            }

            return scores;
        }, CacheConstants.UNIVERSITY_SCORES_TTL);
    }

    @Override
    public List<AdmissionScoreVO> queryByScore(String province, String subjectType, Integer score, Integer range, Integer year, Integer limit) {
        if (score == null || score < 0 || score > 750) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分数必须在 0-750 之间");
        }

        int actualRange = range != null ? range : 30;
        int actualYear = year != null ? year : 2023;
        int actualLimit = limit != null ? limit : 20;

        // 构建缓存 Key
        String cacheKey = CacheConstants.SAME_SCORE_ANALYSIS + province + ":" + subjectType + ":" +
                score + ":" + actualRange + ":" + actualYear;

        return cacheService.getOrLoadWithLock(cacheKey, List.class, () -> {
            int minScore = score - actualRange;
            int maxScore = score + actualRange;

            return admissionScoreMapper.selectByScoreRange(
                    province, subjectType, actualYear, minScore, maxScore, actualLimit
            );
        }, CacheConstants.SAME_SCORE_ANALYSIS_TTL);
    }

    @Override
    public ScoreAnalysisVO analyzeCompetitiveness(Long universityId, String province, String subjectType, Integer score) {
        if (universityId == null || score == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "院校ID和分数不能为空");
        }

        // 构建缓存 Key
        String cacheKey = CacheConstants.CACHE_PREFIX + "score:analysis:" + universityId + ":" +
                province + ":" + subjectType + ":" + score;

        return cacheService.getOrLoadWithLock(cacheKey, ScoreAnalysisVO.class, () -> {
            // 查询历年分数线
            List<AdmissionScoreVO> scores = getUniversityScores(universityId, province, subjectType, null);

            if (CollectionUtils.isEmpty(scores)) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "未找到该院校的历年分数线数据");
            }

            // 计算平均最低分
            double avgMinScore = scores.stream()
                    .filter(s -> s.getMinScore() != null)
                    .mapToInt(AdmissionScoreVO::getMinScore)
                    .average()
                    .orElse(0);

            // 计算标准差
            double stdDev = calculateStdDev(scores, avgMinScore);

            // 计算分差
            int scoreDiff = score - (int) Math.round(avgMinScore);

            // 计算录取概率
            double probability = calculateProbability(score, avgMinScore, stdDev);

            // 判断竞争力等级
            String level = determineCompetitivenessLevel(scoreDiff);

            // 构建分数范围描述
            String scoreRange = scores.stream()
                    .filter(s -> s.getMinScore() != null)
                    .mapToInt(AdmissionScoreVO::getMinScore)
                    .min()
                    .orElse(0) + " - " +
                    scores.stream()
                            .filter(s -> s.getMinScore() != null)
                            .mapToInt(AdmissionScoreVO::getMinScore)
                            .max()
                            .orElse(0);

            // 构建历年分数趋势
            List<ScoreAnalysisVO.YearScore> yearScores = scores.stream()
                    .sorted(Comparator.comparing(AdmissionScoreVO::getYear).reversed())
                    .limit(5)
                    .map(s -> ScoreAnalysisVO.YearScore.builder()
                            .year(s.getYear())
                            .minScore(s.getMinScore())
                            .avgScore(s.getAvgScore())
                            .maxScore(s.getMaxScore())
                            .minRank(s.getMinRank())
                            .enrollment(s.getEnrollment())
                            .build())
                    .collect(Collectors.toList());

            // 生成建议
            String suggestion = generateSuggestion(level, probability, scoreDiff);

            // 获取院校名称
            String universityName = scores.isEmpty() ? "" : scores.get(0).getUniversityName();

            return ScoreAnalysisVO.builder()
                    .universityId(universityId)
                    .universityName(universityName)
                    .province(province)
                    .subjectType(subjectType)
                    .userScore(score)
                    .avgMinScore(BigDecimal.valueOf(avgMinScore).setScale(2, RoundingMode.HALF_UP))
                    .scoreRange(scoreRange)
                    .scoreDiff(scoreDiff)
                    .competitivenessLevel(level)
                    .probability(BigDecimal.valueOf(probability).setScale(2, RoundingMode.HALF_UP))
                    .yearScores(yearScores)
                    .suggestion(suggestion)
                    .build();
        }, CacheConstants.UNIVERSITY_SCORES_TTL);
    }

    @Override
    public List<AdmissionScoreVO> getSameScoreUniversities(String province, String subjectType, Integer score, Integer year, Integer range, String level, Integer limit) {
        if (score == null || score < 0 || score > 750) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分数必须在 0-750 之间");
        }

        int actualRange = range != null ? range : 20;
        int actualYear = year != null ? year : 2023;
        int actualLimit = limit != null ? limit : 30;

        // 构建缓存 Key
        String cacheKey = CacheConstants.SAME_SCORE_ANALYSIS + "universities:" + province + ":" +
                subjectType + ":" + score + ":" + actualYear + ":" + actualRange + ":" + level;

        return cacheService.getOrLoadWithLock(cacheKey, List.class, () ->
                admissionScoreMapper.selectSameScoreUniversities(
                        province, subjectType, actualYear, score, actualRange, level, actualLimit
                ), CacheConstants.SAME_SCORE_ANALYSIS_TTL);
    }

    @Override
    public Map<String, Object> getScoreStatistics(Long universityId, String province, String subjectType) {
        if (universityId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "院校ID不能为空");
        }

        // 构建缓存 Key
        String cacheKey = CacheConstants.CACHE_PREFIX + "score:statistics:" + universityId + ":" +
                (province != null ? province : "all") + ":" +
                (subjectType != null ? subjectType : "all");

        return cacheService.getOrLoadWithLock(cacheKey, Map.class, () ->
                admissionScoreMapper.selectScoreStatistics(universityId, province, subjectType),
                CacheConstants.UNIVERSITY_SCORES_TTL);
    }

    @Override
    public List<Map<String, Object>> getProvinceScoreDistribution(String province, Integer year) {
        if (!StringUtils.hasText(province)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "省份不能为空");
        }

        int actualYear = year != null ? year : 2023;

        // 构建缓存 Key
        String cacheKey = CacheConstants.CACHE_PREFIX + "score:distribution:" + province + ":" + actualYear;

        return cacheService.getOrLoadWithLock(cacheKey, List.class, () ->
                admissionScoreMapper.selectProvinceScoreDistribution(province, actualYear),
                CacheConstants.UNIVERSITY_SCORES_TTL);
    }

    @Override
    public double calculateProbability(Integer score, double avgMinScore, double stdDev) {
        if (score == null || avgMinScore <= 0) {
            return 0;
        }

        double sigma = stdDev > 0 ? stdDev : 10;
        int diff = score - (int) Math.round(avgMinScore);

        // 基于正态分布的概率估算
        if (diff >= 2 * sigma) {
            return 0.95;
        } else if (diff >= sigma) {
            return 0.80;
        } else if (diff >= 0) {
            return 0.60;
        } else if (diff >= -sigma) {
            return 0.40;
        } else if (diff >= -2 * sigma) {
            return 0.20;
        } else {
            return 0.05;
        }
    }

    /**
     * 计算标准差
     */
    private double calculateStdDev(List<AdmissionScoreVO> scores, double avg) {
        List<Integer> scoreList = scores.stream()
                .filter(s -> s.getMinScore() != null)
                .map(AdmissionScoreVO::getMinScore)
                .collect(Collectors.toList());

        if (scoreList.size() < 2) {
            return 10; // 默认标准差
        }

        double variance = scoreList.stream()
                .mapToDouble(s -> Math.pow(s - avg, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance);
    }

    /**
     * 判断竞争力等级
     */
    private String determineCompetitivenessLevel(int scoreDiff) {
        if (scoreDiff >= 20) {
            return "保底";
        } else if (scoreDiff >= 0) {
            return "稳妥";
        } else if (scoreDiff >= -20) {
            return "冲刺";
        } else {
            return "风险较大";
        }
    }

    /**
     * 生成建议
     */
    private String generateSuggestion(String level, double probability, int scoreDiff) {
        StringBuilder sb = new StringBuilder();

        switch (level) {
            case "保底":
                sb.append("您的分数明显高于该校历年分数线，录取概率很高。");
                sb.append("建议作为保底院校，确保有学可上。");
                break;
            case "稳妥":
                sb.append("您的分数略高于该校历年分数线，录取概率较大。");
                sb.append("可作为稳妥选择，放在志愿表中间位置。");
                break;
            case "冲刺":
                sb.append("您的分数略低于该校历年分数线，有一定录取机会。");
                sb.append("可以冲刺尝试，建议放在志愿表前部。");
                break;
            case "风险较大":
                sb.append("您的分数明显低于该校历年分数线，录取难度较大。");
                sb.append("建议谨慎填报，或考虑其他院校。");
                break;
        }

        sb.append(String.format("预估录取概率约 %.0f%%。", probability * 100));

        return sb.toString();
    }

    /**
     * 构建列表缓存 Key
     */
    private String buildListCacheKey(AdmissionScoreQueryDTO dto) {
        StringBuilder sb = new StringBuilder(CacheConstants.CACHE_PREFIX + "score:list:");
        sb.append(dto.getPageNum()).append(":");
        sb.append(dto.getPageSize()).append(":");

        if (dto.getUniversityId() != null) {
            sb.append("uid:").append(dto.getUniversityId()).append(":");
        }
        if (StringUtils.hasText(dto.getProvince())) {
            sb.append("prov:").append(dto.getProvince()).append(":");
        }
        if (StringUtils.hasText(dto.getSubjectType())) {
            sb.append("subj:").append(dto.getSubjectType()).append(":");
        }
        if (dto.getYear() != null) {
            sb.append("year:").append(dto.getYear()).append(":");
        }
        if (dto.getMinScoreFrom() != null) {
            sb.append("min:").append(dto.getMinScoreFrom()).append(":");
        }
        if (dto.getMinScoreTo() != null) {
            sb.append("max:").append(dto.getMinScoreTo()).append(":");
        }

        // 对 Key 进行 MD5 简化
        String keyStr = sb.toString();
        String md5 = DigestUtils.md5DigestAsHex(keyStr.getBytes(StandardCharsets.UTF_8));

        return CacheConstants.CACHE_PREFIX + "score:list:" + md5;
    }
}