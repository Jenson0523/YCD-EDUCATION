package com.yunchendun.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.modules.academic.domain.EduClass;
import com.yunchendun.modules.academic.domain.EduGrade;
import com.yunchendun.modules.academic.mapper.EduClassMapper;
import com.yunchendun.modules.academic.mapper.EduGradeMapper;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块: 学生学籍
 * 功能: 学籍 Excel 模板下载与批量导入（含自动开通家长账号 + 自动创建班级）
 */
@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final StudentMapper studentMapper;
    private final EduClassMapper classMapper;
    private final EduGradeMapper gradeMapper;
    private final SysUserMapper sysUserMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final DataFormatter fmt = new DataFormatter();

    private static final String[] HEADERS =
            {"学籍号", "姓名", "性别(男/女)", "年级", "班级名称", "状态(在读/休学)",
             "监护人姓名", "监护人手机号", "自动开通账号(是/否)"};

    public void downloadTemplate(HttpServletResponse resp) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("学生学籍");
            CellStyle head = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
            head.setFont(f);
            head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setAlignment(HorizontalAlignment.CENTER);

            Row hr = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(HEADERS[i]); c.setCellStyle(head);
                sheet.setColumnWidth(i, 5200);
            }
            String[][] samples = {
                    {"2024010", "陈明", "男", "高一", "高一(1)班", "在读", "陈建国", "13800138001", "是"},
                    {"2024011", "刘静", "女", "高一", "高一(1)班", "在读", "", "", "否"},
            };
            int r = 1;
            for (String[] s : samples) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < s.length; i++) row.createCell(i).setCellValue(s[i]);
            }

            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String name = URLEncoder.encode("学生学籍导入模板", StandardCharsets.UTF_8).replace("+", "%20");
            resp.setHeader("Content-Disposition",
                    "attachment; filename=\"" + name + ".xlsx\"; filename*=UTF-8''" + name + ".xlsx");
            OutputStream os = resp.getOutputStream();
            wb.write(os); os.flush();
        } catch (Exception e) {
            throw new RuntimeException("模板生成失败: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> importStudents(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int parentCreated = 0;
        int classCreated = 0;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isBlank(row)) continue;
                int rowNo = r + 1;
                String studentNo = str(row, 0);
                String name = str(row, 1);
                String gender = str(row, 2);
                String grade  = str(row, 3);
                String className = str(row, 4);
                String statusText = str(row, 5);
                String guardianName  = str(row, 6);
                String guardianPhone = str(row, 7);
                String autoCreateFlag = str(row, 8);

                if (studentNo.isBlank() || name.isBlank()) {
                    errors.add("第" + rowNo + "行：学籍号或姓名为空"); continue;
                }
                // 学籍号唯一
                Long dup = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, studentNo));
                if (dup != null && dup > 0) {
                    errors.add("第" + rowNo + "行：学籍号 " + studentNo + " 已存在，跳过"); continue;
                }

                Student s = new Student();
                s.setTenantId(1L);
                s.setStudentNo(studentNo);
                s.setName(name);
                s.setGender(gender);
                s.setGradeName(grade);
                s.setClassName(className);
                s.setStatus("休学".equals(statusText) ? "SUSPENDED" : "ACTIVE");

                // 监护人信息
                if (StringUtils.hasText(guardianName)) s.setGuardianName(guardianName);
                if (StringUtils.hasText(guardianPhone)) s.setGuardianMobileEncrypted(guardianPhone);

                // 按班级名称匹配 class_id；如果不存在则自动创建
                if (!className.isBlank()) {
                    EduClass clazz = classMapper.selectOne(new LambdaQueryWrapper<EduClass>()
                            .eq(EduClass::getClassName, className).last("LIMIT 1"));
                    if (clazz != null) {
                        s.setClassId(clazz.getId());
                    } else {
                        // 自动创建班级
                        clazz = autoCreateClass(grade, className);
                        s.setClassId(clazz.getId());
                        classCreated++;
                    }
                }

                // 是否自动开通家长账号
                boolean autoCreate = "是".equals(autoCreateFlag) || "1".equals(autoCreateFlag);
                if (autoCreate) s.setParentCreateAccount(1);
                else s.setParentCreateAccount(0);

                studentMapper.insert(s);

                // 自动开通家长账号
                if (autoCreate && StringUtils.hasText(guardianName) && StringUtils.hasText(guardianPhone)) {
                    try {
                        createParentAccount(s, guardianName, guardianPhone);
                        parentCreated++;
                    } catch (Exception e) {
                        errors.add("第" + rowNo + "行：家长账号创建失败 - " + e.getMessage());
                    }
                }

                success++;
            }
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("failed", errors.size());
        m.put("errors", errors);
        m.put("parentCreated", parentCreated);
        m.put("classCreated", classCreated);
        return m;
    }

    // ==================== 自动创建班级 ====================

    /**
     * 当班级不存在时，自动创建班级信息。
     * 如果年级也不存在，则自动创建年级。
     */
    private EduClass autoCreateClass(String gradeName, String className) {
        // 1. 查找或创建年级
        Long gradeId = null;
        if (StringUtils.hasText(gradeName)) {
            EduGrade grd = gradeMapper.selectOne(new LambdaQueryWrapper<EduGrade>()
                    .eq(EduGrade::getGradeName, gradeName).last("LIMIT 1"));
            if (grd == null) {
                grd = new EduGrade();
                grd.setTenantId(1L);
                grd.setGradeName(gradeName);
                grd.setGradeCode(gradeName);
                // 根据年级名称推断学段
                grd.setSchoolSection(inferSchoolSection(gradeName));
                grd.setSortOrder(1);
                gradeMapper.insert(grd);
            }
            gradeId = grd.getId();
        }

        // 2. 创建班级
        EduClass clazz = new EduClass();
        clazz.setTenantId(1L);
        clazz.setGradeId(gradeId);
        clazz.setClassName(className);
        clazz.setClassCode(className);
        clazz.setStudentCount(0);
        classMapper.insert(clazz);
        return clazz;
    }

    // ==================== 自动开通家长账号 ====================

    /**
     * 为导入的学生自动创建 PARENT 用户并绑定。
     * 用户名 = 监护人姓名，密码 = 监护人手机号。
     */
    private void createParentAccount(Student student, String guardianName, String guardianPhone) {
        // 检查是否已存在同名 PARENT 用户
        SysUser existUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, guardianName)
                .eq(SysUser::getRoleCode, "PARENT")
                .last("LIMIT 1"));

        Long parentUserId;
        if (existUser != null) {
            parentUserId = existUser.getId();
        } else {
            // 创建新家长用户
            SysUser parent = new SysUser();
            parent.setTenantId(1L);
            parent.setUsername(guardianName);
            parent.setPasswordHash(passwordEncoder.encode(guardianPhone));
            parent.setRealName(guardianName);
            parent.setMobileEncrypted(guardianPhone);
            parent.setRoleCode("PARENT");
            parent.setStatus("ACTIVE");
            sysUserMapper.insert(parent);
            parentUserId = parent.getId();
        }

        // 更新学生档案的 parentUserId
        Student upd = new Student();
        upd.setId(student.getId());
        upd.setParentUserId(parentUserId);
        studentMapper.updateById(upd);

        // 创建家长-学生绑定（如果尚未存在）
        Long bindingExists = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parentUserId)
                .eq(ParentStudent::getStudentId, student.getId()));
        if (bindingExists == null || bindingExists == 0) {
            ParentStudent binding = new ParentStudent();
            binding.setTenantId(1L);
            binding.setParentUserId(parentUserId);
            binding.setParentName(guardianName);
            binding.setStudentId(student.getId());
            binding.setStudentNo(student.getStudentNo());
            binding.setStudentName(student.getName());
            binding.setRelation("OTHER");
            parentStudentMapper.insert(binding);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 根据年级名称推断学段
     */
    private String inferSchoolSection(String gradeName) {
        if (gradeName == null) return "HIGH";
        String n = gradeName.trim();
        if (n.matches("^[一二三四五六]年级$") || n.matches("^小学.*")) return "PRIMARY";
        if (n.matches("^[七八九]年级$") || n.matches("^初中.*")) return "MIDDLE";
        if (n.matches("^[高一二三].*") || n.matches("^高中.*")) return "HIGH";
        // 默认高中
        return "HIGH";
    }

    private boolean isBlank(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 2; i++) if (!str(row, i).isBlank()) return false;
        return true;
    }

    private String str(Row row, int idx) {
        if (row == null) return "";
        Cell c = row.getCell(idx);
        if (c == null) return "";
        return fmt.formatCellValue(c).trim();
    }
}
