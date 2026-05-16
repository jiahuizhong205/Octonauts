# Auth 模块实现说明

> 分支：`feature/auth-login-zjh` | 基于 `develop` (812067b)

## 一、功能概要

实现注册、登录、JWT 签发与鉴权拦截，密码 BCrypt 加盐哈希存储，统一 `code/message/data` 响应。

## 二、新增文件

```
backend/src/main/java/com/campushub/
├── auth/
│   ├── AuthController.java          # POST /api/v1/auth/register, /login
│   ├── AuthService.java             # 注册/登录业务逻辑
│   ├── MeController.java            # GET /api/v1/me (鉴权测试用)
│   └── dto/
│       ├── RegisterRequest.java     # 注册请求 (username/password/studentId/campus)
│       ├── LoginRequest.java        # 登录请求 (username/password)
│       └── LoginResponse.java       # 登录响应 (userId/token)
├── security/
│   ├── JwtUtil.java                 # JWT 签发与校验 (HS384, 24h)
│   └── JwtAuthInterceptor.java      # HandlerInterceptor, 校验 Bearer token
└── config/
    ├── SecurityConfig.java          # BCryptPasswordEncoder Bean
    ├── WebConfig.java               # 注册 JWT 拦截器, 排除 /api/v1/auth/**, /api/v1/health
    └── MyMetaObjectHandler.java     # MyBatis-Plus 自动填充 createdAt/updatedAt
```

## 三、修改文件

| 文件 | 变更 |
|------|------|
| `backend/pom.xml` | +jjwt 0.12.6, +spring-security-crypto, +mybatis-plus-jsqlparser; MyBatis-Plus 3.5.7→3.5.9; 强制 mybatis-spring 3.0.4 |
| `CampusHubApplication.java` | +`@MapperScan("com.campushub.mapper")` |
| `common/exception/ApiCode.java` | +USERNAME_TAKEN(4003), USER_NOT_FOUND(4004), PASSWORD_ERROR(4005) |
| `common/exception/GlobalExceptionHandler.java` | 校验异常返回具体字段错误信息 |
| `security/CurrentUser.java` | 改为从 `request.getAttribute("userId")` 读取，由拦截器注入 |
| `application.yml` | `campushub.auth.user-header` → `campushub.jwt.secret` + `campushub.jwt.expiration-ms` |
| `notification/*/Service.java` | import 路径修正 (`common.BusinessException` → `common.exception.BusinessException`) |
| `profile/*/Service.java` | 同上 |
| `requirement/*/Service.java` | 同上 |
| `notification/*/Controller.java` | import 路径修正 (`common.ApiResponse` → `common.api.ApiResponse`) |
| `profile/*/Controller.java` | 同上 |
| `requirement/*/Controller.java` | 同上 |

**已删除旧文件：**
- `common/ApiResponse.java`（旧版，新版在 `common/api/` 下）
- `common/BusinessException.java`（旧版，新版在 `common/exception/` 下）
- `common/GlobalExceptionHandler.java`（旧版，新版在 `common/exception/` 下）

## 四、测试路径

> **以下命令均为 Windows cmd 格式**，可直接复制一行粘贴回车执行。

### 前置条件

```cmd
:: 1. 启动 Docker 基础设施
docker-compose up -d

:: 2. 构建并启动后端（会弹出新窗口）
cd backend
mvn package -DskipTests
start java -jar target/campushub-backend-0.0.1-SNAPSHOT.jar
```

### 测试用例

#### 1. 注册

```cmd
:: 正常注册 → 期望: {"code":200,"message":"注册成功"}
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"password\":\"test123456\"}"

:: 重复用户名 → 期望: {"code":4003,"message":"用户名已存在"}
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"password\":\"another123\"}"

:: 密码过短 → 期望: {"code":400,"message":"密码长度需在6-100位之间"}
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"abc\",\"password\":\"12\"}"
```

#### 2. 登录

```cmd
:: 正常登录 → 期望: {"code":200,"message":"登录成功","data":{"userId":...,"token":"eyJ..."}}
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"password\":\"test123456\"}"

:: 密码错误 → 期望: {"code":4005,"message":"密码错误"}
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"password\":\"wrongpwd\"}"

:: 用户不存在 → 期望: {"code":4004,"message":"用户不存在"}
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"nobody\",\"password\":\"123456\"}"
```

#### 3. JWT 鉴权

先执行上一步"正常登录"，从返回结果中复制 `token` 字段的值，替换下面 `<你的token>`：

```cmd
:: 无 token 访问受保护接口 → 期望: {"code":401,"message":"未认证或凭证失效"}
curl -s http://localhost:8080/api/v1/me

:: 带有效 token → 期望: {"code":200,"message":"success","data":{"userId":...}}
curl -s http://localhost:8080/api/v1/me -H "Authorization: Bearer <你的token>"

:: 带无效 token → 期望: {"code":401,"message":"未认证或凭证失效"}
curl -s http://localhost:8080/api/v1/me -H "Authorization: Bearer garbage123"

:: 健康检查无需 token → 期望: {"code":200,"message":"CampusHub backend is running",...}
curl -s http://localhost:8080/api/v1/health
```

