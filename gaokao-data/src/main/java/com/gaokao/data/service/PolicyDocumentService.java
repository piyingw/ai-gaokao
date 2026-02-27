package com.gaokao.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.data.dto.PolicyQueryDTO;
import com.gaokao.data.vo.PolicyDocumentVO;

import java.util.List;

/**
 * 政策文档服务接口
 */
public interface PolicyDocumentService {

    /**
     * 分页查询政策文档
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    Page<PolicyDocumentVO> pageList(PolicyQueryDTO dto);

    /**
     * 获取文档详情
     *
     * @param id 文档ID
     * @return 文档详情
     */
    PolicyDocumentVO getDetail(Long id);

    /**
     * 搜索政策文档
     *
     * @param keyword 关键词
     * @param limit   数量限制
     * @return 文档列表
     */
    List<PolicyDocumentVO> search(String keyword, Integer limit);

    /**
     * 按类型获取文档
     *
     * @param type 文档类型
     * @return 文档列表
     */
    List<PolicyDocumentVO> listByType(String type);

    /**
     * 按省份获取文档
     *
     * @param province 省份
     * @return 文档列表
     */
    List<PolicyDocumentVO> listByProvince(String province);

    /**
     * 获取热门政策
     *
     * @param limit 数量限制
     * @return 文档列表
     */
    List<PolicyDocumentVO> getHotPolicies(Integer limit);

    /**
     * 获取文档类型列表
     *
     * @return 类型列表
     */
    List<String> listTypes();
}