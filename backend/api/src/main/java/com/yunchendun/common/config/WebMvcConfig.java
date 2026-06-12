package com.yunchendun.common.config;

import com.yunchendun.common.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 模块: 平台级 / common
 * 功能: Web MVC 配置 — 拦截器注册、CORS
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        // 受保护文件预览自带 token 校验（image标签无法带Header）
                        "/api/files/preview",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
    }

    /*
     * 安全说明：/uploads/** 公开静态映射已移除——人脸照片等敏感图片
     * 一律经 /api/files/preview 鉴权访问，防止 URL 泄露导致人脸数据外泄。
     */

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
