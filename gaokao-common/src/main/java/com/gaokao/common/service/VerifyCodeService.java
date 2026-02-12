package com.gaokao.common.service;

import com.gaokao.common.config.MailConfig;
import com.gaokao.common.constant.SystemConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final MailConfig mailConfig;

    /**
     * 验证码长度
     */
    private static final int CODE_LENGTH = 6;

    /**
     * 验证码过期时间（分钟）
     */
    private static final long CODE_EXPIRE_MINUTES = 3;

    /**
     * 验证码发送间隔（秒）
     */
    private static final long SEND_INTERVAL_SECONDS = 60;

    /**
     * 发送验证码到邮箱
     *
     * @param email 目标邮箱
     * @return 是否发送成功
     */
    public boolean sendCodeToEmail(String email) {
        try {
            // 检查发送间隔限制
            if (!canSendCode(email)) {
                log.warn("验证码发送过于频繁: email={}", email);
                return false;
            }

            // 1. 生成6位随机验证码
            String code = generateCode();

            // 2. 存储到 Redis，设置3分钟过期
            String codeRedisKey = SystemConstants.RedisKey.EMAIL_CODE + email;
            redisTemplate.opsForValue().set(codeRedisKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 3. 设置发送时间标记，用于限制发送频率
            String timeRedisKey = SystemConstants.RedisKey.EMAIL_CODE_TIME + email;
            redisTemplate.opsForValue().set(timeRedisKey, String.valueOf(System.currentTimeMillis()), 
                                          SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

            // 4. 发送邮件
            sendEmail(email, code);

            log.info("验证码发送成功: email={}", email);
            return true;
        } catch (Exception e) {
            log.error("验证码发送失败: email={}, error={}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查是否可以发送验证码（防止发送过于频繁）
     */
    public boolean canSendCode(String email) {
        String redisKey = SystemConstants.RedisKey.EMAIL_CODE_TIME + email;
        String lastSendTimeStr = redisTemplate.opsForValue().get(redisKey);
        
        if (lastSendTimeStr != null) {
            long lastSendTime = Long.parseLong(lastSendTimeStr);
            long currentTime = System.currentTimeMillis();
            
            // 检查距离上次发送是否超过60秒
            return (currentTime - lastSendTime) >= (SEND_INTERVAL_SECONDS * 1000);
        }
        
        // 如果没有发送记录，则允许发送
        return true;
    }

    /**
     * 验证验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 是否验证通过
     */
    public boolean verifyCode(String email, String code) {
        String redisKey = SystemConstants.RedisKey.EMAIL_CODE + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功后删除验证码
            redisTemplate.delete(redisKey);
            // 同时删除发送时间标记
            redisTemplate.delete(SystemConstants.RedisKey.EMAIL_CODE_TIME + email);
            return true;
        }
        return false;
    }

    /**
     * 生成6位随机数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 发送邮件
     */
    private void sendEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailConfig.getUsername());
        message.setTo(toEmail);
        message.setSubject(mailConfig.getNickname() + " - 验证码");
        message.setText(buildEmailContent(code));

        mailSender.send(message);
        log.debug("邮件发送完成: to={}", toEmail);
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String code) {
        return String.format("""
                您好！

                您正在进行高考志愿填报系统的注册操作，验证码为：%s

                验证码有效期为3分钟，请尽快完成验证。

                如果这不是您本人的操作，请忽略此邮件。

                ------------------
                %s
                """, code, mailConfig.getNickname());
    }
}