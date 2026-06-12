package com.yunchendun.modules.leave.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模块: 人脸识别请假离校
 * 功能: 学生人脸档案
 */
@Data
@TableName("face_record")
public class FaceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long studentId;
    private String studentNo;
    private String realName;
    private Long classId;
    private String className;
    private String gradeName;
    private String headTeacherName;
    private String facePhotoUrl;
    /** 预留：人脸特征码（硬件同步用） */
    private String faceFeature;
    /** 预留：硬件设备授权标识 */
    private String deviceAuthCode;
    private String status; // ACTIVE / DISABLED
    /** 合规授权摘要（最近一次有效授权，详单见 face_consent_log） */
    private String consentVersion;
    private LocalDateTime consentAt;
    private Long consentBy;
    private String consentByName;
    private String consentRole;
    /** 录入时是否勾选同意（非DB字段，提交载荷用） */
    @TableField(exist = false)
    private Boolean consentAgreed;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableLogic
    private Integer deleted;
}
