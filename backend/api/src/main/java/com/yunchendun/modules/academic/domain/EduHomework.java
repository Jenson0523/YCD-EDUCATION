package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 模块: 教务 edu
 * 功能: 作业布置
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_homework")
public class EduHomework extends BaseEntity {
    private Long tenantId;
    private Long classId;
    private Long subjectId;
    private Long teacherUserId;
    private String title;
    private String content;
    private LocalDate assignedDate;
    private LocalDate dueDate;
    private String status;
}
