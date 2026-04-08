# 高考志愿填报系统 - 测试说明

## 前后端集成测试步骤

### 1. 准备工作

确保以下服务已安装并运行：
- JDK 17+ (用于后端)
- Maven 3.6+
- Node.js 16+ (用于前端)
- MySQL 8.0+
- Redis 7.0+
- Nginx

### 2. 后端服务启动

```bash
# 1. 初始化数据库
mysql -u root -p < gaokao-web/src/main/resources/db/init.sql

# 2. 启动后端服务
cd gaokao-web
mvn spring-boot:run
```

后端服务将在 `http://localhost:8088` 启动。

### 3. 前端开发环境测试

```bash
# 1. 安装前端依赖
cd frontend
npm install

# 2. 启动前端开发服务器
npm run dev
```

前端开发服务器将在 `http://localhost:5173` 启动，并通过代理访问后端API。

### 4. 生产环境测试

#### 自动化构建部署（推荐）

```bash
# 运行构建部署脚本
./build-and-deploy.bat
```

#### 手动部署

```bash
# 1. 构建前端项目
cd frontend
npm run build

# 2. 配置 Nginx
# 将 nginx.conf 复制到 Nginx 安装目录
# 修改配置中的路径为实际路径

# 3. 启动后端服务
cd ../gaokao-web
mvn spring-boot:run

# 4. 启动 Nginx
# Windows: start nginx
# Linux/Mac: sudo nginx
```

### 5. 功能测试清单

- [ ] 首页正常显示
- [ ] 院校查询页面能够加载数据
- [ ] 专业查询页面能够加载数据
- [ ] AI助手页面能够与后端通信
- [ ] 用户登录/注册功能正常
- [ ] 个人中心页面能够获取用户信息
- [ ] API请求正确代理到后端
- [ ] 所有页面在不同屏幕尺寸下正常显示

### 6. 常见问题排查

#### 问题1: API请求失败
- 检查后端服务是否运行在 `http://localhost:8088`
- 检查 Nginx 配置中的代理设置

#### 问题2: 静态资源加载失败
- 检查 Nginx 配置中的 `root` 路径是否指向正确的构建目录
- 确认构建产物存在于 `dist` 目录

#### 问题3: Vue Router History模式问题
- 确保 Nginx 配置中有 `try_files $uri $uri/ /index.html;` 配置
- 这样可以确保直接访问路由路径时不会返回404

### 7. 性能优化建议

- 启用 Nginx Gzip 压缩
- 使用 CDN 托管静态资源
- 实施缓存策略
- 优化图片和其他媒体资源大小