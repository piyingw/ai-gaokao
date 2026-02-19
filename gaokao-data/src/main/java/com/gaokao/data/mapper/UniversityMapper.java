package com.gaokao.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gaokao.data.entity.University;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院校 Mapper 接口
 */
@Mapper
public interface UniversityMapper extends BaseMapper<University> {

}