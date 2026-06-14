package com.yunchendun.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
    private final TeacherClassMapper teacherClassMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final StudentMapper studentMapper;

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

    /**
     * 获取当前用户详细信息（含角色相关的绑定数据）
     * 班主任：所辖班级列表
     * 家长：已绑定学生列表
     * 教师：所教班级列表
     */
    @GetMapping("/my-info")
    public ApiResponse<Map<String, Object>> myInfo() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.<Map<String, Object>>fail(401, "用户不存在");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("roleCode", user.getRoleCode() == null ? "" : user.getRoleCode());
        info.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());

        String role = user.getRoleCode();
        if ("HEAD_TEACHER".equals(role) || "TEACHER".equals(role)) {
            List<TeacherClass> bindings = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>()
                            .eq(TeacherClass::getTeacherUserId, userId));
            List<Map<String, Object>> classes = bindings.stream().map(tc -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("classId", tc.getClassId());
                m.put("className", tc.getClassName());
                m.put("isHeadTeacher", tc.getIsHeadTeacher());
                m.put("subjectName", tc.getSubjectName());
                return m;
            }).collect(Collectors.toList());
            info.put("managedClasses", classes);

            // 班主任：额外标记
            if ("HEAD_TEACHER".equals(role)) {
                List<String> classNames = bindings.stream()
                        .map(TeacherClass::getClassName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                info.put("classNames", String.join("、", classNames));
            }
        }
        if ("PARENT".equals(role)) {
            List<ParentStudent> bindings = parentStudentMapper.selectList(
                    new LambdaQueryWrapper<ParentStudent>()
                            .eq(ParentStudent::getParentUserId, userId));
            List<Map<String, Object>> students = bindings.stream().map(ps -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("studentId", ps.getStudentId());
                m.put("studentNo", ps.getStudentNo());
                m.put("studentName", ps.getStudentName());
                m.put("relation", ps.getRelation());
                // 查学生表获取班级年级信息
                if (ps.getStudentId() != null) {
                    Student stu = studentMapper.selectById(ps.getStudentId());
                    if (stu != null) {
                        m.put("className", stu.getClassName());
                        m.put("gradeName", stu.getGradeName());
                    }
                }
                return m;
            }).collect(Collectors.toList());
            info.put("boundStudents", students);
        }

        return ApiResponse.ok(info);
    }

    /** 修改密码（当前登录用户，需校验原密码） */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePwdRequest req) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(401, "用户不存在");
        if (!passwordEncoder.matches(req.oldPassword(), user.getPasswordHash())) {
            return ApiResponse.fail(400, "原密码不正确");
        }
        if (req.newPassword() == null || req.newPassword().length() < 6) {
            return ApiResponse.fail(400, "新密码至少6位");
        }
        SysUser upd = new SysUser();
        upd.setId(userId);
        upd.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userMapper.updateById(upd);
        // 改密后强制下线，需重新登录
        StpUtil.logout();
        return ApiResponse.ok(null);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record ChangePwdRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {}
}
