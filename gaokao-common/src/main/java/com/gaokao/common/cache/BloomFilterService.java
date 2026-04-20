package com.gaokao.common.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 布隆过滤器服务
 *
 * 用于防止缓存穿透：
 * 1. 预加载所有有效的Key到布隆过滤器
 * 2. 查询前先判断Key是否可能存在
 * 3. 如果布隆过滤器判断不存在，直接返回，无需查询数据库
 *
 * 布隆过滤器特性：
 * - 可能存在误判（判断存在但实际不存在）
 * - 不会漏判（判断不存在则一定不存在）
 * - 空间效率高，适合海量数据
 *
 * 使用场景：
 * - 热点院校ID查询
 * - 专业ID查询
 * - 其他可能被恶意请求的Key
 */
@Slf4j
@Component
public class BloomFilterService {

    /**
     * 院校ID布隆过滤器
     * 预期元素数量：10000（全国院校数量）
     * 误判率：0.01%（1%）
     */
    private BloomFilter<String> universityIdFilter;

    /**
     * 专业ID布隆过滤器
     * 预期元素数量：50000（全国专业数量）
     */
    private BloomFilter<String> majorIdFilter;

    /**
     * 通用Key布隆过滤器
     * 用于动态添加的Key
     */
    private BloomFilter<String> generalKeyFilter;

    /**
     * 预期元素数量
     */
    private static final int EXPECTED_INSERTIONS_UNIVERSITY = 10000;
    private static final int EXPECTED_INSERTIONS_MAJOR = 50000;
    private static final int EXPECTED_INSERTIONS_GENERAL = 100000;

    /**
     * 误判率
     */
    private static final double FPP = 0.01; // 1%

    @PostConstruct
    public void init() {
        // 初始化布隆过滤器
        universityIdFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_INSERTIONS_UNIVERSITY,
                FPP
        );

        majorIdFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_INSERTIONS_MAJOR,
                FPP
        );

        generalKeyFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_INSERTIONS_GENERAL,
                FPP
        );

        log.info("布隆过滤器初始化完成：院校={}, 专业={}, 通用={}",
                EXPECTED_INSERTIONS_UNIVERSITY, EXPECTED_INSERTIONS_MAJOR, EXPECTED_INSERTIONS_GENERAL);
    }

    /**
     * 判断院校ID是否可能存在
     *
     * @param universityId 院校ID
     * @return true=可能存在，false=一定不存在
     */
    public boolean mightContainUniversity(String universityId) {
        return universityIdFilter.mightContain(universityId);
    }

    /**
     * 添加院校ID到布隆过滤器
     *
     * @param universityId 院校ID
     */
    public void putUniversity(String universityId) {
        universityIdFilter.put(universityId);
        log.debug("院校ID添加到布隆过滤器：{}", universityId);
    }

    /**
     * 批量添加院校ID
     *
     * @param universityIds 院校ID列表
     */
    public void putUniversities(List<String> universityIds) {
        for (String id : universityIds) {
            universityIdFilter.put(id);
        }
        log.info("批量添加院校ID到布隆过滤器，数量：{}", universityIds.size());
    }

    /**
     * 判断专业ID是否可能存在
     *
     * @param majorId 专业ID
     * @return true=可能存在，false=一定不存在
     */
    public boolean mightContainMajor(String majorId) {
        return majorIdFilter.mightContain(majorId);
    }

    /**
     * 添加专业ID到布隆过滤器
     *
     * @param majorId 专业ID
     */
    public void putMajor(String majorId) {
        majorIdFilter.put(majorId);
        log.debug("专业ID添加到布隆过滤器：{}", majorId);
    }

    /**
     * 批量添加专业ID
     *
     * @param majorIds 专业ID列表
     */
    public void putMajors(List<String> majorIds) {
        for (String id : majorIds) {
            majorIdFilter.put(id);
        }
        log.info("批量添加专业ID到布隆过滤器，数量：{}", majorIds.size());
    }

    /**
     * 判断通用Key是否可能存在
     *
     * @param key 缓存Key
     * @return true=可能存在，false=一定不存在
     */
    public boolean mightContainKey(String key) {
        return generalKeyFilter.mightContain(key);
    }

    /**
     * 添加通用Key到布隆过滤器
     *
     * @param key 缓存Key
     */
    public void putKey(String key) {
        generalKeyFilter.put(key);
        log.debug("通用Key添加到布隆过滤器：{}", key);
    }

    /**
     * 批量添加通用Key
     *
     * @param keys Key列表
     */
    public void putKeys(List<String> keys) {
        for (String key : keys) {
            generalKeyFilter.put(key);
        }
        log.info("批量添加Key到布隆过滤器，数量：{}", keys.size());
    }

    /**
     * 获取布隆过滤器统计信息
     */
    public String getStats() {
        return String.format(
                "布隆过滤器统计：院校近似元素数=%d, 专业近似元素数=%d, 通用近似元素数=%d",
                universityIdFilter.approximateElementCount(),
                majorIdFilter.approximateElementCount(),
                generalKeyFilter.approximateElementCount()
        );
    }

    /**
     * 清空通用布隆过滤器（重建）
     * 注意：Guava布隆过滤器不支持删除，只能重建
     */
    public void clearGeneralFilter() {
        generalKeyFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_INSERTIONS_GENERAL,
                FPP
        );
        log.info("通用布隆过滤器已重建");
    }
}