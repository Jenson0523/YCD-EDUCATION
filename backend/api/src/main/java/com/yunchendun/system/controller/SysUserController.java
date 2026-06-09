package com.yunchendun.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 功能: 用户管理接口（增删改查、重置密码）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/users")
public class SysUserController {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank())
            wrapper.like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword);
        if (roleCode != null && !roleCode.isBlank())
            wrapper.eq(SysUser::getRoleCode, roleCode);
        wrapper.select(SysUser::getId, SysUser::getUsername, SysUser::getRealName,
                SysUser::getRoleCode, SysUser::getStatus, SysUser::getClassId, SysUser::getCreatedAt);
        return ApiResponse.ok(userMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody CreateUserRequest req) {
        if (userMapper.selectByUsername(req.username()) != null)
            return ApiResponse.fail(409, "用户名已存在");
        SysUser user = new SysUser();
        user.setTenantId(1L);
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRealName(req.realName());
        user.setRoleCode(req.roleCode());
        user.setClassId(req.classId());
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        SysUser user = new SysUser();
        user.setId(id);
        if (req.realName() != null) user.setRealName(req.realName());
        if (req.roleCode() != null) user.setRoleCode(req.roleCode());
        if (req.classId() != null) user.setClassId(req.classId());
        if (req.status() != null) user.setStatus(req.status());
        userMapper.updateById(user);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable Long id) {
        String newPwd = "Ycd@" + (int)(Math.random() * 9000 + 1000);
        SysUser user = new SysUser();
        user.setId(id);
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        return ApiResponse.ok(Map.of("newPassword", newPwd));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String realName,
            @NotBlank String roleCode,
            Long classId) {}

    public record UpdateUserRequest(String realName, String roleCode, String status, Long classId) {}
}
