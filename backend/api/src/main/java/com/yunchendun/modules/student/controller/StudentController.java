package com.yunchendun.modules.student.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import com.yunchendun.modules.student.service.StudentImportService;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/**
 * 模块: 学生学籍
 * 功能: 学生主档案接口（CRUD + Excel导入 + 数据权限过滤 + 自动开通家长账号）
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
    private final SysUserMapper sysUserMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /** 拥有写权限的管理角色 */
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "PRINCIPAL", "ACADEMIC");

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
        checkWritePermission();
        // 学籍号唯一校验
        Long dup = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, student.getStudentNo()));
        if (dup != null && dup > 0) return ApiResponse.fail(400, "学籍号已存在");
        student.setTenantId(1L);
        if (!StringUtils.hasText(student.getStatus())) student.setStatus("ACTIVE");
        studentMapper.insert(student);

        // 自动开通家长账号
        if (isTrue(student.getParentCreateAccount())) {
            createParentAccount(student);
        }

        return ApiResponse.ok(student);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Student student) {
        checkWritePermission();
        student.setId(id);
        studentMapper.updateById(student);

        // 编辑时如果启用自动开通家长账号
        if (isTrue(student.getParentCreateAccount())) {
            // 先检查是否已有关联家长
            Student cur = studentMapper.selectById(id);
            if (cur != null && cur.getParentUserId() == null) {
                cur.setGuardianName(student.getGuardianName());
                cur.setGuardianMobileEncrypted(student.getGuardianMobileEncrypted());
                createParentAccount(cur);
            }
        }

        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        checkWritePermission();
        studentMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ==================== 自动开通家长账号 ====================

    /**
     * 根据学生档案中的监护人信息，自动创建 PARENT 角色用户账号
     * 用户名=监护人姓名，密码=监护人手机号，自动绑定到该学生
     */
    private void createParentAccount(Student student) {
        String name = student.getGuardianName();
        String phone = student.getGuardianMobileEncrypted();
        if (!StringUtils.hasText(name) || !StringUtils.hasText(phone)) return;

        // 检查手机号是否已注册为家长用户（按用户名查找，因为用户名=监护人姓名）
        SysUser existUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, name)
                .eq(SysUser::getRoleCode, "PARENT")
                .last("LIMIT 1"));
        if (existUser != null) {
            student.setParentUserId(existUser.getId());
            studentMapper.updateById(student);
            // 绑定家长-学生关系（如果尚未绑定）
            ensureParentStudentBinding(existUser.getId(), student);
            return;
        }

        // 创建新家长用户
        SysUser parent = new SysUser();
        parent.setTenantId(1L);
        parent.setUsername(name);
        parent.setPasswordHash(passwordEncoder.encode(phone));
        parent.setRealName(name);
        parent.setMobileEncrypted(phone);
        parent.setRoleCode("PARENT");
        parent.setStatus("ACTIVE");
        sysUserMapper.insert(parent);

        // 更新学生档案的 parentUserId
        student.setParentUserId(parent.getId());
        Student upd = new Student();
        upd.setId(student.getId());
        upd.setParentUserId(parent.getId());
        studentMapper.updateById(upd);

        // 创建家长-学生绑定
        ParentStudent binding = new ParentStudent();
        binding.setTenantId(1L);
        binding.setParentUserId(parent.getId());
        binding.setParentName(name);
        binding.setStudentId(student.getId());
        binding.setStudentNo(student.getStudentNo());
        binding.setStudentName(student.getName());
        binding.setRelation("OTHER");
        parentStudentMapper.insert(binding);
    }

    /** 确保家长-学生绑定已存在 */
    private void ensureParentStudentBinding(Long parentUserId, Student student) {
        Long dup = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parentUserId)
                .eq(ParentStudent::getStudentId, student.getId()));
        if (dup == null || dup == 0) {
            ParentStudent binding = new ParentStudent();
            binding.setTenantId(1L);
            binding.setParentUserId(parentUserId);
            binding.setParentName(student.getGuardianName());
            binding.setStudentId(student.getId());
            binding.setStudentNo(student.getStudentNo());
            binding.setStudentName(student.getName());
            binding.setRelation("OTHER");
            parentStudentMapper.insert(binding);
        }
    }

    private boolean isTrue(Integer val) {
        return val != null && val == 1;
    }

    // ==================== Excel 模板 / 导入 ====================

    @GetMapping("/template")
    public void template(HttpServletResponse resp) {
        studentImportService.downloadTemplate(resp);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        checkWritePermission();
        return ApiResponse.ok(studentImportService.importStudents(file));
    }

    /** 写操作权限校验：仅管理员/校长/教务可写 */
    private void checkWritePermission() {
        Long uid = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(uid);
        if (user == null || user.getRoleCode() == null || !WRITE_ROLES.contains(user.getRoleCode())) {
            throw new RuntimeException("无写操作权限");
        }
    }
}
