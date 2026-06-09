package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 模块: 教务 edu
 * 功能: 学生成绩
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_score")
public class EduScore extends BaseEntity {
    private Long tenantId;
    private Long studentId;
    private Long classId;
    private Long subjectId;
    private String examType;
    private String semester;
    private BigDecimal score;
    private Integer rankInClass;
    private Integer rankInGrade;
    private String remark;
}
