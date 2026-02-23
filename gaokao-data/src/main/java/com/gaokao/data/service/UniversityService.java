package com.gaokao.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.data.dto.UniversityQueryDTO;
import com.gaokao.data.vo.UniversityDetailVO;
import com.gaokao.data.vo.UniversityVO;

/**
 * 院校服务接口
 */
public interface UniversityService {

    /**
     * 分页查询院校
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    Page<UniversityVO> pageList(UniversityQueryDTO dto);

    /**
     * 获取院校详情
     *
     * @param id 院校 ID
     * @return 院校详情
     */
    UniversityDetailVO getDetail(Long id);

    /**
     * 获取院校历年分数线
     *
     * @param id          院校 ID
     * @param province    省份
     * @param subjectType 科类
     * @return 分数线列表
     */
    Object getScores(Long id, String province, String subjectType);

    /**
     * 获取院校开设专业
     *
     * @param id 院校 ID
     * @return 专业列表
     */
    Object getMajors(Long id);

    /**
     * 院校对比
     *
     * @param ids 院校 ID 列表
     * @return 对比结果
     */
    Object compare(Long[] ids);
}