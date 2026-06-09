package com.yunchendun.modules.familyschool.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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
    private Long studentId;
    private Long parentUserId;
    private LocalDate reportDate;
    private String sleepStatus;
    private String emotionStatus;
    private String studyStatus;
    private String familySpecialSituation;
    private String outgoingReport;
    private String followStatus;
}
