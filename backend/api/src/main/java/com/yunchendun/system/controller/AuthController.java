package com.yunchendun.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yunchendun.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模块: 系统权限
 * 功能: 登录样板接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // Phase 1 样板：真实项目需替换为密码哈希校验、账号状态校验、登录失败锁定策略。
        StpUtil.login(1L);
        return ApiResponse.ok(Map.of(
                "tokenName", StpUtil.getTokenName(),
                "tokenValue", StpUtil.getTokenValue(),
                "realName", "演示管理员"
        ));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        StpUtil.checkLogin();
        return ApiResponse.ok(Map.of("userId", StpUtil.getLoginIdAsLong(), "roles", StpUtil.getRoleList()));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
