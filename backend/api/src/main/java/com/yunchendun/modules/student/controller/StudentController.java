package com.yunchendun.modules.student.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块: 学生学籍
 * 功能: 学生主档案接口（含数据权限过滤）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stu/students")
public class StudentController {

    private final StudentMapper studentMapper;
    private final DataPermissionHelper dataPermissionHelper;

    @GetMapping
    public ApiResponse<IPage<Student>> page(@RequestParam(defaultValue = "1") long pageNo,
                                            @RequestParam(defaultValue = "20") long pageSize) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();

        // 数据权限过滤
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isClass()) {
            // 班主任/任课老师：仅本人关联班级的学生
            if (dp.hasNoClass()) return ApiResponse.ok(Page.of(pageNo, pageSize));
            wrapper.in(Student::getClassId, dp.getClassIds());
        } else if (dp.isSelf()) {
            // 家长/学生：仅本人关联学生
            if (dp.hasNoStudent()) return ApiResponse.ok(Page.of(pageNo, pageSize));
            wrapper.in(Student::getId, dp.getStudentIds());
        }
        // ALL / GATE_VALID：全部可见

        wrapper.orderByDesc(Student::getCreatedAt);
        return ApiResponse.ok(studentMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }
}
