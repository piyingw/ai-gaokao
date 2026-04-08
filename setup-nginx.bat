@echo off
setlocal

echo 下载并配置Nginx...

REM 检查是否已安装wget或curl
where curl >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo 错误: 未找到curl命令，请确保已安装curl
    exit /b 1
)

REM 创建nginx目录
if not exist "D:\nginx" mkdir D:\nginx

REM 下载nginx (使用GitHub上的Windows版本)
echo 正在下载nginx for Windows...
curl -L -o "D:\nginx\nginx.zip" "https://nginx.org/download/nginx-1.24.0.zip"

if %ERRORLEVEL% neq 0 (
    echo 错误: 下载nginx失败
    exit /b 1
)

REM 解压nginx
powershell -command "Expand-Archive -Force -Path 'D:\nginx\nginx.zip' -DestinationPath 'D:\nginx\'"

REM 重命名解压后的文件夹
for /d %%i in (D:\nginx\nginx-*) do ren "%%i" nginx-installed

REM 移动文件到正确位置
move /Y "D:\nginx\nginx-installed" "D:\nginx\nginx"

REM 复制自定义配置文件
copy /Y "D:\Javacode\ai-gaokao\nginx.conf" "D:\nginx\nginx\conf\nginx.conf"

REM 复制前端构建文件
if exist "D:\nginx\nginx\html" (
    echo 删除默认HTML文件...
    del /Q "D:\nginx\nginx\html\*"
    rmdir /S /Q "D:\nginx\nginx\html"
)

echo 复制前端构建文件...
xcopy /E /I "D:\Javacode\ai-gaokao\dist" "D:\nginx\nginx\html"

REM 启动nginx
echo 启动nginx...
cd /d "D:\nginx\nginx"
start nginx

echo.
echo Nginx已安装并启动！
echo 前端应用可通过 http://localhost 访问
echo 后端API可通过 http://localhost/api 访问
echo.
echo 检查nginx进程:
tasklist | findstr nginx

echo.
echo 部署完成！