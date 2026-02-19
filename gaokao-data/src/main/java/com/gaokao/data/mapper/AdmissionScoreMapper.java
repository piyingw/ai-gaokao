package com.gaokao.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.data.entity.AdmissionScore;
import com.gaokao.data.vo.AdmissionScoreVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 分数线 Mapper 接口
 */
@Mapper
public interface AdmissionScoreMapper extends BaseMapper<AdmissionScore> {

    /**
     * 分页查询分数线（关联院校信息）
     *
     * @param page         分页参数
     * @param universityId 院校 ID
     * @param province     省份
     * @param subjectType  科类
     * @param year         年份
     * @param minScoreFrom 最低分下限
     * @param minScoreTo   最低分上限
     * @return 分页结果
     */
    Page<AdmissionScoreVO> selectScorePage(
            Page<AdmissionScoreVO> page,
            @Param("universityId") Long universityId,
            @Param("province") String province,
            @Param("subjectType") String subjectType,
            @Param("year") Integer year,
            @Param("minScoreFrom") Integer minScoreFrom,
            @Param("minScoreTo") Integer minScoreTo
    );

    /**
     * 根据分数范围查询可报考院校
     *
     * @param province    省份
     * @param subjectType 科类
     * @param year        年份
     * @param minScore    分数下限
     * @param maxScore    分数上限
     * @param limit       限制数量
     * @return 分数线列表
     */
    List<AdmissionScoreVO> selectByScoreRange(
            @Param("province") String province,
            @Param("subjectType") String subjectType,
            @Param("year") Integer year,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            @Param("limit") Integer limit
    );

    /**
     * 查询院校历年分数线趋势
     *
     * @param universityId 院校 ID
     * @param province     省份
     * @param subjectType  科类
     * @param years        年份列表
     * @return 分数线列表
     */
    List<AdmissionScoreVO> selectScoreTrend(
            @Param("universityId") Long universityId,
            @Param("province") String province,
            @Param("subjectType") String subjectType,
            @Param("years") List<Integer> years
    );

    /**
     * 统计院校分数线信息
     *
     * @param universityId 院校 ID
     * @param province     省份
     * @param subjectType  科类
     * @return 统计结果
     */
    Map<String, Object> selectScoreStatistics(
            @Param("universityId") Long universityId,
            @Param("province") String province,
            @Param("subjectType") String subjectType
    );

    /**
     * 查询同分段可报考院校
     *
     * @param province    省份
     * @param subjectType 科类
     * @param year        年份
     * @param score       分数
     * @param range       分数浮动范围
     * @param level       院校层次（可选）
     * @param limit       限制数量
     * @return 分数线列表
     */
    List<AdmissionScoreVO> selectSameScoreUniversities(
            @Param("province") String province,
            @Param("subjectType") String subjectType,
            @Param("year") Integer year,
            @Param("score") Integer score,
            @Param("range") Integer range,
            @Param("level") String level,
            @Param("limit") Integer limit
    );

    /**
     * 查询省份分数线统计
     *
     * @param province 省份
     * @param year     年份
     * @return 统计结果
     */
    List<Map<String, Object>> selectProvinceScoreDistribution(
            @Param("province") String province,
            @Param("year") Integer year
    );
}