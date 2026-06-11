# 虹软 ArcSoft 离线人脸引擎接入指南

> 目的：把门卫刷脸核验从"图像过渡引擎"升级为**真正的人脸识别**，做到精准、离线、免调用费。
> 现状：未安装 SDK 时系统自动使用"图像相似度过渡引擎"（已能拒绝无关随机照片，但同一人不同角度可能不稳）。
> 装好 ArcSoft 后，系统自动切换为虹软引擎，准确率达到生产级。

---

## 一、注册并下载 SDK（约 10 分钟）

1. 打开虹软开放平台：https://ai.arcsoft.com.cn/
2. 注册账号并实名认证（个人即可，免费）。
3. 进入「我的应用」→「创建应用」，平台选 **Windows / Linux**（与你服务器一致）。
4. 添加 SDK 能力：选 **人脸识别 ArcFace** → 版本 **V3.0**（含人脸检测+特征比对，离线免费）。
5. 创建后会得到两个关键值（后面要填到配置）：
   - **APP_ID**
   - **SDK_KEY**（按平台/系统区分，注意选对 Windows x64 或 Linux x64）
6. 在该应用下「下载 SDK」，得到压缩包，解压后大致结构：
   ```
   ArcSoft_ArcFace_Windows_x64_V3.0/
   ├── libs/
   │   ├── arcsoft_face.dll
   │   ├── arcsoft_face_engine.dll
   │   ├── arcsoft_face_engine_jni.dll
   │   └── ...（其它依赖dll）
   ├── jar/
   │   └── arcsoft-sdk-face-3.0.0.0.jar
   └── doc/
   ```

---

## 二、放置文件到项目（约 5 分钟）

1. **SDK jar** → 复制到（文件名必须叫 `arcsoft-sdk-face.jar`）：
   ```
   backend/api/libs/arcsoft-sdk-face.jar
   ```
   > Maven 会自动检测到此文件并激活 `arcsoft` profile，编译 ArcSoftFaceEngine。

2. **native 库（DLL/SO）** → 复制到：
   ```
   backend/api/libs/arcsoft/        ← 把 libs/ 里所有 .dll（Linux为.so）放这里
   ```

最终目录：
```
backend/api/libs/
├── arcsoft-sdk-face.jar
└── arcsoft/
    ├── arcsoft_face.dll
    ├── arcsoft_face_engine.dll
    ├── arcsoft_face_engine_jni.dll
    └── ...
```

---

## 三、配置凭证（约 2 分钟）

方式 A（推荐，环境变量）：启动后端前设置
```powershell
$env:ARCSOFT_APP_ID  = "你的APP_ID"
$env:ARCSOFT_SDK_KEY = "你的SDK_KEY"
# lib-path 默认 ./libs/arcsoft，一般不用改
```

方式 B：直接写进 `backend/api/src/main/resources/application.yml`
```yaml
face:
  api:
    provider: arcsoft          # 已默认是 arcsoft
    arcsoft:
      app-id: 你的APP_ID
      sdk-key: 你的SDK_KEY
      lib-path: ./libs/arcsoft
```

---

## 四、重启后端并验证

```powershell
# 在 backend/api 目录
mvn spring-boot:run
```

启动日志看到这行即成功：
```
[ArcSoft] 离线人脸引擎已就绪 ✓ (libPath=./libs/arcsoft)
```

接口验证当前引擎：
```
GET http://localhost:8081/api/leave/face/engine-info
→ {"engine":"arcsoft(虹软离线)","ready":true,"threshold":80.0}
```

若 `engine` 仍是 `image-hash(过渡引擎)`，按日志排查：
- `未配置 app-id/sdk-key` → 凭证没读到
- `激活失败 code=xxx` → APP_ID/SDK_KEY 不匹配或与系统平台不符
- `引擎加载异常` → DLL 没放对目录 / 缺依赖dll / 32位与64位不符

---

## 五、给已有学生重新生成人脸特征（重要）

接入真实引擎后，建议让学生人脸照片走一遍真实特征：
- 门卫端刷脸识别 / 1:1 核验时，系统会用 ArcSoft 实时对"档案照片 vs 抓拍照片"做检测+比对，**无需预先离线注册**，开箱即用。
- 要求：每位学生在「PC后台 → 人脸档案管理」里上传的照片必须是**清晰单人正脸**，否则 ArcSoft 检测不到人脸会判为不匹配。

---

## 常见问题
- **必须联网吗？** 仅首次 `activeOnline` 激活需联网一次，之后纯离线运行。
- **收费吗？** ArcFace 离线 SDK 免费，无调用次数限制。
- **阈值怎么调？** `face.api.threshold`（默认80）。越高越严格。校园安全场景建议 80~82。
- **Linux 服务器？** 下载 Linux x64 版 SDK，把 `.so` 放 `libs/arcsoft/`，jar 同名放 `libs/`。
