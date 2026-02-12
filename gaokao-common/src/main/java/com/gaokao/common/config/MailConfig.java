package com.gaokao.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.mail")
public class MailConfig {

    /**
     * 发件人邮箱
     */
    private String username;

    /**
     * 发件人昵称
     */
    private String nickname = "高考志愿填报系统";
}