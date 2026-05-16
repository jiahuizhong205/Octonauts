# Profile 前端集成与 Auth 对接实现说明

> 分支：`feature/profile-view_edit-zjh` | 基于 `develop` (c47c04f)

## 一、功能概要

补全个人资料查看与修改功能的 **前端 Auth 集成链路**，使 pb 已编写的 profile 前后端代码可端到端工作。核心问题：前端 `http.js` 使用 demo 头 `X-User-Id` 透传，但后端 `JwtAuthInterceptor` 要求 `Authorization: Bearer <token>`，两者完全不对接。

## 二、新增文件

```
frontend/src/
├── utils/
│   └── auth.js              # Token 管理工具 (localStorage 存取)
├── api/
│   └── auth.js              # 登录/注册 API 调用
└── views/
    ├── LoginView.vue        # 登录页面
    └── RegisterView.vue     # 注册页面
```

## 三、修改文件

| 文件 | 变更内容 |
|------|----------|
| `frontend/src/api/http.js` | `X-User-Id` 头 → `Authorization: Bearer <token>`，token 不存在时不发送 Auth 头 |
| `frontend/src/main.js` | 新增 `/login` `/register` 路由，添加 `beforeEach` 路由守卫（未登录 → `/login`，已登录访问 auth 页 → `/profile`） |
| `frontend/src/App.vue` | 侧边栏根据 `isLoggedIn()` 动态切换：未登录显示登录/注册，已登录显示功能菜单+退出按钮 |
| `backend/.../security/JwtAuthInterceptor.java` | 跳过 OPTIONS 预检请求（否则 CORS 预检被 JWT 鉴权拦截，浏览器报 Failed to fetch） |

## 四、发现并修复的问题

### 4.1 CORS 预检被 JWT 拦截器拦截（阻塞性 bug）

**现象**：浏览器中登录后进入个人资料页，显示 "Failed to fetch"

**原因**：浏览器发送 `GET /api/v1/users/me` 前先发送 `OPTIONS` 预检（因为有 `Authorization` 头和 `Content-Type` 头），该预检请求不含 token，被 `JwtAuthInterceptor` 拦截并抛出 `BusinessException(UNAUTHORIZED)`，导致预检返回 500，浏览器拒绝发送实际请求。

**修复**：在 `JwtAuthInterceptor.preHandle()` 开头添加 OPTIONS 跳过逻辑：
```java
if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
    return true;
}
```

### 4.2 数据库缺少 profile 字段（阻塞性 bug，已修复）

**现象**：`GET /api/v1/users/me` 返回 500 `BadSqlGrammarException`

**原因**：`mysql-init/init.sql` 已定义 `nickname`/`college`/`major`/`grade`/`bio`/`contact_visible` 字段，但 Docker 中的实际数据库是用旧版 `init.sql` 初始化的，缺少这些列。

**修复**：执行以下 DDL 补全字段：
```sql
ALTER TABLE sys_user
  ADD COLUMN nickname VARCHAR(64) DEFAULT NULL AFTER username,
  ADD COLUMN college VARCHAR(64) DEFAULT NULL AFTER campus,
  ADD COLUMN major VARCHAR(64) DEFAULT NULL AFTER college,
  ADD COLUMN grade VARCHAR(32) DEFAULT NULL AFTER major,
  ADD COLUMN bio VARCHAR(255) DEFAULT NULL AFTER grade,
  ADD COLUMN contact_visible TINYINT(1) DEFAULT 0 AFTER bio;
```

### 4.3 biz_notification 表缺失（已修复）

**现象**：「消息通知」页面报"服务端异常"

**原因**：`mysql-init/init.sql` 中定义了 `biz_notification` 表，但实际数据库中未创建。

**修复**：手动建表：
```sql
CREATE TABLE biz_notification (
  notification_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(500) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  read_status TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  KEY idx_user_read_created (user_id, read_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 4.4 Maven 打包瘦 JAR（阻塞性 bug，已修复）

**现象**：重启后端时报 `没有主要清单属性`，JAR 只有 70KB。

**原因**：`mvn package -DskipTests` 使用了 Maven 缓存，未执行 `spring-boot:repackage`，产出的 JAR 缺少 Main-Class。

**修复**：使用 `mvn clean package -DskipTests`，清理缓存后正确打包为 41MB 的可执行 fat JAR。

### 4.5 Windows Git Bash 中文编码问题（非阻塞）

测试时 curl 传中文 JSON 会因编码问题触发 `HttpMessageNotReadableException`。前端浏览器使用 UTF-8，不受影响。如需命令行测试中文，使用 ASCII payload 替代。

## 五、架构流程

```
浏览器 → http://localhost:5173
           │
           ├── 未登录
           │   └── /login (登录页) ←→ POST /api/v1/auth/login
           │   └── /register (注册页) ←→ POST /api/v1/auth/register
           │       成功后将 token + userId 存入 localStorage
           │
           └── 已登录
               └── /profile (资料页) ←→ GET/PUT /api/v1/users/me
               └── /requirements (需求大厅)
               └── /notifications (消息通知)
               └── 退出 → 清空 localStorage，跳转 /login

