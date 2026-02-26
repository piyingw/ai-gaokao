package com.gaokao.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gaokao.data.dto.UserApplicationDTO;
import com.gaokao.data.vo.UserApplicationVO;

import java.util.List;

/**
 * 用户志愿服务接口
 */
public interface UserApplicationService {

    /**
     * 分页查询用户志愿
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    Page<UserApplicationVO> pageList(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 获取志愿详情
     *
     * @param id     志愿ID
     * @param userId 用户ID
     * @return 志愿详情
     */
    UserApplicationVO getDetail(Long id, Long userId);

    /**
     * 创建志愿
     *
     * @param userId 用户ID
     * @param dto    志愿信息
     * @return 志愿ID
     */
    Long create(Long userId, UserApplicationDTO dto);

    /**
     * 更新志愿
     *
     * @param userId 用户ID
     * @param dto    志愿信息
     * @return 是否成功
     */
    Boolean update(Long userId, UserApplicationDTO dto);

    /**
     * 删除志愿
     *
     * @param id     志愿ID
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean delete(Long id, Long userId);

    /**
     * 提交志愿
     *
     * @param id     志愿ID
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean submit(Long id, Long userId);

    /**
     * 复制志愿
     *
     * @param id     志愿ID
     * @param userId 用户ID
     * @return 新志愿ID
     */
    Long copy(Long id, Long userId);

    /**
     * 获取用户最新志愿
     *
     * @param userId 用户ID
     * @return 志愿详情
     */
    UserApplicationVO getLatest(Long userId);

    /**
     * 分析志愿方案
     *
     * @param id     志愿ID
     * @param userId 用户ID
     * @return 分析结果
     */
    Object analyze(Long id, Long userId);
}