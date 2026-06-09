-- 模块: 数据权限 (RBAC 细化)
-- 功能: 教师-班级关联、家长-学生关联、学生班级ID
-- 创建: 2026-06
-- 作者: 云辰盾项目组

-- 学生表补 class_id（与 face_record / leave_application 对齐）
ALTER TABLE stu_student
    ADD COLUMN class_id BIGINT NULL COMMENT '班级ID' AFTER class_name,
    ADD COLUMN face_record_id BIGINT NULL COMMENT '预留：关联人脸档案';

-- 教师-班级关联（一个教师可关联多个班级）
CREATE TABLE teacher_class (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL DEFAULT 1,
  teacher_user_id BIGINT NOT NULL COMMENT '教师用户ID(sys_user.id)',
  teacher_name    VARCHAR(64) NULL COMMENT '教师姓名冗余',
  class_id        BIGINT NOT NULL COMMENT '班级ID',
  class_name      VARCHAR(128) NULL COMMENT '班级名称冗余',
  is_head_teacher TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否班主任(1=班主任,0=任课)',
  subject_name    VARCHAR(64) NULL COMMENT '任教学科(任课老师用)',
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by      BIGINT NULL,
  updated_by      BIGINT NULL,
  deleted         TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_teacher_class (tenant_id, teacher_user_id, class_id),
  KEY idx_tc_teacher (teacher_user_id),
  KEY idx_tc_class (class_id)
) COMMENT='教师班级关联';

-- 家长-学生关联（一个家长可关联多个孩子，支持二孩）
CREATE TABLE parent_student (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL DEFAULT 1,
  parent_user_id  BIGINT NOT NULL COMMENT '家长用户ID(sys_user.id)',
  parent_name     VARCHAR(64) NULL COMMENT '家长姓名冗余',
  student_id      BIGINT NOT NULL COMMENT '学生ID(stu_student.id)',
  student_no      VARCHAR(64) NULL COMMENT '学籍号冗余',
  student_name    VARCHAR(64) NULL COMMENT '学生姓名冗余',
  relation        VARCHAR(16) NOT NULL DEFAULT 'OTHER' COMMENT 'FATHER父亲/MOTHER母亲/OTHER其他监护人',
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by      BIGINT NULL,
  updated_by      BIGINT NULL,
  deleted         TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_parent_student (tenant_id, parent_user_id, student_id),
  KEY idx_ps_parent (parent_user_id),
  KEY idx_ps_student (student_id)
) COMMENT='家长学生关联';

-- 角色数据范围初始化（sys_role.data_scope）
-- ALL=全部 / CLASS=本班 / SELF=本人关联 / GATE_VALID=全校有效假条
INSERT INTO sys_role (id, role_code, role_name, data_scope) VALUES
(1001, 'ADMIN', '系统管理员', 'ALL'),
(1002, 'PRINCIPAL', '校长/股东', 'ALL'),
(1003, 'ACADEMIC', '教务处', 'ALL'),
(1004, 'HR', '人事专员', 'ALL'),
(1005, 'FINANCE', '财务专员', 'ALL'),
(1006, 'HEAD_TEACHER', '班主任', 'CLASS'),
(1007, 'TEACHER', '科任教师', 'CLASS'),
(1008, 'GATE', '门卫', 'GATE_VALID'),
(1009, 'PARENT', '家长', 'SELF'),
(1010, 'STUDENT', '学生', 'SELF')
ON DUPLICATE KEY UPDATE data_scope = VALUES(data_scope), role_name = VALUES(role_name);
