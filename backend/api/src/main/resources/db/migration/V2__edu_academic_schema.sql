-- 模块: 教务 edu
-- 功能: Phase 2 教务核心表：学科、年级班级、排课、成绩、作业、教学进度、评优
-- 创建: 2026-06
-- 作者: 云辰盾项目组

CREATE TABLE edu_subject (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  subject_code VARCHAR(32) NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  category VARCHAR(32) NOT NULL COMMENT '文理综合艺体',
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_edu_subject_code (tenant_id, subject_code)
) COMMENT='学科';

CREATE TABLE edu_grade (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  grade_code VARCHAR(32) NOT NULL,
  grade_name VARCHAR(64) NOT NULL,
  school_section VARCHAR(32) NOT NULL COMMENT 'PRIMARY/JUNIOR/SENIOR/KINDERGARTEN',
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_edu_grade_code (tenant_id, grade_code)
) COMMENT='年级';

CREATE TABLE edu_class (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  grade_id BIGINT NOT NULL,
  class_code VARCHAR(32) NOT NULL,
  class_name VARCHAR(64) NOT NULL,
  head_teacher_id BIGINT NULL COMMENT '班主任用户ID',
  student_count INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_edu_class_grade (grade_id),
  UNIQUE KEY uk_edu_class_code (tenant_id, grade_id, class_code)
) COMMENT='班级';

CREATE TABLE edu_schedule (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  class_id BIGINT NOT NULL,
  subject_id BIGINT NOT NULL,
  teacher_user_id BIGINT NOT NULL,
  week_day TINYINT NOT NULL COMMENT '1-7',
  period_no TINYINT NOT NULL COMMENT '第几节课',
  semester VARCHAR(32) NOT NULL COMMENT '2024-1',
  classroom VARCHAR(64) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_edu_schedule_class (class_id, semester),
  KEY idx_edu_schedule_teacher (teacher_user_id, semester)
) COMMENT='课程表';

CREATE TABLE edu_score (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  student_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  subject_id BIGINT NOT NULL,
  exam_type VARCHAR(32) NOT NULL COMMENT 'MONTHLY/MID/FINAL/QUIZ',
  semester VARCHAR(32) NOT NULL,
  score DECIMAL(6,2) NULL,
  rank_in_class INT NULL,
  rank_in_grade INT NULL,
  remark VARCHAR(255) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_edu_score_student (student_id, semester),
  KEY idx_edu_score_class_subject (class_id, subject_id, exam_type)
) COMMENT='学生成绩';

CREATE TABLE edu_homework (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  class_id BIGINT NOT NULL,
  subject_id BIGINT NOT NULL,
  teacher_user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NULL,
  assigned_date DATE NOT NULL,
  due_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_edu_homework_class (class_id, assigned_date)
) COMMENT='作业布置';

CREATE TABLE edu_homework_submission (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  homework_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUBMITTED/LATE/ABSENT',
  submit_time DATETIME NULL,
  score DECIMAL(5,2) NULL,
  comment VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_edu_hw_submission (homework_id, student_id)
) COMMENT='作业提交记录';

CREATE TABLE edu_teaching_progress (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  class_id BIGINT NOT NULL,
  subject_id BIGINT NOT NULL,
  teacher_user_id BIGINT NOT NULL,
  semester VARCHAR(32) NOT NULL,
  chapter VARCHAR(128) NOT NULL,
  content TEXT NULL,
  teach_date DATE NOT NULL,
  planned_hours INT NOT NULL DEFAULT 1,
  actual_hours INT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_edu_progress_class (class_id, semester)
) COMMENT='教学进度';

CREATE TABLE edu_student_honor (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  student_id BIGINT NOT NULL,
  semester VARCHAR(32) NOT NULL,
  honor_type VARCHAR(64) NOT NULL COMMENT '三好学生/优秀班干部/文明学生',
  score DECIMAL(6,2) NULL COMMENT '综合得分',
  rank_in_class INT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE' COMMENT 'CANDIDATE/APPROVED/REJECTED',
  evaluator_id BIGINT NULL,
  evaluated_at DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_edu_honor_student (student_id, semester)
) COMMENT='学生评优';

CREATE TABLE edu_teacher_honor (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  teacher_user_id BIGINT NOT NULL,
  semester VARCHAR(32) NOT NULL,
  honor_type VARCHAR(64) NOT NULL COMMENT '优秀教师/优秀班主任',
  score DECIMAL(6,2) NULL,
  rank_in_dept INT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE',
  evaluator_id BIGINT NULL,
  evaluated_at DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_edu_teacher_honor (teacher_user_id, semester)
) COMMENT='教师评优';
