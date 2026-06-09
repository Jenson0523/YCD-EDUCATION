# API 约定

## URL

```text
/api/{module}/{resource}
```

示例：

- `/api/fs/home-reports`
- `/api/stu/students`
- `/api/flow/instances`
- `/api/dc/student-growth/{studentId}`

## 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 分页参数

- `pageNo`：从 1 开始。
- `pageSize`：默认 20，最大 200。

## 错误码

- `0`：成功
- `400`：参数错误
- `401`：未登录
- `403`：无权限
- `404`：资源不存在
- `409`：业务状态冲突
- `500`：系统错误
