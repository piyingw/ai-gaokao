package com.gaokao.web.config;

import com.gaokao.common.cache.BloomFilterService;
import com.gaokao.data.mapper.UniversityMapper;
import com.gaokao.data.mapper.MajorMapper;
import com.gaokao.data.entity.University;
import com.gaokao.data.entity.Major;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 布隆过滤器预热Runner
 *
 * 在系统启动时预加载院校ID和专业ID到布隆过滤器
 * 防止恶意请求不存在的ID导致的缓存穿透
 *
 * 执行时机：在缓存预热之后执行（Order=2）
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class BloomFilterPreheatRunner implements ApplicationRunner {

    private final BloomFilterService bloomFilterService;
    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;

    @Override
    public void run(ApplicationArguments args) {
        log.info("======== 开始布隆过滤器预热 ========");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 预热院校ID布隆过滤器
            preheatUniversityIds();

            // 2. 预热专业ID布隆过滤器
            preheatMajorIds();

            long duration = System.currentTimeMillis() - startTime;
            log.info("======== 布隆过滤器预热完成，耗时: {}ms ========", duration);
            log.info("布隆过滤器统计: {}", bloomFilterService.getStats());

        } catch (Exception e) {
            log.error("布隆过滤器预热失败", e);
        }
    }

    /**
     * 预热院校ID
     */
    private void preheatUniversityIds() {
        log.info("开始预热院校ID到布隆过滤器...");

        // 查询所有院校ID
        List<University> universities = universityMapper.selectList(
                new LambdaQueryWrapper<University>()
                        .select(University::getId)
                        .eq(University::getDeleted, 0)
        );

        // 提取ID字符串
        List<String> universityIds = universities.stream()
                .map(u -> String.valueOf(u.getId()))
                .collect(Collectors.toList());

        // 添加到布隆过滤器
        bloomFilterService.putUniversities(universityIds);

        log.info("院校ID预热完成，数量: {}", universityIds.size());
    }

    /**
     * 预热专业ID
     */
    private void preheatMajorIds() {
        log.info("开始预热专业ID到布隆过滤器...");

        // 查询所有专业ID
        List<Major> majors = majorMapper.selectList(
                new LambdaQueryWrapper<Major>()
                        .select(Major::getId)
                        .eq(Major::getDeleted, 0)
        );

        // 提取ID字符串
        List<String> majorIds = majors.stream()
                .map(m -> String.valueOf(m.getId()))
                .collect(Collectors.toList());

        // 添加到布隆过滤器
        bloomFilterService.putMajors(majorIds);

        log.info("专业ID预热完成，数量: {}", majorIds.size());
    }
}