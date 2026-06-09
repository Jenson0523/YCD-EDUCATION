# 数据库设计约定

## 表前缀

- 系统：`sys_`
- 审批流：`flow_`
- 家校：`fs_`
- 学籍：`stu_`
- 教务：`edu_`
- 人事：`hr_`
- 财务：`fin_`
- 保险：`ins_`
- 后勤：`logi_`
- 心理安全：`psy_`
- 招生升学：`enr_`
- 数据中台：`dc_`

## 通用字段

业务表默认包含：

- `id`
- `tenant_id`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- `deleted`

## 主键策略

使用雪花 ID 或数据库 BIGINT 主键。学生主档案 `stu_student.id` 是全平台学生维度聚合主键。
