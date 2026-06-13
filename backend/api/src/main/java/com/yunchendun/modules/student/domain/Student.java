package com.yunchendun.modules.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 模块: 学生学籍
 * 功能: 学生主档案，全平台学生维度核心主键
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("stu_student")
public class Student extends BaseEntity {
    private String studentNo;
    private String name;
    private String gender;
    private LocalDate birthday;
    private String gradeName;
    private String className;
    private Long classId;
    private Long faceRecordId;
    private String status;
    private String tags;
    private String guardianName;
    private String guardianMobileEncrypted;
    private Integer parentCreateAccount;
    private Long parentUserId;
}
