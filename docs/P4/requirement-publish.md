# 需求发布接口与发布页面实现说明

> 分支：`feature/requirement-publish-zjh` | 基于 `develop` (79a5f45)

## 一、功能概要

实现需求发布 API (`POST /api/v1/requirements`) 与前端发布页面，支持标题、描述、预算、分类字段校验。已认证用户可发布需求，发布成功后生成 Snowflake reqId 并跳转至需求详情页。

## 二、新增文件

```
backend/src/main/java/com/campushub/
└── requirement/dto/
    └── CreateRequirementRequest.java   # 发布请求 DTO (title/description/budget/type + Jakarta Validation)

frontend/src/
└── views/
    └── PublishRequirementView.vue      # 发布需求页面 (表单 + 客户端校验 + 发布逻辑)
```

## 三、修改文件

| 文件 | 变更内容 |
|------|----------|
| `backend/.../requirement/dto/CreateRequirementRequest.java` | **新增** 发布请求 DTO，含 Jakarta Validation 注解 |
| `backend/.../requirement/RequirementController.java` | 新增 `POST /api/v1/requirements`，注入 `CurrentUser`，`@Valid` 触发校验；`GET /{reqId}` 路径加 `\\d+` 正则约束防止非数字匹配 |
| `backend/.../requirement/RequirementService.java` | 注入 `BizRequirementMapper`；新增 `createRequirement()`：枚举校验 → MyBatis-Plus insert → 返回 reqId |
| `backend/.../config/JacksonConfig.java` | **新增** 全局将 `Long`/`long` 序列化为字符串，防止 JavaScript 精度丢失 |
| `backend/.../exception/GlobalExceptionHandler.java` | 新增 `NoResourceFoundException` → 404 处理；通用异常处理增加 `log.error()` 打印堆栈 |
| `frontend/src/api/requirements.js` | 新增 `createRequirement(data)` 函数 |
| `frontend/src/views/PublishRequirementView.vue` | **新增** 发布需求表单页 |
| `frontend/src/main.js` | **（实际入口文件）** 新增 `PublishRequirementView` import，添加 `/requirements/publish` 路由（在 `:reqId` 之前），含 `requiresAuth` 鉴权守卫 |
| `frontend/src/router/index.ts` | 补全所有缺失路由（注：此文件未被实际加载，实际入口为 main.js） |
| `frontend/src/App.vue` | 侧边栏已登录状态下新增「发布需求」导航链接 |
| `frontend/src/styles.css` | 新增 `.message.error` / `.message.success` 样式变体 |
| `docs/P4/requirement-publish.md` | 本文档 |

## 四、校验规则

### 4.1 后端校验 (Jakarta Validation + 服务层)

| 字段 | 规则 | 违规时返回 |
|------|------|-----------|
| title | `@NotBlank` + `@Size(min=5, max=50)` | `400` 对应 message |
| description | `@NotBlank` + `@Size(max=500)` | `400` 对应 message |
| budget | `@NotNull` + `@DecimalMin("0.0")` | `400` 对应 message |
| type | `@NotBlank` + `RequirementType.valueOf()` 枚举白名单 | `400` "无效的需求分类: xxx" |

### 4.2 前端校验 (JavaScript)

- title.trim().length < 5 → 阻止提交并提示
- budget 为空/null/负数 → 阻止提交并提示
- type 未选择 → 阻止提交并提示
- HTML5 原生 `required` / `maxlength` / `min="0"` 作为第一道防线

### 4.3 认证校验

- `JwtAuthInterceptor` 拦截 `/api/v1/**`（排除 auth/health），无有效 token 返回 401
- `CurrentUser.requireUserId(request)` 从请求属性提取 userId，确保发布者身份真实

## 五、测试方法

> **以下命令均为 Windows cmd 格式**，可直接复制一行粘贴回车执行。
> 注意：cmd 中 `-d` 参数体内的双引号需转义为 `\`"`。

### 5.1 前置条件

```cmd
:: 1. 启动 Docker 基础设施（如未启动）
docker-compose up -d

:: 2. 构建并启动后端（如未启动）
cd backend
mvn clean package -DskipTests （可能因进程占用而构建失败，可用 taskkill /F /IM java.exe 终止所有java进程）
start java -jar target\campushub-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

:: 3. 启动前端（如需要浏览器测试）
cd frontend
npm install
npx vite --host

:: 4. 获取测试 token（使用已有用户或注册新用户）
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"pangbin\",\"password\":\"pass123456\"}"
```

