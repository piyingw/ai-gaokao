# build-and-deploy.bat
@echo off
setlocal

echo 正在构建前端项目...
cd /d D:\Javacode\ai-gaokao\frontend

REM 安装依赖
call npm install

REM 构建生产版本
call npm run build

if %ERRORLEVEL% NEQ 0 (
    echo 构建失败
    exit /b %ERRORLEVEL%
)

echo 前端构建完成，文件位于 D:\Javacode\ai-gaokao\dist 目录

echo.
echo 项目构建和部署准备就绪
echo 1. 启动后端服务: cd D:\Javacode\ai-gaokao\gaokao-web && mvn spring-boot:run
echo 2. 启动Nginx: 在nginx目录下执行 start nginx
echo 3. 访问 http://localhost 查看应用