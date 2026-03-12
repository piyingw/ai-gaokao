package com.gaokao.ai.skill.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.ai.skill.AbstractSkill;
import com.gaokao.ai.skill.SkillParameter;
import com.gaokao.data.entity.University;
import com.gaokao.data.mapper.UniversityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 院校查询技能
 */
@Slf4j
@Component
public class UniversityQuerySkill extends AbstractSkill {
    private final UniversityMapper universityMapper;

    public UniversityQuerySkill(UniversityMapper universityMapper) {
        super("university-query-skill", "根据条件查询院校信息，支持按省份、层次、类型筛选");
        this.universityMapper = universityMapper;
    }

    @Override
    public Object execute(Map<String, Object> params) {
        // 根据参数决定执行哪种查询
        String operation = (String) params.getOrDefault("operation", "query");
        
        switch (operation) {
            case "query":
                return queryUniversities(
                    (String) params.get("province"),
                    (String) params.get("level"),
                    (String) params.get("type"),
                    (Integer) params.get("minScore"),
                    (Integer) params.get("maxScore"),
                    (Integer) params.get("limit")
                );
            case "detail":
                return getUniversityDetail(
                    (Long) params.get("universityId"),
                    (String) params.get("universityName")
                );
            case "search":
                return searchUniversities(
                    (String) params.get("keyword"),
                    (Integer) params.get("limit")
                );
            default:
                return "不支持的操作: " + operation;
        }
    }

    @Override
    public List<SkillParameter> getParameters() {
        return Arrays.asList(
            SkillParameter.builder()
                .name("operation")
                .type("string")
                .description("操作类型: query(查询), detail(详情), search(搜索)")
                .required(true)
                .defaultValue("query")
                .build(),
            SkillParameter.builder()
                .name("province")
                .type("string")
                .description("省份")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("level")
                .type("string")
                .description("院校层次")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("type")
                .type("string")
                .description("院校类型")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("minScore")
                .type("integer")
                .description("最低分数")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("maxScore")
                .type("integer")
                .description("最高分数")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("limit")
                .type("integer")
                .description("返回数量限制")
                .required(false)
                .defaultValue(10)
                .build(),
            SkillParameter.builder()
                .name("universityId")
                .type("long")
                .description("院校ID")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("universityName")
                .type("string")
                .description("院校名称")
                .required(false)
                .build(),
            SkillParameter.builder()
                .name("keyword")
                .type("string")
                .description("搜索关键词")
                .required(false)
                .build()
        );
    }

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
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

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
                .collect(java.util.stream.Collectors.joining("\n\n"));
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