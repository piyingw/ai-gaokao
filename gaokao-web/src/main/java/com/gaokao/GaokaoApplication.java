package com.gaokao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 高考志愿填报系统启动类
 */
@SpringBootApplication
@EnableScheduling
public class GaokaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaokaoApplication.class, args);
        System.out.println("========================================");
        System.out.println("   高考志愿填报系统启动成功！");
        System.out.println("   API 文档地址: http://localhost:8080/doc.html");
        System.out.println("========================================");
    }
}