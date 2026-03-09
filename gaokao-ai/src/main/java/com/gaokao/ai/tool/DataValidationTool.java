package com.gaokao.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.data.entity.University;
import com.gaokao.data.entity.AdmissionScore;
import com.gaokao.data.mapper.UniversityMapper;
import com.gaokao.data.mapper.AdmissionScoreMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据验证工具
 * 用于验证AI返回的分数线等数据的准确性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataValidationTool {

    private final UniversityMapper universityMapper;
    private final AdmissionScoreMapper admissionScoreMapper;

    @Tool("验证院校信息是否存在")
    public String validateUniversityExists(Long universityId, String universityName) {
        log.info("验证院校信息: id={}, name={}", universityId, universityName);

        LambdaQueryWrapper<University> wrapper = new LambdaQueryWrapper<>();
        if (universityId != null) {
            wrapper.eq(University::getId, universityId);
        } else if (universityName != null && !universityName.isEmpty()) {
            wrapper.eq(University::getName, universityName);
        } else {
            return "错误：必须提供院校ID或院校名称之一";
        }

        University university = universityMapper.selectOne(wrapper);
        if (university == null) {
            return "验证失败：数据库中不存在该院校";
        }

        return String.format("验证成功：院校[%s]信息有效", university.getName());
    }

    @Tool("验证分数线数据是否存在")
    public String validateScoreExists(Long universityId, String province, String subjectType, Integer year) {
        log.info("验证分数线数据: universityId={}, province={}, subjectType={}, year={}",
                universityId, province, subjectType, year);

        if (universityId == null) {
            return "错误：必须提供院校ID";
        }

        LambdaQueryWrapper<AdmissionScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmissionScore::getUniversityId, universityId);

        if (province != null && !province.isEmpty()) {
            wrapper.eq(AdmissionScore::getProvince, province);
        }
        if (subjectType != null && !subjectType.isEmpty()) {
            wrapper.eq(AdmissionScore::getSubjectType, subjectType);
        }
        if (year != null) {
            wrapper.eq(AdmissionScore::getYear, year);
        }

        List<AdmissionScore> scores = admissionScoreMapper.selectList(wrapper);

        if (scores.isEmpty()) {
            return "验证失败：数据库中不存在相应的分数线数据";
        }

        return String.format("验证成功：找到%d条分数线记录", scores.size());
    }
}