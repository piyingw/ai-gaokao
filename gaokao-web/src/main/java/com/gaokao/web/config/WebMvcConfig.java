package com.gaokao.web.config;

import com.gaokao.web.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/code/**",
                        "/api/university/list",
                        "/api/university/*/scores",
                        "/api/university/*/majors",
                        "/api/university/*",
                        "/api/major/list",
                        "/api/major/*",
                        "/api/member/products",       // 会员商品列表（公开）
                        "/api/policy/list",
                        "/api/policy/*",
                        "/api/ai/qa",
                        "/api/ai/policy/list",
                        "/api/ai/policy/*",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
    }
}