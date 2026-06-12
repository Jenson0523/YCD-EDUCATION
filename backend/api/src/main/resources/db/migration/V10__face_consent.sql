-- 模块: 人脸识别请假离校
-- 功能: 人脸信息采集合规（PIPL）——授权同意记录 + 审计日志
-- 创建: 2026-06

-- face_record 增加授权摘要字段（最近一次有效授权）
ALTER TABLE face_record ADD COLUMN consent_version VARCHAR(32) NULL COMMENT '同意书版本号';
ALTER TABLE face_record ADD COLUMN consent_at DATETIME NULL COMMENT '授权同意时间';
ALTER TABLE face_record ADD COLUMN consent_by BIGINT NULL COMMENT '授权操作人用户ID';
ALTER TABLE face_record ADD COLUMN consent_by_name VARCHAR(64) NULL COMMENT '授权操作人姓名';
ALTER TABLE face_record ADD COLUMN consent_role VARCHAR(32) NULL COMMENT '授权操作人角色(PARENT/TEACHER/ADMIN等)';

-- 授权同意审计日志（每次录入/更新人脸都留痕，合规追溯用）
CREATE TABLE face_consent_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    student_id      BIGINT       NULL COMMENT '学生ID',
    student_no      VARCHAR(64)  NULL COMMENT '学籍号',
    student_name    VARCHAR(64)  NULL COMMENT '学生姓名',
    consent_version VARCHAR(32)  NOT NULL COMMENT '同意书版本号',
    agree_user_id   BIGINT       NOT NULL COMMENT '同意操作人用户ID',
    agree_user_name VARCHAR(64)  NULL COMMENT '同意操作人姓名',
    agree_role      VARCHAR(32)  NULL COMMENT '操作人角色',
    action          VARCHAR(32)  NOT NULL DEFAULT 'ENROLL' COMMENT 'ENROLL=录入 UPDATE=更新照片 REVOKE=撤回',
    remark          VARCHAR(255) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fcl_student (student_no),
    INDEX idx_fcl_user (agree_user_id)
) COMMENT '人脸采集授权同意审计日志';
