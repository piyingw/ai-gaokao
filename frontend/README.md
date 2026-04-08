# 高考志愿填报系统 - 前端

基于 Vue3 + Vite + Nginx 的高考志愿填报系统前端部分。

## 项目结构

```
frontend/                           # 前端项目根目录
├── src/                           # 源代码目录
│   ├── assets/                    # 静态资源
│   ├── components/                # 公共组件
│   ├── views/                     # 页面组件
│   ├── services/                  # API服务
│   ├── router/                    # 路由配置
│   └── App.vue                    # 根组件
├── public/                        # 公共资源
├── package.json                   # 项目配置
├── vite.config.js                 # Vite配置
└── README.md                      # 项目说明
```

## 功能模块

- 首页展示
- 院校查询
- 专业查询
- AI智能助手
- 个人中心
- 用户认证

## 开发环境搭建

### 前提条件

- Node.js >= 16.0.0
- npm 或 yarn
- 后端服务已启动（默认端口 8088）

### 安装依赖

```bash
cd frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

开发服务器将在 `http://localhost:5173` 启动，并自动打开浏览器。

### 构建生产版本

```bash
npm run build
```

构建后的文件将输出到 `../dist` 目录。

## API 接口说明

前端通过代理将 `/api` 请求转发到后端服务：

- 用户服务: `/api/user/*`
- 院校服务: `/api/university/*`
- AI服务: `/api/ai/*`

## Nginx 部署

### 构建项目

```bash
npm run build
```

### Nginx 配置

使用项目根目录下的 `nginx.conf` 文件配置 Nginx：

```nginx
# nginx.conf
events {
    worker_connections 1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    
    # 上游服务器 (后端API)
    upstream backend {
        server localhost:8088;  # 后端Spring Boot应用端口
    }
    
    server {
        listen       80;          
        server_name  localhost;   

        # 前端静态资源
        location / {
            root   dist;          
            index  index.html;
            try_files $uri $uri/ /index.html;  
        }
        
        # API请求代理到后端
        location /api/ {
            proxy_pass http://backend/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

### 部署步骤

1. 构建前端项目: `npm run build`
2. 将 `dist` 目录内容复制到 Nginx 的根目录
3. 更新 Nginx 配置文件
4. 启动后端服务: `cd gaokao-web && mvn spring-boot:run`
5. 启动 Nginx: `start nginx` (Windows) 或 `sudo nginx` (Linux/Mac)
6. 访问 `http://localhost` 查看应用

## 环境变量

开发环境中，API 请求通过 Vite 代理转发到后端。生产环境中，Nginx 将 `/api` 请求代理到后端服务。

## 技术栈

- Vue 3
- Vue Router 4
- Vite 4
- JavaScript (ES6+)
- CSS3
- Axios (API 请求)

## 代码规范

- 使用 ESLint 进行代码检查
- 使用 Prettier 进行代码格式化
- 组件命名采用 PascalCase
- 文件命名采用 kebab-case
- 遵循 Vue 官方风格指南

## 维护说明

此前端项目与后端 API 紧密配合，确保后端服务正常运行后再启动前端服务。