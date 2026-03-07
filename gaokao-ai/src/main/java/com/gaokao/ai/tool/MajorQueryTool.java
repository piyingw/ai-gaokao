package com.gaokao.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.data.entity.Major;
import com.gaokao.data.mapper.MajorMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 专业查询工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MajorQueryTool {

    private final MajorMapper majorMapper;

    @Tool("根据条件查询专业列表")
    public String queryMajors(String category, String keyword, Integer limit) {
        log.info("查询专业: category={}, keyword={}", category, keyword);

        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Major::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Major::getName, keyword);
        }
        
        wrapper.last("LIMIT " + (limit != null ? limit : 10));

        List<Major> majors = majorMapper.selectList(wrapper);

        if (majors.isEmpty()) {
            return "未找到符合条件的专业";
        }

        return majors.stream()
                .map(this::formatMajor)
                .collect(Collectors.joining("\n\n"));
    }

    @Tool("获取专业详细信息")
    public String getMajorDetail(Long majorId, String majorName) {
        log.info("查询专业详情: id={}, name={}", majorId, majorName);

        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        if (majorId != null) {
            wrapper.eq(Major::getId, majorId);
        } else if (majorName != null && !majorName.isEmpty()) {
            wrapper.like(Major::getName, majorName);
        } else {
            return "请提供专业ID或名称";
        }

        Major major = majorMapper.selectOne(wrapper);
        if (major == null) {
            return "未找到该专业";
        }

        return formatMajorDetail(major);
    }

    @Tool("根据性格特点推荐适合的专业")
    public String recommendMajorsByPersonality(String personalityType, String interests) {
        log.info("根据性格推荐专业: personality={}, interests={}", personalityType, interests);

        StringBuilder sb = new StringBuilder();
        sb.append("根据您的性格特点和兴趣爱好，推荐以下专业方向：\n\n");

        if (personalityType != null) {
            if (personalityType.contains("内向") || personalityType.contains("严谨")) {
                sb.append("【适合理性严谨型】\n");
                sb.append("- 计算机科学与技术：适合逻辑思维强的人\n");
                sb.append("- 会计学：适合细心严谨的人\n");
                sb.append("- 数学与应用数学：适合抽象思维强的人\n\n");
            }
            if (personalityType.contains("外向") || personalityType.contains("开放")) {
                sb.append("【适合外向开放型】\n");
                sb.append("- 市场营销：适合善于沟通的人\n");
                sb.append("- 新闻传播学：适合表达能力强的人\n");
                sb.append("- 旅游管理：适合喜欢社交的人\n\n");
            }
        }

        if (interests != null) {
            sb.append("【基于您的兴趣】\n");
            if (interests.contains("编程") || interests.contains("计算机")) {
                sb.append("- 软件工程、人工智能、数据科学与大数据技术\n");
            }
            if (interests.contains("写作") || interests.contains("文学")) {
                sb.append("- 汉语言文学、新闻学、广告学\n");
            }
            if (interests.contains("设计") || interests.contains("艺术")) {
                sb.append("- 视觉传达设计、环境设计、产品设计\n");
            }
            if (interests.contains("研究") || interests.contains("科学")) {
                sb.append("- 物理学、化学、生物科学\n");
            }
        }

        sb.append("\n建议结合自身分数和院校情况进行选择。");
        return sb.toString();
    }

    private String formatMajor(Major m) {
        return String.format("【%s】\n类别：%s | 专业类：%s\n学制：%s | 学位：%s",
                m.getName(),
                m.getCategory(),
                m.getSubCategory(),
                m.getDuration() != null ? m.getDuration() + "年" : "4年",
                m.getDegreeType());
    }

    private String formatMajorDetail(Major m) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(m.getName()).append("】\n\n");
        sb.append("基本信息：\n");
        sb.append("- 专业代码：").append(m.getCode()).append("\n");
        sb.append("- 学科门类：").append(m.getCategory()).append("\n");
        sb.append("- 专业类：").append(m.getSubCategory()).append("\n");
        sb.append("- 学制：").append(m.getDuration() != null ? m.getDuration() + "年" : "4年").append("\n");
        sb.append("- 学位类型：").append(m.getDegreeType()).append("\n");
        
        if (m.getIntro() != null) {
            sb.append("\n专业简介：\n").append(m.getIntro()).append("\n");
        }
        
        if (m.getEmployment() != null) {
            sb.append("\n就业方向：\n").append(m.getEmployment()).append("\n");
        }
        
        return sb.toString();
    }
}