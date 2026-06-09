# backend/api

Spring Boot 接口服务，负责业务写入、权限校验、审批流、操作日志和模块接口。

## 模块包约定

```text
com.yunchendun
├─ common          # 通用返回体、枚举、基础实体、异常
├─ system          # 用户、角色、权限、操作日志
├─ workflow        # 统一审批流引擎
├─ modules
│  ├─ familyschool # /api/fs
│  ├─ student      # /api/stu
│  ├─ academic     # /api/edu
│  ├─ hr           # /api/hr
│  ├─ finance      # /api/fin
│  ├─ insurance    # /api/ins
│  ├─ logistics    # /api/logi
│  ├─ psychology   # /api/psy
│  ├─ enrollment   # /api/enr
│  └─ dashboard    # /api/dash
```

## 启动前置

- Java 17
- Maven 3.9+
- MySQL 8
- Redis

复制 `src/main/resources/application.yml`，按本机数据库账号调整配置。
