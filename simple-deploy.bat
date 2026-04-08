@echo off
setlocal

echo 高考志愿填报系统部署向导
echo ============================

echo.
echo 1. 检查后端服务状态
curl -s -o nul -w "后端状态: %%{http_code}\n" http://localhost:8088/actuator/health
echo.

echo 2. 请确保您已下载并安装了Nginx for Windows
echo    如果还没有，请:
echo    - 访问 https://nginx.org/en/download.html
echo    - 下载Windows版本并解压到某个目录 (例如: C:\nginx)
echo.

echo 3. 部署步骤:
echo    a. 停止当前运行的Nginx (如果有)
echo       C:\nginx\nginx.exe -s quit
echo    b. 复制配置文件
echo       copy nginx.conf C:\nginx\conf\nginx.conf
echo    c. 复制前端文件
echo       xcopy /E /I dist C:\nginx\html
echo    d. 启动Nginx
echo       cd C:\nginx && start nginx
echo.

echo 4. 访问应用:
echo    - 前端: http://localhost
echo    - API文档: http://localhost/doc.html
echo.

echo 完成部署后请按任意键退出...
pause >nul