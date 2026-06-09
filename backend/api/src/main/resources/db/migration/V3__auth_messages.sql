-- 模块: 系统 / 家校
-- 功能: 登录鉴权完善、家校跟进字段、站内消息表
-- 创建: 2026-06
-- 作者: 云辰盾项目组

-- 补全 sys_user 字段（已有表，用 ALTER 追加缺失列）
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS role_code VARCHAR(64) NOT NULL DEFAULT 'TEACHER' COMMENT '快速角色标识',
    ADD COLUMN IF NOT EXISTS avatar VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS class_id BIGINT NULL COMMENT '班主任/教师关联班级';

-- 家校报备补充跟进字段
ALTER TABLE fs_home_report
    ADD COLUMN IF NOT EXISTS follow_remark VARCHAR(500) NULL COMMENT '跟进备注',
    ADD COLUMN IF NOT EXISTS follow_by BIGINT NULL COMMENT '跟进人ID',
    ADD COLUMN IF NOT EXISTS follow_at DATETIME NULL COMMENT '跟进时间';

-- 站内消息表
CREATE TABLE IF NOT EXISTS sys_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  receiver_id BIGINT NOT NULL COMMENT '接收人用户ID',
  sender_id BIGINT NULL COMMENT '发送人用户ID，NULL表示系统',
  title VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  biz_type VARCHAR(64) NOT NULL DEFAULT 'NOTICE' COMMENT 'NOTICE/HOME_REPORT_REPLY/APPROVAL/HONOR',
  biz_id BIGINT NULL COMMENT '关联业务ID',
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sys_message_receiver (receiver_id, is_read),
  KEY idx_sys_message_created (created_at)
) COMMENT='站内消息';
