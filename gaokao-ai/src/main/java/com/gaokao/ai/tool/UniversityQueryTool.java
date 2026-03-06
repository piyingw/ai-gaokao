package com.gaokao.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.data.entity.University;
import com.gaokao.data.mapper.UniversityMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 院校查询工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniversityQueryTool {

    private final UniversityMapper universityMapper;

    @Tool("根据条件查询院校列表，支持按省份、层次、类型筛选")
    public String queryUniversities(
            String province,
            String level,
            String type,
            Integer minScore,
            Integer maxScore,
            Integer limit
    ) {
        log.info("查询院校: province={}, level={}, type={}, score=[{}, {}]", 
                province, level, type, minScore, maxScore);

        LambdaQueryWrapper<University> wrapper = new LambdaQueryWrapper<>();
        
        if (province != null && !province.isEmpty()) {
            wrapper.eq(University::getProvince, province);
        }
        if (level != null && !level.isEmpty()) {
            wrapper.eq(University::getLevel, level);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(University::getType, type);
        }
        
        wrapper.orderByAsc(University::getRanking);
        wrapper.last("LIMIT " + (limit != null ? limit : 10));

        List<University> universities = universityMapper.selectList(wrapper);

        if (universities.isEmpty()) {
            return "未找到符合条件的院校";
        }

        return universities.stream()
                .map(this::formatUniversity)
                .collect(Collectors.joining("\n\n"));
    }

    @Tool("根据院校ID或名称查询院校详细信息")
    public String getUniversityDetail(Long universityId, String universityName) {
        log.info("查询院校详情: id={}, name={}", universityId, universityName);

        LambdaQueryWrapper<University> wrapper = new LambdaQueryWrapper<>();
        if (universityId != null) {
            wrapper.eq(University::getId, universityId);
        } else if (universityName != null && !universityName.isEmpty()) {
            wrapper.like(University::getName, universityName);
        } else {
            return "请提供院校ID或名称";
        }

        University university = universityMapper.selectOne(wrapper);
        if (university == null) {
            return "未找到该院校";
        }

        return formatUniversityDetail(university);
    }

    @Tool("搜索院校，支持模糊匹配名称")
    public String searchUniversities(String keyword, Integer limit) {
        log.info("搜索院校: keyword={}", keyword);

        if (keyword == null || keyword.isEmpty()) {
            return "请提供搜索关键词";
        }

        LambdaQueryWrapper<University> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(University::getName, keyword)
                .or()
                .like(University::getTags, keyword);
        
        wrapper.orderByAsc(University::getRanking);
        wrapper.last("LIMIT " + (limit != null ? limit : 5));

        List<University> universities = universityMapper.selectList(wrapper);

        if (universities.isEmpty()) {
            return "未找到匹配的院校";
        }

        return universities.stream()
                .map(this::formatUniversity)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatUniversity(University u) {
        return String.format("【%s】\n代码：%s | 层次：%s | 类型：%s\n所在地：%s %s | 性质：%s\n排名：%s",
                u.getName(),
                u.getCode(),
                u.getLevel(),
                u.getType(),
                u.getProvince(),
                u.getCity(),
                u.getNature(),
                u.getRanking() != null ? u.getRanking() : "未排名");
    }

    private String formatUniversityDetail(University u) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(u.getName()).append("】\n\n");
        sb.append("基本信息：\n");
        sb.append("- 院校代码：").append(u.getCode()).append("\n");
        sb.append("- 所在地：").append(u.getProvince()).append(" ").append(u.getCity()).append("\n");
        sb.append("- 院校层次：").append(u.getLevel()).append("\n");
        sb.append("- 院校类型：").append(u.getType()).append("\n");
        sb.append("- 办学性质：").append(u.getNature()).append("\n");
        sb.append("- 综合排名：").append(u.getRanking() != null ? u.getRanking() : "未排名").append("\n");
        
        if (u.getIntro() != null) {
            sb.append("\n院校简介：\n").append(u.getIntro()).append("\n");
        }
        
        if (u.getFeatures() != null) {
            sb.append("\n特色专业：").append(u.getFeatures()).append("\n");
        }
        
        return sb.toString();
    }
}