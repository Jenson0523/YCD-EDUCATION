package com.yunchendun.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
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
 * 功能: 用户管理接口（增删改查、重置密码）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/users")
public class SysUserController {

    private final SysUserMapper userMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<IPage<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank())
            wrapper.and(w -> w.like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword));
        if (roleCode != null && !roleCode.isBlank()) {
            String[] codes = roleCode.split(",");
            if (codes.length == 1) {
                wrapper.eq(SysUser::getRoleCode, roleCode);
            } else {
                wrapper.in(SysUser::getRoleCode, Arrays.asList(codes));
            }
        }
        wrapper.select(SysUser::getId, SysUser::getUsername, SysUser::getRealName,
                SysUser::getRoleCode, SysUser::getStatus, SysUser::getClassId,
                SysUser::getMobileEncrypted, SysUser::getGender, SysUser::getCreatedAt);
        wrapper.orderByAsc(SysUser::getRoleCode, SysUser::getCreatedAt);
        IPage<SysUser> userPage = userMapper.selectPage(Page.of(pageNo, pageSize), wrapper);

        // 批量查询绑定数据
        List<Long> allUserIds = userPage.getRecords().stream().map(SysUser::getId).collect(Collectors.toList());

        // 家长→学生绑定
        Set<Long> parentIds = userPage.getRecords().stream()
                .filter(u -> "PARENT".equals(u.getRoleCode())).map(SysUser::getId).collect(Collectors.toSet());
        Map<Long, List<String>> parentStudents = new HashMap<>();
        if (!parentIds.isEmpty()) {
            List<ParentStudent> bindings = parentStudentMapper.selectList(
                    new LambdaQueryWrapper<ParentStudent>().in(ParentStudent::getParentUserId, parentIds));
            for (ParentStudent ps : bindings) {
                parentStudents.computeIfAbsent(ps.getParentUserId(), k -> new ArrayList<>())
                        .add(ps.getStudentName());
            }
        }

        // 教师→班级绑定
        Set<Long> teacherIds = userPage.getRecords().stream()
                .filter(u -> "HEAD_TEACHER".equals(u.getRoleCode()) || "TEACHER".equals(u.getRoleCode()))
                .map(SysUser::getId).collect(Collectors.toSet());
        Map<Long, List<String>> teacherClasses = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            List<TeacherClass> bindings = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>().in(TeacherClass::getTeacherUserId, teacherIds));
            for (TeacherClass tc : bindings) {
                teacherClasses.computeIfAbsent(tc.getTeacherUserId(), k -> new ArrayList<>())
                        .add(tc.getClassName());
            }
        }

        // 组装返回
        IPage<Map<String, Object>> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (SysUser u : userPage.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("roleCode", u.getRoleCode());
            m.put("status", u.getStatus());
            m.put("classId", u.getClassId());
            m.put("mobileEncrypted", u.getMobileEncrypted());
            m.put("gender", u.getGender());
            m.put("createdAt", u.getCreatedAt());
            m.put("boundStudents", parentStudents.getOrDefault(u.getId(), Collections.emptyList()));
            m.put("managedClasses", teacherClasses.getOrDefault(u.getId(), Collections.emptyList()));
            records.add(m);
        }
        result.setRecords(records);
        return ApiResponse.ok(result);
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
        user.setMobileEncrypted(req.mobile());
        user.setGender(req.gender());
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
        if (req.mobile() != null) user.setMobileEncrypted(req.mobile());
        if (req.gender() != null) user.setGender(req.gender());
        if (req.status() != null) user.setStatus(req.status());
        userMapper.updateById(user);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable Long id,
                                                          @RequestBody(required = false) Map<String, String> body) {
        String newPwd = body != null && body.get("newPassword") != null && !body.get("newPassword").isBlank()
                ? body.get("newPassword")
                : "Ycd@" + (int) (Math.random() * 9000 + 1000);
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
            Long classId,
            String mobile,
            String gender) {}

    public record UpdateUserRequest(String realName, String roleCode, String status,
                                    Long classId, String mobile, String gender) {}
}
