package com.yunchendun.modules.academic.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 模块: 教务 edu
 * 功能: 学科
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("edu_subject")
public class EduSubject extends BaseEntity {
    private Long tenantId;
    private String subjectCode;
    private String subjectName;
    private String category;
    private Integer sortOrder;
    private Boolean enabled;
}
