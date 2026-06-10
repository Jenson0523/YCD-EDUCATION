package com.yunchendun.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 模块: 平台级 / common
 * 功能: Jackson 序列化配置
 *   - 雪花算法 Long 主键(19位)超过 JS Number 安全整数范围(2^53)，
 *     前端(浏览器/小程序)会丢失精度导致 ID 错位（绑定失效、增删改查命中错误记录）。
 *   - 统一将 Long / long 序列化为 String，前端按字符串处理即可。
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Configuration
public class JacksonConfig {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter T_FMT = DateTimeFormatter.ofPattern(TIME_PATTERN);

    /**
     * 统一序列化/反序列化规则：
     *  - Long/long → String（雪花ID防精度丢失）
     *  - LocalDateTime/LocalDate/LocalTime → "yyyy-MM-dd HH:mm:ss" 等空格格式
     *    （前端 picker 传 "2026-06-10 14:00:00" 也能正确解析）
     */
    private SimpleModule buildModule() {
        SimpleModule module = new SimpleModule();
        // Long → String
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // 时间类型：空格格式，序列化与反序列化一致
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DT_FMT));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DT_FMT));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(D_FMT));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(D_FMT));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(T_FMT));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(T_FMT));
        return module;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.modulesToInstall(buildModule());
    }

    /**
     * 兜底：若有直接注入的 ObjectMapper 也注册该模块。
     */
    @Bean
    public SimpleModule ycdJacksonModule() {
        return buildModule();
    }
}