每次 fetch 请求都会从 localStorage 读取 token，附加到 Authorization 头
JwtAuthInterceptor 拦截 /api/** (排除 auth/health)，验证 token 并设置 request.userId
ProfileController 通过 CurrentUser.requireUserId(request) 获取当前用户ID
```

## 六、启动方式

### 6.1 启动依赖服务

```bash
cd ~/Desktop/Octonauts
docker compose up -d
```

### 6.2 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/campushub-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

后端运行在 `http://localhost:8080`，健康检查：
```bash
curl http://localhost:8080/api/v1/health
# → {"code":200,"message":"CampusHub backend is running","data":{"status":"UP"}}
```

### 6.3 启动前端

```bash
cd frontend
npm install    # 首次或依赖变更时执行
npx vite --host
```

前端运行在 `http://localhost:5173/`，浏览器直接打开即可。

## 七、测试用例

以下用例全部通过验证（在分支 `feature/profile-view_edit-zjh` 上测试）。

### 7.1 用户注册

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123456","campus":"west"}'
```

**期望**：`{"code":200,"message":"注册成功"}`

### 7.2 用户登录

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123456"}'
```

**期望**：返回 `code:200`，`data.userId` 为雪花ID，`data.token` 为 JWT 字符串。

### 7.3 获取个人资料（需鉴权）

```bash
TOKEN="<从登录接口获取>"
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

**期望**：返回 `code:200`，`data` 包含 `userId`、`username`、`nickname`、`studentId`、`campus`、`college`、`major`、`grade`、`bio`、`contactVisible`、`creditScore`。

### 7.4 修改个人资料（需鉴权）

```bash
TOKEN="<从登录接口获取>"
curl -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"my-nick","campus":"north","college":"CS","major":"SE","grade":"3","bio":"hello","contactVisible":true}'
```

**期望**：`{"code":200,"message":"个人资料更新成功"}`，`data` 返回更新后的完整资料。

### 7.5 修改持久化验证

```bash
# 接上一步，再次 GET 确认数据已持久化
TOKEN="<同一token>"
curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"
```

**期望**：返回的 `nickname`、`campus` 等字段值与上次 PUT 一致。

### 7.6 未认证拒绝

```bash
curl http://localhost:8080/api/v1/users/me
```

**期望**：`{"code":401,"message":"未认证或凭证失效"}`

### 7.7 越权保护（只能改自己）

两个用户分别登录，用户 A 调用 `/api/v1/users/me` 只能获取和修改自己的资料，无法通过修改请求体来操作其他用户的资料。`ProfileController` 中 `userId` 始终从 `CurrentUser.requireUserId(request)` 获取，不从请求体读取。

### 7.8 前端流程测试（浏览器）

1. 打开 `http://localhost:5173/` → 自动重定向到 `/login`
2. 点击「去注册」→ 填写用户名和密码 → 点击注册 → 提示成功跳回登录页
3. 输入账号密码 → 点击登录 → 跳转到 `/profile` 个人资料页
4. 修改昵称/校区/学院等字段 → 点击「保存」→ 页面提示"个人资料已更新"
5. 刷新页面 → 修改后的资料保持不变
6. 点击侧边栏「退出」→ 跳回登录页 → 手动输入 `/profile` → 被路由守卫重定向回 `/login`

## 八、关键设计点

- **后端接口不在此分支修改**，Profile 前后端代码（pb 编写）仅通过前端 Auth 层对接
- Profile 鉴权模型：「只能改自己」由 `ProfileController` 保证 —— `userId` 始终从 JWT token 提取，不接受请求体中的 userId
- 前端路由守卫 `beforeEach`：未登录访问需认证页面 → `/login`；已登录访问 auth 页面 → `/profile`
- 所有 API 错误前端统一在 `http.js` 的 `request()` 中捕获，`code !== 200` 时抛 Error，业务层 try/catch 展示 `message` 字段
