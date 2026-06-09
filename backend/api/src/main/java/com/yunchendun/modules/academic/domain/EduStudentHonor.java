package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模块: 教务 edu
 * 功能: 学生评优
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_student_honor")
public class EduStudentHonor extends BaseEntity {
    private Long tenantId;
    private Long studentId;
    private String semester;
    private String honorType;
    private BigDecimal score;
    private Integer rankInClass;
    private String status;
    private Long evaluatorId;
    private LocalDateTime evaluatedAt;
}
