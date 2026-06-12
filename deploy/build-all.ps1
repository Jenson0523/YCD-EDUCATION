# ============================================================
# YCD one-click package script (run on local Windows)
# Output to deploy\out\:
#   ycd-api.jar      backend executable
#   pc-admin\        PC admin static files (upload to /data/ycd/pc-admin)
# Usage: powershell -ExecutionPolicy Bypass -File deploy\build-all.ps1
# Note: messages kept ASCII-only so PowerShell 5.1 (GBK) parses correctly.
# ============================================================
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$out  = Join-Path $PSScriptRoot "out"
New-Item -ItemType Directory -Force $out | Out-Null

Write-Host "=== 1/3 Package backend jar ===" -ForegroundColor Cyan
Set-Location (Join-Path $root "backend\api")
# arcsoft profile auto-activates via libs/arcsoft-sdk-face.jar; ArcSoftFaceEngine
# is bundled into the jar (server only needs Linux .so + this jar).
mvn -q clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "backend package failed" }
$jar = Get-ChildItem target\*.jar | Where-Object { $_.Name -notlike "*original*" } | Select-Object -First 1
Copy-Item $jar.FullName (Join-Path $out "ycd-api.jar") -Force
Write-Host ("  -> " + $jar.Name + " OK")

Write-Host "=== 2/3 Package PC admin ===" -ForegroundColor Cyan
Set-Location (Join-Path $root "frontend\pc-admin")
npm run build
if ($LASTEXITCODE -ne 0) { throw "pc-admin build failed" }
if (Test-Path (Join-Path $out "pc-admin")) { Remove-Item (Join-Path $out "pc-admin") -Recurse -Force }
Copy-Item dist (Join-Path $out "pc-admin") -Recurse
Write-Host "  -> pc-admin/dist OK"

Write-Host "=== 3/3 Copy deploy configs ===" -ForegroundColor Cyan
Copy-Item (Join-Path $PSScriptRoot "application-prod.example.yml") $out -Force
Copy-Item (Join-Path $PSScriptRoot "nginx.conf") $out -Force
Copy-Item (Join-Path $PSScriptRoot "ycd-common.inc") $out -Force
Copy-Item (Join-Path $PSScriptRoot "ycd-api.service") $out -Force

Write-Host ""
Write-Host ("DONE -> " + $out) -ForegroundColor Green
Write-Host "Next: upload out\ to server, follow deploy\DEPLOY.md"
Write-Host "Reminder: set PROD_MODE/PROD_API in miniapp request.js before release (DEPLOY.md section 6)" -ForegroundColor Yellow
