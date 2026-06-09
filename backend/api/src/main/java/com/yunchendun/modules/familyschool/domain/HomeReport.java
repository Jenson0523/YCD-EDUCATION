package com.yunchendun.modules.familyschool.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 模块: 家校共同体
 * 功能: 居家状态报备
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("fs_home_report")
public class HomeReport extends BaseEntity {
    private Long tenantId;
    private Long studentId;
    private Long parentUserId;
    private LocalDate reportDate;
    private String sleepStatus;
    private String emotionStatus;
    private String studyStatus;
    private String familySpecialSituation;
    private String outgoingReport;
    private String followStatus;
    private String followRemark;
    private Long followBy;
    private LocalDateTime followAt;
}
