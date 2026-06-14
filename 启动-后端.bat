@echo off
chcp 65001 >nul
echo ============================================
echo   云辰盾 - 启动后端 API (端口 8081)
echo ============================================
echo.
echo 提示：需先确保 MySQL80 服务已启动
echo.
cd /d E:\jenson\claude\YCD-education\backend\api
call mvn spring-boot:run
pause
