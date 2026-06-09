package com.yunchendun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模块: 平台级
 * 功能: 云辰盾后端接口服务启动类
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@SpringBootApplication
@MapperScan("com.yunchendun.**.mapper")
public class YcdApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(YcdApiApplication.class, args);
    }
}
