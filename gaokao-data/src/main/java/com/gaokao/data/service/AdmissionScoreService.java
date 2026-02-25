package com.gaokao.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.data.dto.AdmissionScoreQueryDTO;
import com.gaokao.data.vo.AdmissionScoreVO;
import com.gaokao.data.vo.ScoreAnalysisVO;

import java.util.List;
import java.util.Map;

/**
 * 历年分数线服务接口
 */
public interface AdmissionScoreService {

    /**
     * 分页查询分数线
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    Page<AdmissionScoreVO> pageList(AdmissionScoreQueryDTO dto);

    /**
     * 查询院校历年分数线
     *
     * @param universityId 院校 ID
     * @param province     省份
     * @param subjectType  科类
     * @param years        年份列表
     * @return 分数线列表
     */
    List<AdmissionScoreVO> getUniversityScores(Long universityId, String province, String subjectType, List<Integer> years);

    /**
     * 根据分数查询可报考院校
     *
     * @param province    省份
     * @param subjectType 科类
     * @param score       分数
     * @param range       分数浮动范围
     * @param year        年份
     * @param limit       限制数量
     * @return 分数线列表
     */
    List<AdmissionScoreVO> queryByScore(String province, String subjectType, Integer score, Integer range, Integer year, Integer limit);

    /**
     * 分析分数竞争力
     *
     * @param universityId 院校 ID
     * @param province     省份
     * @param subjectType  科类
     * @param score        用户分数
     * @return 分析结果
     */
    ScoreAnalysisVO analyzeCompetitiveness(Long universityId, String province, String subjectType, Integer score);

    /**
     * 查询同分段可报考院校
     *
     * @param province    省份
     * @param subjectType 科类
     * @param score       分数
     * @param year        年份
     * @param range       分数浮动范围
     * @param level       院校层次（可选）
     * @param limit       限制数量
     * @return 分数线列表
     */
    List<AdmissionScoreVO> getSameScoreUniversities(String province, String subjectType, Integer score, Integer year, Integer range, String level, Integer limit);

    /**
     * 获取院校分数线统计信息
     *
     * @param universityId 院校 ID
     * @param province     省份
     * @param subjectType  科类
     * @return 统计信息
     */
    Map<String, Object> getScoreStatistics(Long universityId, String province, String subjectType);

    /**
     * 获取省份分数线分布
     *
     * @param province 省份
     * @param year     年份
     * @return 分布信息
     */
    List<Map<String, Object>> getProvinceScoreDistribution(String province, Integer year);

    /**
     * 计算录取概率
     *
     * @param score      用户分数
     * @param avgMinScore 历年平均最低分
     * @param stdDev     标准差
     * @return 录取概率（0-1）
     */
    double calculateProbability(Integer score, double avgMinScore, double stdDev);
}