从返回的 JSON 中复制 `data.token` 字段的值，下文以 `<TOKEN>` 指代。

### 5.2 正常发布需求

```cmd
:: 正常发布 → 期望: {"code":200,"message":"发布成功","data":{"reqId":<雪花ID>}}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"求代拿北门快递中通\",\"description\":\"在北门中通快递点，大概下午3点去拿，小件\",\"budget\":5.00,\"type\":\"EXPRESS\"}"
```

### 5.3 必填校验测试

```cmd
:: 标题为空 → 期望: {"code":400,"message":"标题不能为空"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"\",\"description\":\"test desc\",\"budget\":10.00,\"type\":\"EXPRESS\"}"

:: 标题少于5字符 → 期望: {"code":400,"message":"标题长度需在 5-50 字符之间"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"ab\",\"description\":\"test desc\",\"budget\":10.00,\"type\":\"EXPRESS\"}"

:: 描述为空 → 期望: {"code":400,"message":"描述不能为空"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"test title abc\",\"description\":\"\",\"budget\":10.00,\"type\":\"EXPRESS\"}"

:: 预算为空 → 期望: {"code":400,"message":"预算不能为空"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"test title abc\",\"description\":\"test desc\",\"type\":\"EXPRESS\"}"

:: 分类为空 → 期望: {"code":400,"message":"需求分类不能为空"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"test title abc\",\"description\":\"test desc\",\"budget\":10.00,\"type\":\"\"}"
```

### 5.4 预算 < 0 测试

```cmd
:: 预算为负数 → 期望: {"code":400,"message":"预算金额必须大于等于0"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"test title abc\",\"description\":\"test desc\",\"budget\":-1.00,\"type\":\"EXPRESS\"}"
```

### 5.5 无效分类测试

```cmd
:: 非法分类值 → 期望: {"code":400,"message":"无效的需求分类: INVALID_TYPE"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d "{\"title\":\"test title abc\",\"description\":\"test desc\",\"budget\":10.00,\"type\":\"INVALID_TYPE\"}"
```

### 5.6 未认证拒绝测试

```cmd
:: 无 token → 期望: {"code":401,"message":"未认证或凭证失效"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -d "{\"title\":\"test title abc\",\"description\":\"test desc\",\"budget\":10.00,\"type\":\"EXPRESS\"}"

:: 无效 token → 期望: {"code":401,"message":"未认证或凭证失效"}
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer garbage123" -d "{\"title\":\"test title abc\",\"description\":\"test desc\",\"budget\":10.00,\"type\":\"EXPRESS\"}"
```

### 5.7 数据库验证

```cmd
:: 查询刚发布的需求（替换 <REQ_ID> 为上一步返回的 reqId）
docker exec campushub-mysql mysql -u ch_dev -pch_password campushub_db -e "SELECT req_id, publisher_id, title, budget, type, status, created_at FROM biz_requirement WHERE req_id=<REQ_ID>;"

:: 期望: status 为 PENDING，publisher_id 为当前用户 ID，created_at 为当前时间
```

### 5.8 前端浏览器测试

1. 打开 `http://localhost:5173/` → 登录（如 pangbin / pass123456）
2. 点击侧边栏「发布需求」
3. 填写表单：标题(5-50字)、选分类、填预算(>=0)、写描述(<=500字)
4. 点击「发布需求」→ 提示 `发布成功！需求编号：xxx`
5. 1.5 秒后自动跳转到 `/requirements/<reqId>` 详情页
6. 回到需求大厅，应能看到刚发布的需求

## 六、前端校验交互测试

| 操作 | 期望结果 |
|------|---------|
| 标题输入少于 5 个字符后点发布 | 红色提示「标题至少需要 5 个字符」 |
| 不填预算点发布 | 红色提示「请填写预算金额」 |
| 预算填 -1 点发布 | 红色提示「预算金额必须大于等于 0」 |
| 不选分类点发布 | 红色提示「请选择需求分类」 |
| 所有字段正确填写后点发布 | 绿色提示「发布成功！需求编号：xxx」 |

## 七、实现过程中发现的问题

### 7.1 `main.js` 与 `main.ts` 双入口文件（阻塞性 bug，已修复）

**现象**：后端 API 测试全部通过，但浏览器访问发布页面始终报"资源不存在"或"服务端异常"。

