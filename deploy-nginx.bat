@echo off
setlocal

echo 正在配置Nginx...

REM 检查是否提供了nginx.conf路径
if "%~1"=="" (
    echo 错误: 请提供nginx.conf的路径
    echo 用法: %0 ^<nginx_conf_path^>
    exit /b 1
)

set NGINX_CONF_PATH=%~1

REM 检查nginx.conf是否存在
if not exist "%NGINX_CONF_PATH%" (
    echo 错误: nginx.conf 不存在于 %NGINX_CONF_PATH%
    exit /b 1
)

REM 停止nginx（如果正在运行）
echo 停止nginx服务...
nginx -s quit

REM 等待几秒确保nginx已停止
timeout /t 3 /nobreak >nul

REM 复制新的配置文件
echo 复制nginx配置文件...
copy /Y "D:\Javacode\ai-gaokao\nginx.conf" "%NGINX_CONF_PATH%"

REM 启动nginx
echo 启动nginx服务...
start nginx

echo.
echo Nginx配置完成！
echo 前端应用可通过 http://localhost 访问
echo 后端API可通过 http://localhost/api 访问
echo.
echo 检查nginx进程:
tasklist | findstr nginx

echo.
echo 部署完成！