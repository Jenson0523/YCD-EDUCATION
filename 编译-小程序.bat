@echo off
chcp 65001 >nul
echo ============================================
echo   云辰盾 - 编译小程序
echo ============================================
echo.
cd /d E:\jenson\claude\YCD-education\frontend\miniapp
call npm run build:mp-weixin
echo.
echo 编译完成！微信开发者工具导入目录：
echo   E:\jenson\claude\YCD-education\frontend\miniapp\dist\build\mp-weixin
echo.
pause