**排查过程**：
1. 浏览器 Console 显示 `GET :8080/api/v1/requirements/publish` 返回 404
2. 检查后端日志发现 `NumberFormatException: For input string: "publish"`，说明 `@PathVariable Long reqId` 收到了字符串 "publish"
3. 推断前端路由将 `/requirements/publish` 匹配到了 `/:reqId` 路由，导致 `RequirementDetailView` 调用 `getRequirement("publish")`
4. 使用 `curl http://localhost:5173/src/main.js` 直接拉取前端入口文件，发现返回的路由中**没有 `/requirements/publish`**
5. 进一步发现项目同时存在 `src/main.js` 和 `src/main.ts` 两个入口文件，而 `index.html` 实际加载的是 `main.js`，我修改的 `router/index.ts` 从未被加载

**修复**：在 `main.js`（实际入口）中新增 `PublishRequirementView` 的 import，并在 routes 数组中添加 `/requirements/publish` 路由（置于 `/:reqId` 之前），同时标记 `meta: { requiresAuth: true }` 以复用已有的路由守卫。

```javascript
import PublishRequirementView from './views/PublishRequirementView.vue'

const routes = [
  // ...
  { path: '/requirements/publish', component: PublishRequirementView, meta: { requiresAuth: true } },
  { path: '/requirements/:reqId', component: RequirementDetailView, meta: { requiresAuth: true } },
  // ...
]
```

### 7.2 `GET /api/v1/requirements/publish` 返回 500（已修复）

**现象**：`GET /api/v1/requirements/publish` 返回 `{"code":500,"message":"服务端异常"}`。

**原因**：`@GetMapping("/{reqId}")` 未对路径变量做格式约束，"publish" 被传入 `@PathVariable Long reqId` 导致 Spring 类型转换抛出 `NumberFormatException`，被通用 `handleException(Exception.class)` 捕获后返回 500。

**修复**：两处改动——
1. 给 `@GetMapping` 路径变量加正则约束：`@GetMapping("/{reqId:\\d+}")`，仅匹配纯数字，非数字路径直接不匹配该 handler
2. 新增 `NoResourceFoundException` 处理器返回 404，避免 Spring Boot 3 的未匹配请求被通用异常处理器捕获为 500：
```java
@ExceptionHandler(NoResourceFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException exception) {
    return ApiResponse.fail(404, "资源不存在");
}
```

### 7.3 全局异常吞掉堆栈（已修复）

**现象**：后端返回通用 500 "服务端异常"，但控制台看不到具体异常信息，排查困难。

**原因**：`GlobalExceptionHandler.handleException(Exception.class)` 仅返回错误响应，未记录日志。

**修复**：添加 `Logger` 并在通用异常处理器中打印完整堆栈：
```java
private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

@ExceptionHandler(Exception.class)
public ApiResponse<Void> handleException(Exception exception) {
    log.error("未捕获的异常: {}", exception.getMessage(), exception);
    return ApiResponse.fail(ApiCode.INTERNAL_ERROR.getCode(), ApiCode.INTERNAL_ERROR.getMessage());
}
```

### 7.4 JavaScript Number 精度丢失导致大 ID 匹配失败（已修复）

**现象**：发布成功后跳转到详情页显示"需求不存在"，且 URL 中 reqId 末尾几位与数据库实际值不一致。

**原因**：MyBatis-Plus Snowflake 生成的 `reqId` 是 19 位 64-bit Long（如 `2055858038844178433`）。JSON 序列化为数字后，JavaScript `JSON.parse` 将其转为 `Number` 类型，而 JS `Number` 采用 IEEE 754 双精度浮点，整数安全范围为 `±2^53`（约 16 位）。19 位 ID 超出精度范围，末尾 3~4 位被截断为 0，导致 `getRequirement(错位ID)` 查不到记录。

**验证**：后端返回 `reqId: 2055858038844178433`，浏览器 `console.log` 打印 `2055858038844178400`（末两位丢失）。

