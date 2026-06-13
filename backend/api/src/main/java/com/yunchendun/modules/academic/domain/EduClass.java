package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 模块: 教务 edu
 * 功能: 班级
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_class")
public class EduClass extends BaseEntity {
    private Long tenantId;
    private Long gradeId;
    private String classCode;
    private String className;
    private Long headTeacherId;
    private String headTeacherName;
    private Integer studentCount;
}
