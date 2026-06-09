package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 模块: 教务 edu
 * 功能: 教学进度
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_teaching_progress")
public class EduTeachingProgress extends BaseEntity {
    private Long tenantId;
    private Long classId;
    private Long subjectId;
    private Long teacherUserId;
    private String semester;
    private String chapter;
    private String content;
    private LocalDate teachDate;
    private Integer plannedHours;
    private Integer actualHours;
}
