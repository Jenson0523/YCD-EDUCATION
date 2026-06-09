package com.yunchendun.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 模块: 平台级 / common
 * 功能: 安全相关 Bean 配置（仅 BCrypt，不引入 Spring Security 过滤链）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
