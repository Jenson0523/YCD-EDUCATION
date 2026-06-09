-- 模块: 人脸识别请假离校系统
-- 功能: 人脸库、请假申请、门卫核验、临时补批表
-- 创建: 2026-06
-- 作者: 云辰盾项目组

-- 学生人脸档案
CREATE TABLE face_record (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id   BIGINT NOT NULL DEFAULT 1,
  student_id  BIGINT NOT NULL COMMENT '关联sys_user.id（学生角色）',
  student_no  VARCHAR(64) NOT NULL COMMENT '学籍号（唯一）',
  real_name   VARCHAR(64) NOT NULL COMMENT '学生姓名',
  class_id    BIGINT NULL COMMENT '班级ID',
  class_name  VARCHAR(128) NULL COMMENT '班级名称冗余',
  face_photo_url VARCHAR(500) NULL COMMENT '人脸照片存储路径',
  face_feature   TEXT NULL COMMENT '预留：人脸特征码（硬件同步用）',
  device_auth_code VARCHAR(128) NULL COMMENT '预留：硬件设备授权标识',
  status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by  BIGINT NULL,
  updated_by  BIGINT NULL,
  deleted     TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_face_student_no (student_no),
  KEY idx_face_student (student_id),
  KEY idx_face_class (class_id)
) COMMENT='学生人脸档案';

-- 请假申请（常规 + 临时紧急共用一张表）
CREATE TABLE leave_application (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL DEFAULT 1,
  leave_no        VARCHAR(64) NOT NULL COMMENT '假条编号 YCD-LEAVE-yyyyMMdd-序号',
  student_id      BIGINT NOT NULL COMMENT '学生ID',
  student_no      VARCHAR(64) NOT NULL,
  student_name    VARCHAR(64) NOT NULL,
  class_id        BIGINT NULL,
  class_name      VARCHAR(128) NULL,
  applicant_id    BIGINT NOT NULL COMMENT '申请人ID（家长或教师）',
  applicant_role  VARCHAR(32) NOT NULL COMMENT 'PARENT/TEACHER/GATE',
  leave_type      VARCHAR(16) NOT NULL COMMENT 'SICK/PERSONAL',
  reason          TEXT NOT NULL COMMENT '请假原因',
  leave_start     DATETIME NOT NULL COMMENT '计划离校时间',
  leave_end       DATETIME NOT NULL COMMENT '计划返校时间',
  proof_photo_url VARCHAR(500) NULL COMMENT '凭证照片',
  is_temp         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否临时紧急（先放行后补批）',
  status          VARCHAR(32) NOT NULL DEFAULT 'PENDING'
                  COMMENT 'PENDING待审批/APPROVED已批准/REJECTED已驳回/DEPARTED已离校/RETURNED已返校/TEMP_PENDING临时待补批',
  approve_remark  VARCHAR(500) NULL COMMENT '审批备注/驳回原因',
  approved_by     BIGINT NULL,
  approved_at     DATETIME NULL,
  depart_at       DATETIME NULL COMMENT '实际离校时间（门卫核验时间）',
  return_at       DATETIME NULL COMMENT '实际返校时间',
  temp_deadline   DATETIME NULL COMMENT '临时补批截止时间（depart_at + 24h）',
  warn_sent       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '超时预警是否已发送',
  -- 预留字段（不开发，只占位）
  attendance_ref_id    BIGINT NULL COMMENT '预留：考勤联动ID',
  growth_record_ref_id BIGINT NULL COMMENT '预留：成长档案记录ID',
  safety_ref_id        BIGINT NULL COMMENT '预留：校园安全风控关联ID',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by  BIGINT NULL,
  updated_by  BIGINT NULL,
  deleted     TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_leave_no (leave_no),
  KEY idx_leave_student (student_id),
  KEY idx_leave_status (status),
  KEY idx_leave_class (class_id),
  KEY idx_leave_start (leave_start)
) COMMENT='请假申请（常规+临时）';

-- 门卫核验记录（人脸比对日志）
CREATE TABLE gate_verification (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL DEFAULT 1,
  verify_type     VARCHAR(16) NOT NULL COMMENT 'DEPART离校/RETURN返校',
  student_id      BIGINT NOT NULL,
  student_name    VARCHAR(64) NOT NULL,
  leave_app_id    BIGINT NULL COMMENT '关联假条ID（无假条时为NULL）',
  verifier_id     BIGINT NOT NULL COMMENT '操作门卫用户ID',
  face_match_score DECIMAL(5,2) NULL COMMENT '人脸匹配分（0-100）',
  capture_photo_url VARCHAR(500) NULL COMMENT '核验时抓拍照片',
  result          VARCHAR(20) NOT NULL COMMENT 'PASS通过/NO_LEAVE无假条拦截/FACE_MISMATCH人脸不匹配',
  remark          VARCHAR(500) NULL,
  -- 预留硬件字段
  device_id       VARCHAR(128) NULL COMMENT '预留：硬件设备ID',
  device_channel  VARCHAR(64) NULL COMMENT '预留：设备通道号',
  hardware_mode   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '预留：0=手机端 1=硬件端',
  verified_at     DATETIME NOT NULL COMMENT '核验时间',
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by      BIGINT NULL,
  updated_by      BIGINT NULL,
  deleted         TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_gate_student (student_id),
  KEY idx_gate_date (verified_at),
  KEY idx_gate_result (result)
) COMMENT='门卫核验记录';

-- 临时补批工单（is_temp=1 的假条对应的补批追踪）
CREATE TABLE temp_supplement_order (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL DEFAULT 1,
  leave_app_id    BIGINT NOT NULL COMMENT '关联请假申请ID',
  student_name    VARCHAR(64) NOT NULL,
  class_name      VARCHAR(128) NULL,
  gate_verifier_id BIGINT NOT NULL COMMENT '放行门卫ID',
  depart_at       DATETIME NOT NULL COMMENT '实际离校时间',
  deadline        DATETIME NOT NULL COMMENT '补批截止时间',
  supplement_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/OVERDUE',
  supplemented_by BIGINT NULL,
  supplemented_at DATETIME NULL,
  warn_sent       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已发超时预警',
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by      BIGINT NULL,
  updated_by      BIGINT NULL,
  deleted         TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_supp_leave (leave_app_id),
  KEY idx_supp_deadline (deadline),
  KEY idx_supp_status (supplement_status)
) COMMENT='临时放行补批工单';

-- 系统配置（补批时限、推送开关等）
CREATE TABLE sys_config (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id   BIGINT NOT NULL DEFAULT 1,
  config_key  VARCHAR(128) NOT NULL COMMENT '配置键',
  config_value VARCHAR(500) NOT NULL COMMENT '配置值',
  description VARCHAR(256) NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_config_key (tenant_id, config_key)
) COMMENT='系统参数配置';

-- 初始化默认配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('leave.temp.deadline.hours', '24', '临时请假补批截止时限（小时）'),
('leave.notify.depart.enabled', '1', '离校成功是否通知家长（1=开启）'),
('leave.notify.return.enabled', '1', '返校成功是否通知家长（1=开启）'),
('leave.face.threshold', '80', '人脸比对通过分数线（0-100）');
