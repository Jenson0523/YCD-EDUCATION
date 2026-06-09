package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 模块: 教务 edu
 * 功能: 课程表
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_schedule")
public class EduSchedule extends BaseEntity {
    private Long tenantId;
    private Long classId;
    private Long subjectId;
    private Long teacherUserId;
    private Integer weekDay;
    private Integer periodNo;
    private String semester;
    private String classroom;
}
