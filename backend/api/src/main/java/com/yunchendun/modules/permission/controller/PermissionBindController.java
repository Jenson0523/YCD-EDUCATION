package com.yunchendun.modules.permission.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
import com.yunchendun.modules.permission.service.BindImportService;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final StudentMapper studentMapper;
    private final DataPermissionHelper dataPermissionHelper;
    private final BindImportService bindImportService;

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

    /**
     * 当前用户可为其请假的学生列表（小程序请假申请用）
     *  - 家长/学生：仅已绑定的孩子（忽略 keyword）
     *  - 班主任/任课教师：所辖班级的学生，支持 keyword 搜索（代请假可选人）
     *  - 管理员/校长/教务：全校学生，支持 keyword 搜索
     * 统一返回 { studentId, studentName, studentNo, className }
     */
    @Operation(summary = "可请假学生列表（按角色返回）")
    @GetMapping("/selectable-students")
    public ApiResponse<List<Map<String, Object>>> selectableStudents(
            @RequestParam(required = false) String keyword) {
        Long uid = StpUtil.getLoginIdAsLong();
        DataPermission dp = dataPermissionHelper.current();
        List<Map<String, Object>> result = new ArrayList<>();

        if (dp.isSelf()) {
            // 家长/学生：仅绑定孩子，补充班级年级信息
            List<ParentStudent> binds = parentStudentMapper.selectList(
                    new LambdaQueryWrapper<ParentStudent>()
                            .eq(ParentStudent::getParentUserId, uid));
            for (ParentStudent b : binds) {
                String cn = "", gn = "";
                Long cid = null;
                if (b.getStudentId() != null) {
                    Student stu = studentMapper.selectById(b.getStudentId());
                    if (stu != null) {
                        cn = stu.getClassName() == null ? "" : stu.getClassName();
                        gn = stu.getGradeName() == null ? "" : stu.getGradeName();
                        cid = stu.getClassId();
                    }
                }
                result.add(Map.of(
                        "studentId", b.getStudentId(),
                        "studentName", b.getStudentName() == null ? "" : b.getStudentName(),
                        "studentNo", b.getStudentNo() == null ? "" : b.getStudentNo(),
                        "classId", cid != null ? cid : "",
                        "className", cn,
                        "gradeName", gn
                ));
            }
            return ApiResponse.ok(result);
        }

        // 教师/管理员：从学生档案查询
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<Student>()
                .and(StringUtils.hasText(keyword), q -> q
                        .like(Student::getName, keyword)
                        .or().like(Student::getStudentNo, keyword))
                .orderByAsc(Student::getClassId)
                .last("LIMIT 100");

        if (dp.isClass()) {
            // 班主任/任课教师：限定所辖班级
            if (dp.hasNoClass()) return ApiResponse.ok(result);
            w.in(Student::getClassId, dp.getClassIds());
        }
        // ALL：不限制（全校）

        for (Student s : studentMapper.selectList(w)) {
            result.add(Map.of(
                    "studentId", s.getId(),
                    "studentName", s.getName() == null ? "" : s.getName(),
                    "studentNo", s.getStudentNo() == null ? "" : s.getStudentNo(),
                    "classId", s.getClassId() != null ? s.getClassId() : "",
                    "className", s.getClassName() == null ? "" : s.getClassName(),
                    "gradeName", s.getGradeName() == null ? "" : s.getGradeName()
            ));
        }
        return ApiResponse.ok(result);
    }

    // ==================== Excel 模板下载 / 导入 ====================

    @Operation(summary = "下载教师班级绑定模板")
    @GetMapping("/teacher-class/template")
    public void teacherClassTemplate(HttpServletResponse resp) {
        bindImportService.downloadTeacherClassTemplate(resp);
    }

    @Operation(summary = "下载家长学生绑定模板（支持多孩）")
    @GetMapping("/parent-student/template")
    public void parentStudentTemplate(HttpServletResponse resp) {
        bindImportService.downloadParentStudentTemplate(resp);
    }

    @Operation(summary = "导入教师班级绑定")
    @PostMapping("/teacher-class/import")
    public ApiResponse<Map<String, Object>> importTeacherClass(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(bindImportService.importTeacherClass(file));
    }

    @Operation(summary = "导入家长学生绑定（支持多孩）")
    @PostMapping("/parent-student/import")
    public ApiResponse<Map<String, Object>> importParentStudent(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(bindImportService.importParentStudent(file));
    }
}
