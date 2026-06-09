package com.yunchendun.modules.permission.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模块: 数据权限
 * 功能: 教师班级关联（一个教师可关联多个班级）
 */
@Data
@TableName("teacher_class")
public class TeacherClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long teacherUserId;
    private String teacherName;
    private Long classId;
    private String className;
    private Integer isHeadTeacher; // 1=班主任 0=任课
    private String subjectName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableLogic
    private Integer deleted;
}
