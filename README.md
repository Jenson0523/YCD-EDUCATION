# 云辰盾 · 家校共育共同体综合管理平台

面向幼儿园、小学、初中、高中全学段民办学校，建设学校、教师、学生、家长四方协同的综合管理平台。平台以家校双向互通为核心，覆盖教务、人事薪资、学籍成长档案、收费财务、保险、后勤、心理安全、招生升学、数据驾驶舱与绩效考核。

## 技术栈

| 层级 | 技术选型 |
| --- | --- |
| 后端接口服务 | Java 17 + Spring Boot 3 + MyBatis-Plus + Sa-Token |
| 数据中台 | Spring Boot 独立服务，统一聚合指标与画像 |
| 数据库 | MySQL 8 + Redis |
| 数据库迁移 | Flyway |
| 接口文档 | Swagger / Knife4j |
| PC 管理后台 | Vue 3 + Vite + Element Plus + Pinia |
| 微信小程序 | uni-app + Vue 3 |

## 项目结构

```text
YCD-education/
├─ backend/
│  ├─ api/                 # Spring Boot 接口服务，按 10 大板块分包
│  └─ data-center/         # 统一数据中台，负责聚合指标、画像、排名、预警
├─ frontend/
│  ├─ pc-admin/            # PC 管理后台
│  └─ miniapp/             # 教师、家长、学生小程序
├─ modules/                # 10 大业务板块说明与边界
├─ shared/                 # 跨端常量、设计 Token、通用工具
├─ templates/              # 标准化业务模板
├─ docs/                   # 架构、规范、接口、安全、验收文档
└─ README.md
```

## 十大业务板块

| # | 板块 | 目录 | 表前缀 | API 前缀 |
| --- | --- | --- | --- | --- |
| 1 | 家校共同体·双向互通中心 | `family-school` | `fs_` | `/api/fs` |
| 2 | 教务处·全学段分科教学管理 | `academic` | `edu_` | `/api/edu` |
| 3 | 教职工人事 + 薪资薪酬流程管理 | `hr` | `hr_` | `/api/hr` |
| 4 | 学生学籍 + 全生命周期电子成长档案 | `student` | `stu_` | `/api/stu` |
| 5 | 收费管理 + 全域财务管控 | `finance` | `fin_` | `/api/fin` |
| 6 | 学平险 + 综合保险 + 重大事故联动 | `insurance` | `ins_` | `/api/ins` |
| 7 | 后勤 + 校园综合服务管理 | `logistics` | `logi_` | `/api/logi` |
| 8 | 心理健康 + 全域安全风控 | `psychology-safety` | `psy_` | `/api/psy` |
| 9 | 招生宣传 + 升学选科指导 | `enrollment` | `enr_` | `/api/enr` |
| 10 | 大数据驾驶舱 + 绩效考核 | `dashboard` | `dash_` | `/api/dash` |

系统级表使用 `sys_`，审批流使用 `flow_`，数据中台使用 `dc_`。

## 当前开发阶段

当前落地 Phase 1：基础底座。

- 统一目录结构、Git 规范、文档规范
- RBAC 角色权限模型与操作日志基础表
- 统一审批流引擎基础表与接口样板
- 学生主档案基础模型
- 家校互通样板接口与页面
- PC 后台、小程序、数据中台工程骨架

## 启动说明

后端需要 Java 17、Maven、MySQL 8、Redis。PC 管理后台和小程序需要 Node.js。

详细说明见：

- [开发规范](docs/dev-spec.md)
- [架构设计](docs/architecture.md)
- [分支策略](docs/branch-strategy.md)
- [阶段验收](docs/phase-acceptance.md)
- [安全与隐私](docs/security.md)
