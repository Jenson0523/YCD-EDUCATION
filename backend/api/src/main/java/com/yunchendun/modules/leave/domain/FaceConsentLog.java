package com.yunchendun.modules.leave.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模块: 人脸识别请假离校
 * 功能: 人脸采集授权同意审计日志（PIPL合规留痕，只增不改）
 */
@Data
@TableName("face_consent_log")
public class FaceConsentLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String consentVersion;
    private Long agreeUserId;
    private String agreeUserName;
    private String agreeRole;
    /** ENROLL=录入 UPDATE=更新照片 REVOKE=撤回授权 */
    private String action;
    private String remark;
    private LocalDateTime createdAt;
}
