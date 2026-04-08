@echo off
setlocal

echo 高考志愿填报系统部署向导
echo ============================
echo.
echo 本向导将帮助您完成系统的部署
echo.
echo 部署前请确保：
echo 1. 已经下载并解压了Nginx for Windows
echo 2. 后端服务正在运行 (http://localhost:8088)
echo.

:MENU
echo.
echo 请选择操作:
echo 1. 部署到现有Nginx
echo 2. 检查后端服务状态
echo 3. 显示部署说明
echo 4. 退出
echo.
set /p CHOICE=请输入选项 (1-4):

if "%CHOICE%"=="1" goto DEPLOY
if "%CHOICE%"=="2" goto CHECK_BACKEND
if "%CHOICE%"=="3" goto SHOW_INSTRUCTIONS
if "%CHOICE%"=="4" goto EXIT
goto MENU

:DEPLOY
echo.
echo 开始部署到Nginx...
echo.

REM 检查nginx.conf是否存在
if not exist "..\..\nginx.conf" (
    echo 错误: nginx.conf 不存在
    pause
    goto MENU
)

REM 检查dist目录是否存在
if not exist "..\..\dist" (
    echo 错误: dist目录不存在，请先运行npm run build
    pause
    goto MENU
)

set /p NGINX_DIR=请输入Nginx安装目录 (例如: C:\nginx): 

if not exist "%NGINX_DIR%" (
    echo 错误: 目录不存在: %NGINX_DIR%
    pause
    goto MENU
)

if not exist "%NGINX_DIR%\nginx.exe" (
    echo 错误: nginx.exe 不存在于: %NGINX_DIR%
    pause
    goto MENU
)

echo 停止Nginx服务...
"%NGINX_DIR%\nginx.exe" -s quit

timeout /t 3 /nobreak >nul

echo 复制配置文件...
copy /Y "..\..\nginx.conf" "%NGINX_DIR%\conf\nginx.conf"

echo 清空HTML目录...
if exist "%NGINX_DIR%\html" (
    del /Q "%NGINX_DIR%\html\*.*"
    for /d %%x in ("%NGINX_DIR%\html\*") do rmdir /S /Q "%%x"
)

echo 复制前端文件...
xcopy /E /I /Y "..\..\dist" "%NGINX_DIR%\html"

echo 启动Nginx服务...
cd /d "%NGINX_DIR%"
start nginx

echo.
echo 部署完成！
echo.
echo 前端应用可通过 http://localhost 访问
echo 后端API可通过 http://localhost/api 访问
pause
goto MENU

:CHECK_BACKEND
echo.
echo 检查后端服务状态...
curl -s -o nul -w "后端状态: %%{http_code}\n" http://localhost:8088/actuator/health
if %ERRORLEVEL% equ 0 (
    echo 后端服务运行正常
) else (
    echo 后端服务可能未运行，请先启动后端服务
)
pause
goto MENU

:SHOW_INSTRUCTIONS
echo.
echo 部署说明:
echo.
echo 1. 下载Nginx for Windows:
echo    - 访问 https://nginx.org/en/download.html
echo    - 下载Windows版本并解压到目录 (例如: C:\nginx)
echo.
echo 2. 确保后端服务正在运行:
echo    - cd D:\Javacode\ai-gaokao\gaokao-web
echo    - mvn spring-boot:run
echo.
echo 3. 使用此脚本部署:
echo    - 选择选项1
echo    - 输入Nginx安装目录
echo.
echo 4. 访问应用:
echo    - 前端: http://localhost
echo    - API文档: http://localhost/doc.html
echo.
pause
goto MENU

:EXIT
echo.
echo 退出部署向导
exit /b 0