@echo off
chcp 65001 >nul
echo ============================================
echo   云辰盾 - 启动 PC 管理后台 (端口 5173)
echo ============================================
echo.
echo 启动后浏览器访问: http://localhost:5173
echo.
cd /d E:\jenson\claude\YCD-education\frontend\pc-admin
call npm run dev
pause
