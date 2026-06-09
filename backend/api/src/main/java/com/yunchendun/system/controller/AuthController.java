package com.yunchendun.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模块: 系统权限
 * 功能: 登录、登出、获取当前用户
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        SysUser user = userMapper.selectByUsername(req.username());
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ApiResponse.fail(400, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        return ApiResponse.ok(Map.of(
                "tokenName", StpUtil.getTokenName(),
                "tokenValue", StpUtil.getTokenValue(),
                "userId", user.getId(),
                "realName", user.getRealName(),
                "roleCode", user.getRoleCode() == null ? "" : user.getRoleCode(),
                "avatar", user.getAvatar() == null ? "" : user.getAvatar()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.<Map<String, Object>>fail(401, "用户不存在");
        return ApiResponse.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "roleCode", user.getRoleCode() == null ? "" : user.getRoleCode(),
                "avatar", user.getAvatar() == null ? "" : user.getAvatar()
        ));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}
