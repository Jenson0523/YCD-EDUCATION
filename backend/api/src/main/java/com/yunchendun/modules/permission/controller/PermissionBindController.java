package com.yunchendun.modules.permission.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块: 数据权限
 * 功能: 教师-班级、家长-学生 绑定管理（管理员后台）+ 当前用户关联查询
 */
@Tag(name = "数据权限绑定")
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionBindController {

    private final TeacherClassMapper teacherClassMapper;
    private final ParentStudentMapper parentStudentMapper;

    // ==================== 教师-班级绑定 ====================

    @Operation(summary = "教师班级绑定列表")
    @GetMapping("/teacher-class")
    public ApiResponse<Page<TeacherClass>> teacherClassList(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long teacherUserId,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TeacherClass> w = new LambdaQueryWrapper<TeacherClass>()
                .eq(teacherUserId != null, TeacherClass::getTeacherUserId, teacherUserId)
                .like(StringUtils.hasText(keyword), TeacherClass::getTeacherName, keyword)
                .orderByDesc(TeacherClass::getCreatedAt);
        return ApiResponse.ok(teacherClassMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    @Operation(summary = "新增教师班级绑定")
    @PostMapping("/teacher-class")
    public ApiResponse<TeacherClass> bindTeacherClass(@RequestBody TeacherClass req) {
        // 去重
        TeacherClass exist = teacherClassMapper.selectOne(new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getTeacherUserId, req.getTeacherUserId())
                .eq(TeacherClass::getClassId, req.getClassId()));
        if (exist != null) return ApiResponse.fail(400, "该教师已绑定此班级");
        req.setTenantId(1L);
        teacherClassMapper.insert(req);
        return ApiResponse.ok(req);
    }

    @Operation(summary = "解除教师班级绑定")
    @DeleteMapping("/teacher-class/{id}")
    public ApiResponse<Void> unbindTeacherClass(@PathVariable Long id) {
        teacherClassMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ==================== 家长-学生绑定 ====================

    @Operation(summary = "家长学生绑定列表")
    @GetMapping("/parent-student")
    public ApiResponse<Page<ParentStudent>> parentStudentList(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long parentUserId,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ParentStudent> w = new LambdaQueryWrapper<ParentStudent>()
                .eq(parentUserId != null, ParentStudent::getParentUserId, parentUserId)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(ParentStudent::getParentName, keyword)
                        .or().like(ParentStudent::getStudentName, keyword))
                .orderByDesc(ParentStudent::getCreatedAt);
        return ApiResponse.ok(parentStudentMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    @Operation(summary = "新增家长学生绑定")
    @PostMapping("/parent-student")
    public ApiResponse<ParentStudent> bindParentStudent(@RequestBody ParentStudent req) {
        ParentStudent exist = parentStudentMapper.selectOne(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, req.getParentUserId())
                .eq(ParentStudent::getStudentId, req.getStudentId()));
        if (exist != null) return ApiResponse.fail(400, "该家长已绑定此学生");
        req.setTenantId(1L);
        parentStudentMapper.insert(req);
        return ApiResponse.ok(req);
    }

    @Operation(summary = "解除家长学生绑定")
    @DeleteMapping("/parent-student/{id}")
    public ApiResponse<Void> unbindParentStudent(@PathVariable Long id) {
        parentStudentMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ==================== 当前用户关联查询（小程序用） ====================

    @Operation(summary = "我关联的孩子（家长用）")
    @GetMapping("/my-students")
    public ApiResponse<List<ParentStudent>> myStudents() {
        Long uid = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(parentStudentMapper.selectList(
                new LambdaQueryWrapper<ParentStudent>()
                        .eq(ParentStudent::getParentUserId, uid)));
    }

    @Operation(summary = "我关联的班级（教师用）")
    @GetMapping("/my-classes")
    public ApiResponse<List<TeacherClass>> myClasses() {
        Long uid = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherUserId, uid)));
    }
}