**修复**：新增 `JacksonConfig`，全局将 `Long`/`long` 序列化为 JSON 字符串，前端收到的 `reqId` 为 `"2055858038844178433"`（字符串），不再经过 JS Number 转换：
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
        };
    }
}
```

> **影响范围**：此配置使所有 `Long` 字段（`reqId`、`publisherId`、`userId`、`total` 等）统一序列化为字符串。前端 `auth.js` 中 `setUserId()` 已使用 `String(userId)` 存储，不受影响。

### 7.5 RequireType 枚举与前端下拉选项不一致（已修复，非本次引入）

**现象**：`RequirementListView.vue` 中分类下拉包含 `LOST_FOUND`，但后端 `RequirementType` 枚举中无此值，导致用 `LOST_FOUND` 筛选时查询无结果。

**处理**：`PublishRequirementView.vue` 的分类下拉严格使用后端 `RequirementType` 枚举的所有值（EXPRESS、TUTORING、SECOND_HAND、STUDY_HELP、MATERIAL_SHARE、TEAM_UP、CARPOOL、Q_AND_A、OTHER），未包含 `LOST_FOUND`。`RequirementListView.vue` 的下拉选项待后续统一修正。

### 7.6 MyBatis-Plus 与 JdbcClient 混合使用（设计决策记录）

`RequirementService` 中原有代码使用 `JdbcClient` 做查询，新增的 `createRequirement` 使用 `BizRequirementMapper`（MyBatis-Plus）做插入。理由是 MyBatis-Plus 的 `IdType.ASSIGN_ID` 可自动生成 Snowflake ID，避免手写 ID 生成逻辑。两者在同一个 Service 中并存，通过构造器注入两个依赖。

### 7.7 Windows Git Bash 中文编码导致 curl 测试失败（非阻塞，已知）

测试 3（正常发布含中文）返回 `{"code":400,"message":"请求体格式错误"}`（`HttpMessageNotReadableException`）。原因是 Git Bash 环境下 curl 传中文 JSON 时编码异常。使用纯 ASCII payload 可正常测试，浏览器前端使用 UTF-8 不受影响。此问题在 `profile-integration.md` 中已有记录。

### 7.8 Maven clean 因 jar 占用失败（环境问题）

`mvn clean` 时若旧 Java 进程仍占用 `target/*.jar`，会报 `Failed to delete` 错误。解决方式：先 `taskkill /F /IM java.exe` 终止所有 Java 进程，或使用 `cmd //c "taskkill /PID <PID> /F"` 指定进程终止。

## 八、调试与测试流程总结

### 8.1 后端 API 测试（curl）

| 步骤 | 命令 | 结果 |
|------|------|------|
| 注册测试账号 | `POST /api/v1/auth/register` | 200 |
| 登录获取 token | `POST /api/v1/auth/login` | 200 + JWT |
| 正常发布 | `POST /api/v1/requirements` + token | 200 + reqId(字符串) |
| 标题为空 | `POST` title="" | 400 校验失败 |
| 标题 < 5 字 | `POST` title="ab" | 400 长度不足 |
| 预算负数 | `POST` budget=-1 | 400 必须 >=0 |
| 无效分类 | `POST` type="INVALID" | 400 无效分类 |
| 无 token | `POST` 无 Authorization | 401 未认证 |
| GET /publish | `GET /api/v1/requirements/publish` | 404 (原 500) |
| 正常查询详情 | `GET /api/v1/requirements/<reqId>` | 200 + Long→String |

### 8.2 前端路由验证（curl）

```cmd
:: 验证 Vite 返回的 main.js 包含 publish 路由
curl -s http://localhost:5173/src/main.js | grep "requirements/publish"
:: 期望: { path: '/requirements/publish', component: PublishRequirementView, meta: { requiresAuth: true } }
```

### 8.3 浏览器端到端测试

1. 打开 `http://localhost:5173/` → 自动跳转 `/requirements`（需登录）
2. 登录后点击侧边栏「发布需求」
3. 填写表单并提交 → 绿色提示「发布成功」
4. 1.5 秒后自动跳转详情页，显示完整需求信息
5. 回到需求大厅可看到新发布的需求

## 九、关键设计点

- **ID 生成**：使用 MyBatis-Plus `@TableId(type = IdType.ASSIGN_ID)` 自动生成 Snowflake 算法 ID，无需额外配置
- **状态初始化**：新发布的需求状态固定为 `PENDING`（待接单），不在请求体中暴露 status 字段
- **发布者身份**：`publisherId` 从 JWT token 提取，不由前端传入，防止伪造发布者
- **预算精度**：数据库使用 `DECIMAL(10,2)`，前端使用 `type="number" step="0.01"`，支持两位小数
- **P3 符合度**：API 路径 `POST /api/v1/requirements`、字段定义、校验规则均对齐 P3 设计文档规范
