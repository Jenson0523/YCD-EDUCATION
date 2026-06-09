package com.yunchendun.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.modules.academic.domain.EduClass;
import com.yunchendun.modules.academic.mapper.EduClassMapper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
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
 * 模块: 学生学籍
 * 功能: 学籍 Excel 模板下载与批量导入
 */
@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final StudentMapper studentMapper;
    private final EduClassMapper classMapper;
    private final DataFormatter fmt = new DataFormatter();

    private static final String[] HEADERS =
            {"学籍号", "姓名", "性别(男/女)", "年级", "班级名称", "状态(在读/休学)"};

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
                sheet.setColumnWidth(i, 4500);
            }
            String[][] samples = {
                    {"2024010", "陈明", "男", "高一", "高一(1)班", "在读"},
                    {"2024011", "刘静", "女", "高一", "高一(1)班", "在读"},
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
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isBlank(row)) continue;
                int rowNo = r + 1;
                String studentNo = str(row, 0);
                String name = str(row, 1);
                String gender = str(row, 2);
                String grade = str(row, 3);
                String className = str(row, 4);
                String statusText = str(row, 5);

                if (studentNo.isBlank() || name.isBlank()) {
                    errors.add("第" + rowNo + "行：学籍号或姓名为空"); continue;
                }
                // 学籍号唯一
                Long dup = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, studentNo));
                if (dup != null && dup > 0) { errors.add("第" + rowNo + "行：学籍号 " + studentNo + " 已存在，跳过"); continue; }

                Student s = new Student();
                s.setTenantId(1L);
                s.setStudentNo(studentNo);
                s.setName(name);
                s.setGender(gender);
                s.setGradeName(grade);
                s.setClassName(className);
                s.setStatus("休学".equals(statusText) ? "SUSPENDED" : "ACTIVE");
                // 按班级名称匹配 class_id（便于班主任数据权限）
                if (!className.isBlank()) {
                    EduClass clazz = classMapper.selectOne(new LambdaQueryWrapper<EduClass>()
                            .eq(EduClass::getClassName, className).last("LIMIT 1"));
                    if (clazz != null) s.setClassId(clazz.getId());
                }
                studentMapper.insert(s);
                success++;
            }
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("failed", errors.size());
        m.put("errors", errors);
        return m;
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
