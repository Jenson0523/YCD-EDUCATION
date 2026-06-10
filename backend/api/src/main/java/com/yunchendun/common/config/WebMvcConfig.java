package com.yunchendun.common.config;

import com.yunchendun.common.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

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

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
    }

    /** 静态资源：上传的图片对外访问 /uploads/** */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = Paths.get(uploadDir).toAbsolutePath().normalize().toString()
                .replace("\\", "/");
        if (!absPath.endsWith("/")) absPath += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absPath);
    }

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
