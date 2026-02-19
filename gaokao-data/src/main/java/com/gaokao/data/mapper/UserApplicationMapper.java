package com.gaokao.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gaokao.data.entity.UserApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户志愿 Mapper 接口
 */
@Mapper
public interface UserApplicationMapper extends BaseMapper<UserApplication> {

}