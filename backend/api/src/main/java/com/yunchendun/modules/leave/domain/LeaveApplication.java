package com.yunchendun.modules.leave.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模块: 人脸识别请假离校
 * 功能: 请假申请（常规 + 临时紧急共用）
 */
@Data
@TableName("leave_application")
public class LeaveApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String leaveNo;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long classId;
    private String className;
    private Long applicantId;
    private String applicantRole; // PARENT / TEACHER / GATE
    private String leaveType;    // SICK / PERSONAL
    private String reason;
    private LocalDateTime leaveStart;
    private LocalDateTime leaveEnd;
    private String proofPhotoUrl;
    /** 暂存人脸照片URL（提交请假时传入，持久化到face_record表，不存入本表） */
    @TableField(exist = false)
    private String facePhotoUrl;
    /** 申请人标签（如"家长张大大 申请"、"语文老师张英语 代申请"、"门卫王大爷 临时放行"），非DB字段 */
    @TableField(exist = false)
    private String applicantLabel;
    private Integer isTemp;      // 0=常规 1=临时紧急
    /**
     * 状态：
     * PENDING=待审批, APPROVED=已批准, REJECTED=已驳回,
     * DEPARTED=已离校, RETURNED=已返校, TEMP_PENDING=临时待补批
     */
    private String status;
    private String approveRemark;
    private String approveSignatureUrl; // 审批人签字图片
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime departAt;
    private LocalDateTime returnAt;
    private LocalDateTime tempDeadline;
    private Integer warnSent;
    // 预留字段
    private Long attendanceRefId;
    private Long growthRecordRefId;
    private Long safetyRefId;
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
