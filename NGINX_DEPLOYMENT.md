# Nginx 部署说明

## 部署步骤

### 1. 确保后端服务正在运行
```bash
# 启动后端服务
cd D:\Javacode\ai-gaokao\gaokao-web
mvn spring-boot:run
```

后端服务将在 `http://localhost:8088` 启动。

### 2. 部署前端到 Nginx

#### 手动部署
1. 将 `D:\Javacode\ai-gaokao\dist` 目录下的所有文件复制到 Nginx 的 HTML 目录下
2. 将 `D:\Javacode\ai-gaokao\nginx.conf` 文件复制到 Nginx 安装目录下
3. 重启 Nginx 服务

#### 自动部署
运行部署脚本：
```bash
# 假设你的nginx.conf位于C:\nginx\conf\nginx.conf
deploy-nginx.bat "C:\nginx\conf\nginx.conf"
```

### 3. 启动 Nginx
```bash
# 启动 Nginx
nginx

# 或者重新加载配置
nginx -s reload
```

## 验证部署

1. 访问 `http://localhost` - 应该能看到前端页面
2. 访问 `http://localhost/api/user/info` - 应该能收到后端API响应（需要认证）

## 故障排除

### 如果无法访问前端页面
1. 检查 Nginx 是否正在运行
   ```bash
   tasklist | findstr nginx
   ```
   
2. 检查 Nginx 配置是否正确
   ```bash
   nginx -t
   ```

3. 检查防火墙设置是否允许 80 端口

### 如果 API 请求失败
1. 确认后端服务是否在 `http://localhost:8088` 运行
2. 检查 Nginx 配置中的代理设置

## 停止服务

### 停止 Nginx
```bash
nginx -s quit
```

### 停止后端服务
在后端服务运行的终端窗口按 Ctrl+C，或使用：
```bash
taskkill /F /IM java.exe
```

## Nginx 配置说明

当前的 nginx.conf 配置：
- 将前端静态资源托管在根路径 /
- 将 /api 路径的请求代理到后端服务 http://localhost:8088
- 支持 Vue Router 的 History 模式
- 启用了 Gzip 压缩