#### 4. 密码哈希验证

```cmd
docker exec campushub-mysql mysql -u ch_dev -pch_password campushub_db -e "SELECT username, LEFT(password_hash, 20) AS pwd_prefix FROM sys_user WHERE username='testuser';"
:: 期望: password_hash 以 $2a$10$ 开头 (BCrypt)
```

## 五、架构说明

```
请求 → JwtAuthInterceptor.preHandle()
       ├── 路径匹配 /api/** (排除 /api/v1/auth/**, /api/v1/health)
       ├── 提取 Authorization: Bearer <token>
       ├── JwtUtil.validateToken() 校验签名+过期
       ├── JwtUtil.parseUserId() 解析 subject
       └── request.setAttribute("userId", userId)
              ↓
       Controller → CurrentUser.requireUserId(request)
              ↓
       Service 层执行业务逻辑
```

## 六、实现过程中发现的问题

### 1. MyBatis-Plus 与 Spring Boot 3.3.5 兼容性

**现象：** 启动报 `Invalid value type for attribute 'factoryBeanObjectType': java.lang.String`

**原因：** MyBatis-Plus 3.5.7 boot-starter 传递依赖了 `mybatis-spring:2.1.2`（面向 Spring Boot 2.x），而 Spring Boot 3.3.5 / Spring 6.1.14 的 `AbstractAutowireCapableBeanFactory.getTypeForFactoryBean()` 不兼容旧版 MapperFactoryBean。

**修复：** 排除传递依赖，显式声明 `mybatis-spring:3.0.4`。

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.9</version>
    <exclusions>
        <exclusion>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-spring</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>3.0.4</version>
</dependency>
```

### 2. PaginationInnerInterceptor 类找不到

**现象：** `mvn compile` 报 `PaginationInnerInterceptor cannot be resolved to a type`

**原因：** MyBatis-Plus 3.5.8+ 将分页拦截器从 `mybatis-plus-extension` 移到了独立包 `mybatis-plus-jsqlparser`。pom.xml 中未声明该依赖。

**修复：** 新增依赖。

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
```

### 3. 旧版 common 类与新包冲突

**现象：** 启动报 `ConflictingBeanDefinitionException: 'globalExceptionHandler' conflicts with existing ...`

**原因：** `develop` 合并后同时存在两套公共类：
- 旧：`common/ApiResponse.java`、`common/BusinessException.java`、`common/GlobalExceptionHandler.java`
- 新：`common/api/ApiResponse.java`、`common/exception/BusinessException.java`、`common/exception/GlobalExceptionHandler.java`

Spring 组件扫描发现同名 Bean 且不兼容。

**修复：** 删除旧版三个文件，将 pb 的 6 个引用文件（notification/profile/requirement 的 Controller 和 Service）import 路径全部改为新版。

### 4. @MapperScan 缺失

**现象：** 启动报 `No qualifying bean of type 'SysUserMapper' available`

**原因：** `CampusHubApplication` 未配置 `@MapperScan`，MyBatis 默认扫描范围不包含 `com.campushub.mapper` 包。

**修复：** 在主类添加 `@MapperScan("com.campushub.mapper")`。

### 5. sys_user 表 NOT NULL 字段在注册时未提供值

**现象：** 注册接口返回 `{"code":500,"message":"服务端异常"}`（数据库报列不可为 NULL）

**原因：** `sys_user` 表的 `phone_encrypted` 和 `student_id` 列有 NOT NULL 约束且无默认值，但 `RegisterRequest` 未收集这两个字段。

**修复：** 在 `AuthService.register()` 中补充默认值：
```java
user.setStudentId(request.getStudentId() != null ? request.getStudentId() : "U_" + request.getUsername());
user.setPhoneEncrypted("");
```

### 6. created_at / updated_at 未自动填充

**现象：** INSERT 语句中 `created_at` 和 `updated_at` 参数为 `null`，依赖 DB 默认值。

**原因：** `SysUser` 实体标注了 `@TableField(fill = FieldFill.INSERT)` 但项目缺少 `MetaObjectHandler` 实现。

**修复：** 新增 `MyMetaObjectHandler`，实现 `insertFill` 和 `updateFill`。

### 7. application.yml 未打包进 jar

**现象：** `java -jar` 启动后报 "no profiles are currently active"，无法连接数据源。

**原因：** `mvn compile` 后直接 `mvn spring-boot:repackage` 不会自动复制资源文件到 jar。

**修复：** 使用 `mvn package -DskipTests` 走完整生命周期，确保 `process-resources` 阶段被执行。

### 8. biz_notification 表不存在（非本次引入）

**现象：** pb 的 notification/profile 业务接口返回 500。

**原因：** `biz_notification` 表在 pb 分支的 init.sql 中定义，但未在当前 Docker MySQL 中执行创建，导致 pb 的 `NotificationService` 查询失败。

**建议：** 团队统一维护一份 `mysql-init/init.sql`，新表变更需同步更新并重新初始化。待 pb 分支合入 develop 后执行完整的 init.sql。
