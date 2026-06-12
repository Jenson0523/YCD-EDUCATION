# 云辰盾 云服务器部署指南

> 目标架构（单台云服务器即可，推荐 2核4G 起）：
> ```
> 用户/微信小程序/PC浏览器
>         │ https
>         ▼
> Nginx (80/443) ──► /api/**  ──► Spring Boot :8081 ──► MySQL 8
>         │                            │
>         └─► PC后台静态文件            ├─► /data/ycd/uploads (人脸/凭证，鉴权访问)
>                                      └─► /data/ycd/arcsoft (虹软 Linux .so)
> ```
> 适用系统：Ubuntu 22.04 / Debian 12 / CentOS 兼容发行版（命令以 Ubuntu 为例）。

---

## 〇、准备清单（开始前确认）

| 项 | 说明 |
|---|---|
| 云服务器 | 2核4G+，开放 22/80/443 端口（安全组） |
| 域名 | 已解析到服务器IP；**小程序正式发布要求域名已ICP备案** |
| SSL证书 | 云厂商免费证书或 certbot（小程序要求 https） |
| 虹软 Linux SDK | ai.arcsoft.com.cn 同一应用下，下载 **Linux x64 V3.0**（.so 与 Windows .dll 不通用；SDK_KEY 也是 Linux 专属的一个） |

---

## 一、本机打包（Windows）

```powershell
powershell -ExecutionPolicy Bypass -File deploy\build-all.ps1
```
产物在 `deploy\out\`：`ycd-api.jar`、`pc-admin/`、各配置模板。

---

## 二、服务器初始化（一次性）

```bash
# 1. 装 JDK21 + MySQL8 + Nginx
sudo apt update
sudo apt install -y openjdk-21-jre-headless mysql-server nginx

# 2. 创建运行用户与目录
sudo useradd -r -m -s /usr/sbin/nologin ycd
sudo mkdir -p /opt/ycd /data/ycd/uploads /data/ycd/arcsoft /data/ycd/pc-admin
sudo chown -R ycd:ycd /opt/ycd /data/ycd

# 3. 建库与账号（进入 mysql）
sudo mysql
```
```sql
CREATE DATABASE ycd_education DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ycd'@'localhost' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON ycd_education.* TO 'ycd'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```
> 表结构无需手工导入——后端启动时 Flyway 自动执行 V1~V10 全部迁移。

---

## 三、上传部署文件

把 `deploy/out/` 内容上传（scp / WinSCP / 宝塔均可）：

```bash
# 后端
scp deploy/out/ycd-api.jar  root@服务器IP:/opt/ycd/
scp deploy/out/application-prod.example.yml root@服务器IP:/opt/ycd/application-prod.yml  # 上传时改名
# 原命令: scp deploy/out/application-prod.yml root@服务器IP:/opt/ycd/
# PC后台
scp -r deploy/out/pc-admin/* root@服务器IP:/data/ycd/pc-admin/
# 虹软 Linux SDK 的 .so（从 Linux 版SDK压缩包 libs/ 取）
scp libarcsoft_face*.so root@服务器IP:/data/ycd/arcsoft/
```

服务器上编辑 `/opt/ycd/application-prod.yml`，改所有 ★ 标注：
- MySQL 密码
- 虹软 **Linux 版** APP_ID / SDK_KEY
- 路径如已按本文默认则不用改

```bash
sudo chown -R ycd:ycd /opt/ycd /data/ycd
sudo chmod 600 /opt/ycd/application-prod.yml   # 含密码，收紧权限
```

---

## 四、注册 systemd 服务并启动

```bash
sudo cp ycd-api.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now ycd-api

# 看日志确认两件事：Flyway 迁到 v10、[ArcSoft] 离线人脸引擎已就绪 ✓
sudo journalctl -u ycd-api -f
```
> 虹软首次激活需服务器能访问公网一次，之后纯离线。激活后生成的
> `ArcFace64.dat` 在工作目录 /opt/ycd，绑定本机，勿拷贝到其他机器。

本机自测：
```bash
curl -s -X POST http://127.0.0.1:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}'
```

---

## 五、配置 Nginx + HTTPS

```bash
sudo cp ycd-common.inc /etc/nginx/conf.d/
sudo cp nginx.conf /etc/nginx/conf.d/ycd.conf
sudo vim /etc/nginx/conf.d/ycd.conf     # 改 server_name 与证书路径
sudo nginx -t && sudo systemctl reload nginx
```

证书两种来源任选：
- **云厂商免费证书**：下载 Nginx 格式 pem/key，放 `/etc/nginx/ssl/`；
- **certbot**：`sudo apt install certbot python3-certbot-nginx && sudo certbot --nginx -d 你的域名`。

验证：浏览器打开 `https://你的域名` 应见 PC 登录页；
`https://你的域名/api/auth/login` POST 可登录。

---

## 六、小程序切到生产域名（发布前必做）

1. 改 `frontend/miniapp/src/api/request.js`：`resolveBaseUrl()` 里把真机分支
   返回值改为 `https://你的域名/api`（保留 devtools 分支便于本地调试）。
2. 重新编译：`npm run build:mp-weixin`，微信开发者工具上传体验版/发布。
3. **微信公众平台 → 开发 → 开发设置 → 服务器域名**：
   - request合法域名：`https://你的域名`
   - uploadFile合法域名：`https://你的域名`
   - downloadFile合法域名：`https://你的域名`
   > 域名必须 https 且已ICP备案，每月可修改5次。

---

## 七、安全与备份（上线必读）

| 项 | 操作 |
|---|---|
| 改默认密码 | 上线后立刻改 admin 密码，并重置全部测试账号 |
| 防火墙 | 安全组只开 22/80/443；**8081、3306 不对公网开放** |
| 数据库备份 | `crontab -e` 加：`0 3 * * * mysqldump -uycd -p'密码' ycd_education | gzip > /data/backup/ycd_$(date +\%F).sql.gz` |
| uploads备份 | 人脸/凭证在 /data/ycd/uploads，纳入快照或 rsync |
| 人脸合规 | /uploads 无公开访问（已默认）；同意书留痕在 face_consent_log 表 |
| HTTPS | 证书到期前续期（certbot 自动；云证书1年需手动换） |

---

## 八、日常运维速查

```bash
sudo systemctl restart ycd-api      # 重启后端
sudo journalctl -u ycd-api -n 200   # 看最近日志
sudo systemctl reload nginx         # 重载Nginx
# 升级发版：替换 /opt/ycd/ycd-api.jar 后 restart 即可（Flyway 自动迁移新表）
```

## 常见问题
- **启动报 Communications link failure** → MySQL 未启动或密码错。
- **[ArcSoft] 激活失败** → SDK_KEY 用成了 Windows 版；Linux 版要单独在虹软后台获取。
- **[ArcSoft] 引擎加载异常** → .so 未放 /data/ycd/arcsoft 或缺依赖：`ldd libarcsoft_face_engine_jni.so` 检查。
- **小程序请求失败** → 域名未在微信后台白名单 / 证书链不完整（用 `https://myssl.com` 检测）。
- **图片不显示** → 正常现象是必须带 token：前端 assetUrl/resolveUrl 已自动拼接，若自写代码请走 `/api/files/preview?path=..&token=..`。
