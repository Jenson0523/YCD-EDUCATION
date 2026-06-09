package com.yunchendun.common.security;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模块: 数据权限
 * 功能: 根据当前登录用户角色，构建数据可见范围
 */
@Component
@RequiredArgsConstructor
public class DataPermissionHelper {

    private final SysUserMapper userMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final StudentMapper studentMapper;

    /** 拥有全部数据权限的角色 */
    private static final Set<String> ALL_ROLES =
            Set.of("ADMIN", "PRINCIPAL", "ACADEMIC", "HR", "FINANCE");

    /**
     * 获取当前登录用户的数据权限上下文
     */
    public DataPermission current() {
        Long uid = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(uid);
        String role = user != null ? user.getRoleCode() : null;

        DataPermission dp = new DataPermission();
        dp.setUserId(uid);
        dp.setRoleCode(role);

        if (role == null || ALL_ROLES.contains(role)) {
            dp.setScope("ALL");
        } else if ("HEAD_TEACHER".equals(role) || "TEACHER".equals(role)) {
            dp.setScope("CLASS");
            dp.setClassIds(loadTeacherClassIds(uid));
        } else if ("GATE".equals(role)) {
            dp.setScope("GATE_VALID");
        } else if ("PARENT".equals(role) || "STUDENT".equals(role)) {
            dp.setScope("SELF");
            dp.setStudentIds(loadParentStudentIds(uid));
        } else {
            // 未知角色默认最小权限
            dp.setScope("SELF");
            dp.setStudentIds(Collections.emptyList());
        }
        return dp;
    }

    /** 当前教师关联的班级ID列表 */
    public List<Long> loadTeacherClassIds(Long teacherUserId) {
        return teacherClassMapper.selectList(
                        new LambdaQueryWrapper<TeacherClass>()
                                .eq(TeacherClass::getTeacherUserId, teacherUserId))
                .stream().map(TeacherClass::getClassId)
                .distinct().collect(Collectors.toList());
    }

    /** 当前家长关联的学生ID列表 */
    public List<Long> loadParentStudentIds(Long parentUserId) {
        return parentStudentMapper.selectList(
                        new LambdaQueryWrapper<ParentStudent>()
                                .eq(ParentStudent::getParentUserId, parentUserId))
                .stream().map(ParentStudent::getStudentId)
                .distinct().collect(Collectors.toList());
    }

    /** 当前家长关联的学籍号列表（人脸/请假用 student_no 查询时用） */
    public List<String> loadParentStudentNos(Long parentUserId) {
        return parentStudentMapper.selectList(
                        new LambdaQueryWrapper<ParentStudent>()
                                .eq(ParentStudent::getParentUserId, parentUserId))
                .stream().map(ParentStudent::getStudentNo)
                .filter(s -> s != null && !s.isBlank())
                .distinct().collect(Collectors.toList());
    }

    /** 根据班级ID列表查出其下所有学生ID（班主任查门卫核验/学生档案用） */
    public List<Long> studentIdsByClasses(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) return Collections.emptyList();
        return studentMapper.selectList(
                        new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds))
                .stream().map(Student::getId)
                .distinct().collect(Collectors.toList());
    }
}
