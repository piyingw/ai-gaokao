package com.gaokao.ai.skill.impl;

import com.gaokao.ai.skill.AbstractSkill;
import com.gaokao.ai.skill.SkillParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 网络搜索技能
 * 使用通义千问的搜索增强功能进行联网搜索
 */
@Slf4j
@Component
public class WebSearchSkill extends AbstractSkill {

    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public WebSearchSkill() {
        super("web-search-skill", "联网搜索院校信息，当数据库中不存在时通过网络获取");
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        String searchType = (String) params.getOrDefault("searchType", "university");

        if (query == null || query.isEmpty()) {
            return "请提供搜索关键词";
        }

        log.info("执行网络搜索: query={}, searchType={}", query, searchType);

        try {
            return searchWithQwen(query, searchType);
        } catch (Exception e) {
            log.error("网络搜索失败", e);
            return "网络搜索失败: " + e.getMessage() + "。建议您访问教育部官网或院校官方网站查询详细信息。";
        }
    }

    /**
     * 使用通义千问搜索增强进行联网搜索
     */
    private String searchWithQwen(String query, String searchType) {
        // 构建搜索提示词
        String systemPrompt = buildSearchPrompt(searchType);
        String userQuery = "请帮我搜索并提供以下信息的详细内容：" + query;

        // 调用通义千问 API（启用搜索增强）
        // 注意：通义千问的搜索增强需要使用原生 DashScope API
        String apiUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen-plus");

        Map<String, Object> input = new HashMap<>();
        input.put("messages", Arrays.asList(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userQuery)
        ));
        requestBody.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("enable_search", true);  // 启用搜索增强
        parameters.put("temperature", 0.7);
        parameters.put("max_tokens", 2048);
        requestBody.put("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.debug("搜索响应: {}", responseBody);

                // 解析响应
                if (responseBody.containsKey("output")) {
                    Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                    if (output.containsKey("text")) {
                        return (String) output.get("text");
                    } else if (output.containsKey("choices")) {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                        if (!choices.isEmpty()) {
                            Map<String, Object> firstChoice = choices.get(0);
                            if (firstChoice.containsKey("message")) {
                                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                                return (String) message.get("content");
                            }
                        }
                    }
                }
                return "搜索返回结果格式异常，请稍后重试";
            } else {
                return "搜索服务响应异常: " + response.getStatusCode();
            }
        } catch (Exception e) {
            log.error("调用通义千问API失败", e);
            // 返回友好的错误信息和备选建议
            return formatFallbackResponse(query, searchType, e.getMessage());
        }
    }

    /**
     * 构建搜索类型的提示词
     */
    private String buildSearchPrompt(String searchType) {
        if ("university".equals(searchType)) {
            return """
                    你是一个专业的院校信息助手，具有联网搜索能力。
                    当用户询问院校信息时，请通过网络搜索获取准确、最新的信息。

                    请提供以下内容：
                    1. 院校基本信息（名称、代码、层次、类型、所在地）
                    2. 办学性质（公办/民办）
                    3. 院校简介和特色
                    4. 优势专业和学科
                    5. 录取分数线参考（如有）
                    6. 官方网站链接

                    注意：
                    - 确保信息准确，注明信息来源
                    - 如果搜索不到确切信息，请如实告知
                    - 提供的信息应当是官方渠道可验证的
                    """;
        } else if ("major".equals(searchType)) {
            return """
                    你是一个专业的专业信息助手，具有联网搜索能力。
                    当用户询问专业信息时，请通过网络搜索获取准确、最新的信息。

                    请提供以下内容：
                    1. 专业名称和专业代码
                    2. 专业简介和学习内容
                    3. 就业方向和就业前景
                    4. 开设该专业的知名院校
                    5. 报考建议和要求

                    注意：
                    - 确保信息准确，注明信息来源
                    - 如果搜索不到确切信息，请如实告知
                    """;
        } else {
            return """
                    你是一个高考志愿填报助手，具有联网搜索能力。
                    请通过网络搜索获取用户所需的信息，确保信息准确、及时。
                    注明信息来源，如果搜索不到请如实告知。
                    """;
        }
    }

    /**
     * 生成备用响应（当API调用失败时）
     */
    private String formatFallbackResponse(String query, String searchType, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("抱歉，网络搜索服务暂时不可用。\n\n");
        sb.append("您可以通过以下渠道查询 ").append(query).append(" 的详细信息：\n\n");

        if ("university".equals(searchType)) {
            sb.append("1. 教育部官方网站：https://www.moe.gov.cn\n");
            sb.append("2. 全国高等学校名单查询：https://www.moe.gov.cn/jyb_xxgk/s5743/s5744/\n");
            sb.append("3. 各省份教育考试院官网\n");
            sb.append("4. 院校官方网站\n");
            sb.append("5. 阳光高考平台：https://gaokao.chsi.com.cn\n");
        } else if ("major".equals(searchType)) {
            sb.append("1. 教育部专业目录查询\n");
            sb.append("2. 阳光高考专业解读：https://gaokao.chsi.com.cn\n");
            sb.append("3. 各院校招生网站专业介绍\n");
        }

        sb.append("\n（错误详情：").append(error).append("）");
        return sb.toString();
    }

    @Override
    public List<SkillParameter> getParameters() {
        return Arrays.asList(
                SkillParameter.builder()
                        .name("query")
                        .type("string")
                        .description("搜索关键词或问题")
                        .required(true)
                        .build(),
                SkillParameter.builder()
                        .name("searchType")
                        .type("string")
                        .description("搜索类型: university(院校), major(专业), general(通用)")
                        .required(false)
                        .defaultValue("university")
                        .build()
        );
    }
}