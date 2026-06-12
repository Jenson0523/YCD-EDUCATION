# ═══════════════════════════════════════════════════════════
# 云辰盾一键打包脚本（在本机 Windows 运行）
# 产物输出到 deploy\out\：
#   ycd-api.jar      后端可执行包
#   pc-admin\        PC后台静态文件（上传到服务器 /data/ycd/pc-admin）
# 用法：powershell -ExecutionPolicy Bypass -File deploy\build-all.ps1
# ═══════════════════════════════════════════════════════════
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$out  = Join-Path $PSScriptRoot "out"
New-Item -ItemType Directory -Force $out | Out-Null

Write-Host "═══ 1/3 打包后端 jar ═══" -ForegroundColor Cyan
Set-Location (Join-Path $root "backend\api")
# -DskipTests 跳过测试；arcsoft profile 会因本机 libs/arcsoft-sdk-face.jar 自动激活，
# ArcSoftFaceEngine 一并编进 jar（服务器只需放 Linux .so + jar 即可用）
mvn -q clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "后端打包失败" }
$jar = Get-ChildItem target\*.jar | Where-Object { $_.Name -notlike "*original*" } | Select-Object -First 1
Copy-Item $jar.FullName (Join-Path $out "ycd-api.jar") -Force
Write-Host "  → $($jar.Name) ✓"

Write-Host "═══ 2/3 打包 PC 后台 ═══" -ForegroundColor Cyan
Set-Location (Join-Path $root "frontend\pc-admin")
npm run build
if ($LASTEXITCODE -ne 0) { throw "PC后台打包失败" }
if (Test-Path (Join-Path $out "pc-admin")) { Remove-Item (Join-Path $out "pc-admin") -Recurse -Force }
Copy-Item dist (Join-Path $out "pc-admin") -Recurse
Write-Host "  → pc-admin/dist ✓"

Write-Host "═══ 3/3 复制部署配置 ═══" -ForegroundColor Cyan
Copy-Item (Join-Path $PSScriptRoot "application-prod.example.yml") $out -Force
Copy-Item (Join-Path $PSScriptRoot "nginx.conf") $out -Force
Copy-Item (Join-Path $PSScriptRoot "ycd-common.inc") $out -Force
Copy-Item (Join-Path $PSScriptRoot "ycd-api.service") $out -Force

Write-Host ""
Write-Host "打包完成 → $out" -ForegroundColor Green
Write-Host "下一步：把 out\ 上传服务器，按 deploy\DEPLOY.md 操作"
Write-Host "提醒：小程序发布前需改 request.js 的生产域名（见 DEPLOY.md 第六节）" -ForegroundColor Yellow
