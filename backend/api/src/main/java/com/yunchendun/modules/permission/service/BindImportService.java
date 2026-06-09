package com.yunchendun.modules.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.modules.academic.domain.EduClass;
import com.yunchendun.modules.academic.mapper.EduClassMapper;
import com.yunchendun.modules.permission.domain.ParentStudent;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.ParentStudentMapper;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块: 数据权限
 * 功能: 教师班级 / 家长学生 绑定的 Excel 模板下载与批量导入
 */
@Service
@RequiredArgsConstructor
public class BindImportService {

    private final SysUserMapper userMapper;
    private final EduClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final ParentStudentMapper parentStudentMapper;

    private static final String[] TC_HEADERS =
            {"教师用户名", "班级名称", "是否班主任(是/否)", "任教学科(选填)"};
    private static final String[] PS_HEADERS =
            {"家长用户名", "学生学籍号", "关系(父亲/母亲/其他)"};

    // ==================== 模板下载 ====================

    public void downloadTeacherClassTemplate(HttpServletResponse resp) {
        List<String[]> samples = new ArrayList<>();
        samples.add(new String[]{"teacher01", "高一(1)班", "是", "语文"});
        samples.add(new String[]{"teacher02", "高一(1)班", "否", "数学"});
        writeTemplate(resp, "教师班级绑定模板", TC_HEADERS, samples);
    }

    public void downloadParentStudentTemplate(HttpServletResponse resp) {
        List<String[]> samples = new ArrayList<>();
        // 演示多孩：同一家长两行，对应两个孩子
        samples.add(new String[]{"parent01", "2024001", "父亲"});
        samples.add(new String[]{"parent01", "2024002", "父亲"});
        writeTemplate(resp, "家长学生绑定模板", PS_HEADERS, samples);
    }

    private void writeTemplate(HttpServletResponse resp, String fileName,
                               String[] headers, List<String[]> samples) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("绑定数据");
            // 表头样式
            CellStyle headStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headStyle.setFont(font);
            headStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 5000);
            }
            // 示例行
            int r = 1;
            for (String[] sample : samples) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < sample.length; i++) {
                    row.createCell(i).setCellValue(sample[i]);
                }
            }

            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            resp.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encoded + ".xlsx\"; filename*=UTF-8''" + encoded + ".xlsx");
            OutputStream os = resp.getOutputStream();
            wb.write(os);
            os.flush();
        } catch (Exception e) {
            throw new RuntimeException("模板生成失败: " + e.getMessage(), e);
        }
    }

    // ==================== 导入 ====================

    public Map<String, Object> importTeacherClass(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row)) continue;
                String username = str(row, 0);
                String className = str(row, 1);
                String headFlag = str(row, 2);
                String subject = str(row, 3);
                int rowNo = r + 1;

                if (username.isBlank() || className.isBlank()) {
                    errors.add("第" + rowNo + "行：用户名或班级名称为空"); continue;
                }
                SysUser teacher = findUser(username);
                if (teacher == null) { errors.add("第" + rowNo + "行：找不到教师账号 " + username); continue; }
                EduClass clazz = findClass(className);
                if (clazz == null) { errors.add("第" + rowNo + "行：找不到班级 " + className); continue; }

                // 去重
                Long exist = teacherClassMapper.selectCount(new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherUserId, teacher.getId())
                        .eq(TeacherClass::getClassId, clazz.getId()));
                if (exist != null && exist > 0) { errors.add("第" + rowNo + "行：已存在绑定，跳过"); continue; }

                TeacherClass tc = new TeacherClass();
                tc.setTenantId(1L);
                tc.setTeacherUserId(teacher.getId());
                tc.setTeacherName(teacher.getRealName());
                tc.setClassId(clazz.getId());
                tc.setClassName(clazz.getClassName());
                tc.setIsHeadTeacher("是".equals(headFlag.trim()) ? 1 : 0);
                tc.setSubjectName(subject);
                teacherClassMapper.insert(tc);
                success++;
            }
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }
        return result(success, errors);
    }

    public Map<String, Object> importParentStudent(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row)) continue;
                String username = str(row, 0);
                String studentNo = str(row, 1);
                String relation = str(row, 2);
                int rowNo = r + 1;

                if (username.isBlank() || studentNo.isBlank()) {
                    errors.add("第" + rowNo + "行：用户名或学籍号为空"); continue;
                }
                SysUser parent = findUser(username);
                if (parent == null) { errors.add("第" + rowNo + "行：找不到家长账号 " + username); continue; }
                Student student = findStudent(studentNo);
                if (student == null) { errors.add("第" + rowNo + "行：找不到学籍号 " + studentNo); continue; }

                Long exist = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                        .eq(ParentStudent::getParentUserId, parent.getId())
                        .eq(ParentStudent::getStudentId, student.getId()));
                if (exist != null && exist > 0) { errors.add("第" + rowNo + "行：已存在绑定，跳过"); continue; }

                ParentStudent ps = new ParentStudent();
                ps.setTenantId(1L);
                ps.setParentUserId(parent.getId());
                ps.setParentName(parent.getRealName());
                ps.setStudentId(student.getId());
                ps.setStudentNo(student.getStudentNo());
                ps.setStudentName(student.getName());
                ps.setRelation(mapRelation(relation));
                parentStudentMapper.insert(ps);
                success++;
            }
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }
        return result(success, errors);
    }

    // ==================== 工具 ====================

    private SysUser findUser(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim()).last("LIMIT 1"));
    }

    private EduClass findClass(String className) {
        return classMapper.selectOne(new LambdaQueryWrapper<EduClass>()
                .eq(EduClass::getClassName, className.trim()).last("LIMIT 1"));
    }

    private Student findStudent(String studentNo) {
        return studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo.trim()).last("LIMIT 1"));
    }

    private String mapRelation(String s) {
        if (s == null) return "OTHER";
        s = s.trim();
        if (s.contains("父")) return "FATHER";
        if (s.contains("母")) return "MOTHER";
        return "OTHER";
    }

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 3; i++) {
            if (!str(row, i).isBlank()) return false;
        }
        return true;
    }

    private final DataFormatter dataFormatter = new DataFormatter();

    private String str(Row row, int idx) {
        if (row == null) return "";
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return dataFormatter.formatCellValue(cell).trim();
    }

    private Map<String, Object> result(int success, List<String> errors) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("failed", errors.size());
        m.put("errors", errors);
        return m;
    }
}
