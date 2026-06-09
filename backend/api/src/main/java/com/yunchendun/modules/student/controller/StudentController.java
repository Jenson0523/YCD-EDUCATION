package com.yunchendun.modules.student.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import com.yunchendun.modules.student.service.StudentImportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 模块: 学生学籍
 * 功能: 学生主档案接口（CRUD + Excel导入 + 数据权限过滤）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stu/students")
public class StudentController {

    private final StudentMapper studentMapper;
    private final DataPermissionHelper dataPermissionHelper;
    private final StudentImportService studentImportService;

    @GetMapping
    public ApiResponse<IPage<Student>> page(@RequestParam(defaultValue = "1") long pageNo,
                                            @RequestParam(defaultValue = "20") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long classId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), q -> q
                .like(Student::getName, keyword).or().like(Student::getStudentNo, keyword));
        wrapper.eq(classId != null, Student::getClassId, classId);

        // 数据权限过滤
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isClass()) {
            if (dp.hasNoClass()) return ApiResponse.ok(Page.of(pageNo, pageSize));
            wrapper.in(Student::getClassId, dp.getClassIds());
        } else if (dp.isSelf()) {
            if (dp.hasNoStudent()) return ApiResponse.ok(Page.of(pageNo, pageSize));
            wrapper.in(Student::getId, dp.getStudentIds());
        }

        wrapper.orderByDesc(Student::getCreatedAt);
        return ApiResponse.ok(studentMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    public ApiResponse<Student> create(@RequestBody Student student) {
        // 学籍号唯一校验
        Long dup = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, student.getStudentNo()));
        if (dup != null && dup > 0) return ApiResponse.fail(400, "学籍号已存在");
        student.setTenantId(1L);
        if (!StringUtils.hasText(student.getStatus())) student.setStatus("ACTIVE");
        studentMapper.insert(student);
        return ApiResponse.ok(student);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentMapper.updateById(student);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ==================== Excel 模板 / 导入 ====================

    @GetMapping("/template")
    public void template(HttpServletResponse resp) {
        studentImportService.downloadTemplate(resp);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(studentImportService.importStudents(file));
    }
}
