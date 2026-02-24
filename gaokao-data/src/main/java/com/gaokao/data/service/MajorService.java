package com.gaokao.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.data.dto.MajorQueryDTO;
import com.gaokao.data.vo.MajorDetailVO;
import com.gaokao.data.vo.MajorVO;

import java.util.List;

/**
 * 专业服务接口
 */
public interface MajorService {

    /**
     * 分页查询专业
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    Page<MajorVO> pageList(MajorQueryDTO dto);

    /**
     * 获取专业详情
     *
     * @param id 专业ID
     * @return 专业详情
     */
    MajorDetailVO getDetail(Long id);

    /**
     * 根据学科门类获取专业列表
     *
     * @param category 学科门类
     * @return 专业列表
     */
    List<MajorVO> listByCategory(String category);

    /**
     * 根据院校ID获取该校开设的专业
     *
     * @param universityId 院校ID
     * @return 专业列表
     */
    List<MajorVO> listByUniversityId(Long universityId);

    /**
     * 搜索专业（模糊查询）
     *
     * @param keyword 关键词
     * @param limit   数量限制
     * @return 专业列表
     */
    List<MajorVO> search(String keyword, Integer limit);

    /**
     * 根据性格类型推荐专业
     *
     * @param personalityType 性格类型（MBTI/霍兰德代码）
     * @return 推荐专业列表
     */
    List<MajorVO> recommendByPersonality(String personalityType);

    /**
     * 获取所有学科门类
     *
     * @return 学科门类列表
     */
    List<String> listCategories();

    /**
     * 根据学科门类获取专业类
     *
     * @param category 学科门类
     * @return 专业类列表
     */
    List<String> listSubCategories(String category);
}