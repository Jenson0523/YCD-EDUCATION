CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  mobile_encrypted VARCHAR(255) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_user_username (tenant_id, username)
) COMMENT='系统用户';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_role_code (tenant_id, role_code)
) COMMENT='系统角色';

CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY,
  permission_code VARCHAR(128) NOT NULL,
  permission_name VARCHAR(128) NOT NULL,
  module_code VARCHAR(32) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  action VARCHAR(32) NOT NULL,
  UNIQUE KEY uk_sys_permission_code (permission_code)
) COMMENT='资源操作权限';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_sys_user_role (tenant_id, user_id, role_id)
) COMMENT='用户角色关联';

CREATE TABLE sys_role_permission (
  id BIGINT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  UNIQUE KEY uk_sys_role_permission (role_id, permission_id)
) COMMENT='角色权限关联';

CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(64) NOT NULL,
  module_code VARCHAR(32) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NULL,
  action VARCHAR(64) NOT NULL,
  before_data JSON NULL,
  after_data JSON NULL,
  client_ip VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sys_operation_log_module_time (module_code, created_at)
) COMMENT='全量操作日志';

CREATE TABLE stu_student (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  student_no VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  gender VARCHAR(16) NULL,
  birthday DATE NULL,
  grade_name VARCHAR(64) NULL,
  class_name VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  tags JSON NULL,
  guardian_name VARCHAR(64) NULL,
  guardian_mobile_encrypted VARCHAR(255) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_stu_student_no (tenant_id, student_no)
) COMMENT='学生主档案';

CREATE TABLE fs_home_report (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  student_id BIGINT NOT NULL,
  parent_user_id BIGINT NOT NULL,
  report_date DATE NOT NULL,
  sleep_status VARCHAR(64) NULL,
  emotion_status VARCHAR(64) NULL,
  study_status VARCHAR(64) NULL,
  family_special_situation TEXT NULL,
  outgoing_report TEXT NULL,
  follow_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_fs_home_report_student_date (student_id, report_date)
) COMMENT='居家状态报备表';

CREATE TABLE flow_definition (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  flow_code VARCHAR(64) NOT NULL,
  flow_name VARCHAR(128) NOT NULL,
  business_module VARCHAR(32) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flow_definition_code (tenant_id, flow_code)
) COMMENT='审批流定义';

CREATE TABLE flow_node (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  flow_definition_id BIGINT NOT NULL,
  node_name VARCHAR(128) NOT NULL,
  node_order INT NOT NULL,
  approver_role_code VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_flow_node_definition (flow_definition_id)
) COMMENT='审批节点';

CREATE TABLE flow_instance (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  flow_code VARCHAR(64) NOT NULL,
  business_module VARCHAR(32) NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  business_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  applicant_id BIGINT NOT NULL,
  current_node_id BIGINT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_flow_instance_business (business_module, business_type, business_id),
  KEY idx_flow_instance_status (status)
) COMMENT='审批实例';

CREATE TABLE flow_task (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  flow_instance_id BIGINT NOT NULL,
  node_id BIGINT NOT NULL,
  approver_role_code VARCHAR(64) NOT NULL,
  approver_user_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  opinion VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  KEY idx_flow_task_instance (flow_instance_id),
  KEY idx_flow_task_approver (approver_role_code, status)
) COMMENT='审批任务';
