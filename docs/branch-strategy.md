# Git 分支策略

## 分支

- `main`：稳定版本，只接收已验收代码。
- `develop`：开发主线，所有功能合并到这里。
- `feature/<module>-<feature>`：单功能分支。
- `hotfix/<scope>`：线上紧急修复。

## 提交信息

格式：

```text
[模块] 类型: 描述
```

模块示例：`SYS`、`FS`、`STU`、`HR`、`FIN`、`FLOW`、`PC`、`MINIAPP`、`DOCS`。

类型示例：`feat`、`fix`、`docs`、`refactor`、`test`、`chore`。

## Tag

每个阶段验收通过后打 Tag：

- `v1.0-phase1`
- `v1.0-phase2`
- `v1.0-phase3`
- `v1.0-phase4`
- `v1.0-phase5`
- `v1.0-phase6`
- `v1.0-phase7`
