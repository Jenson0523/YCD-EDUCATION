package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 模块: 教务 edu
 * 功能: 年级
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_grade")
public class EduGrade extends BaseEntity {
    private Long tenantId;
    private String gradeCode;
    private String gradeName;
    private String schoolSection;
    private Integer sortOrder;
}
