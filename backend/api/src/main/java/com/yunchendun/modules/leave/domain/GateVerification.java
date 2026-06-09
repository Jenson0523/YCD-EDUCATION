package com.yunchendun.modules.leave.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模块: 人脸识别请假离校
 * 功能: 门卫核验记录（人脸比对日志）
 */
@Data
@TableName("gate_verification")
public class GateVerification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String verifyType;  // DEPART / RETURN
    private Long studentId;
    private String studentName;
    private Long leaveAppId;
    private Long verifierId;
    private BigDecimal faceMatchScore;
    private String capturePhotoUrl;
    /** PASS=通过 / NO_LEAVE=无假条拦截 / FACE_MISMATCH=人脸不匹配 */
    private String result;
    private String remark;
    // 预留硬件字段
    private String deviceId;
    private String deviceChannel;
    private Integer hardwareMode; // 0=手机端 1=硬件端
    private LocalDateTime verifiedAt;
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
