package com.yunchendun.system.init;

import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 模块: 系统
 * 功能: 启动初始化 — 首次运行时自动创建管理员账号
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.selectByUsername("admin") == null) {
            String defaultPassword = "Admin@123";
            SysUser admin = new SysUser();
            admin.setId(1L);
            admin.setTenantId(1L);
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            admin.setRealName("系统管理员");
            admin.setStatus("ACTIVE");
            admin.setRoleCode("ADMIN");
            userMapper.insert(admin);
            log.info("==========================================");
            log.info("  管理员账号已创建");
            log.info("  用户名: admin");
            log.info("  密  码: {}", defaultPassword);
            log.info("==========================================");
        }
    }
}
