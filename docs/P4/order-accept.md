# 接单接口实现说明

> 分支：`feature/order-accept-zjh` | 基于 `develop` (601a2f4)

## 一、功能概要

实现接单 API (`POST /api/v1/orders`)，基于需求创建订单。处理"重复接单"和"发布者接自己需求"两种业务错误，接单成功后需求状态同步更新为 ACCEPTED。前端需求详情页"接单"按钮完成对接。

## 二、新增文件

```
backend/src/main/java/com/campushub/order/
├── OrderController.java              # POST /api/v1/orders
├── OrderService.java                 # 接单业务逻辑 + @Transactional
└── dto/
    └── CreateOrderRequest.java       # 接单请求 DTO ({ reqId: Long })
```

## 三、修改文件

| 文件 | 变更内容 |
|------|----------|
| `frontend/src/api/requirements.js` | 新增 `acceptOrder(reqId)` 函数 |
| `frontend/src/views/RequirementDetailView.vue` | 接单按钮接入 `handleAccept`：调用 API → 成功更新本地状态（status/acceptable）→ 失败展示对应错误消息（含 4001/4002） |
| `frontend/tsconfig.json` | `ignoreDeprecations` 5.0→6.0 消除 baseUrl 弃用警告 |
| `backend/.../auth/AuthService.java` | `studentId` 空值判断 `!= null` → `StringUtils.hasText()`，修复空字符串导致 `uk_student_id` 唯一冲突 |

## 四、业务逻辑设计

### 4.1 接单流程

```
POST /api/v1/orders { reqId }
  │
  ├── JwtAuthInterceptor 鉴权 (无 token → 401)
  ├── @Valid 校验 reqId 非空
  ├── CurrentUser.requireUserId() 提取 receiverId
  │
  └── OrderService.createOrder(reqId, receiverId) @Transactional
        │
        ├── 1. 查询需求 (publisher_id, budget, status)
        │     └── 不存在 → 404 "需求不存在"
        │
        ├── 2. publisherId == receiverId ?
        │     └── 是 → 4002 "发布者不可接取自身发布的需求"
        │
        ├── 3. status != "PENDING" ?
        │     └── 是 → 4001 "手慢了，该需求已被他人接取"
        │
        ├── 4. INSERT INTO biz_order (req_id UNIQUE)
        │     └── DuplicateKeyException → 4001 (并发兜底)
        │
        └── 5. UPDATE biz_requirement SET status='ACCEPTED'
```

### 4.2 并发安全

- `biz_order` 表 `uk_req_id` UNIQUE KEY 确保数据库层一需求一订单
- 先做应用层 status 检查（快速失败 + 明确错误消息），再依赖 DB 约束 catch `DuplicateKeyException`（并发兜底）
- `@Transactional` 保证 INSERT order + UPDATE requirement 原子性

### 4.3 错误码

| 错误码 | 场景 | 消息 |
|--------|------|------|
| 4001 | 需求已被接单 / 并发重复 | "手慢了，该需求已被他人接取" |
| 4002 | 发布者接自己需求 | "发布者不可接取自身发布的需求" |
| 401 | 未认证 | "未认证或凭证失效" |
| 404 | 需求不存在 | "需求不存在" |

## 五、测试方法

> **以下命令均为 Windows cmd 格式**，可直接复制一行粘贴回车执行。

### 5.1 前置条件

```cmd
:: 1. 启动依赖服务
docker-compose up -d

:: 2. 构建并启动后端
cd backend
mvn clean package -DskipTests
start java -jar target\campushub-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

:: 3. 启动前端
cd frontend
npx vite --host
```

### 5.2 准备测试账号和数据

```cmd
:: 注册两个测试用户
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"test_pub\",\"password\":\"test123456\"}"
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"test_rec\",\"password\":\"test123456\"}"

:: 登录获取 token
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"test_pub\",\"password\":\"test123456\"}"
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"test_rec\",\"password\":\"test123456\"}"

:: test_pub 发布需求（用 TOKEN_PUB 替换上一步获取的 token）
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN_PUB>" -d "{\"title\":\"accept order test req\",\"description\":\"testing acceptance\",\"budget\":25.00,\"type\":\"EXPRESS\"}"
```

记下返回的 `reqId`，下文以 `<REQ_ID>` 指代。

### 5.3 接单正常流程

```cmd
:: test_rec 接单 → 期望: {"code":200,"message":"接单成功","data":{"orderId":<雪花ID>}}
curl -s -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN_REC>" -d "{\"reqId\":<REQ_ID>}"
```

### 5.4 发布者接自己需求

```cmd
:: test_pub 接自己发布的需求 → 期望: {"code":4002,"message":"发布者不可接取自身发布的需求"}
curl -s -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN_PUB>" -d "{\"reqId\":<REQ_ID>}"
```

### 5.5 重复接单

```cmd
:: 需求已被 test_rec 接单后，再次接单 → 期望: {"code":4001,"message":"手慢了，该需求已被他人接取"}
curl -s -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN_REC>" -d "{\"reqId\":<REQ_ID>}"
```

### 5.6 需求状态验证

```cmd
:: 查询需求详情，确认 status 已变为 ACCEPTED → 期望: "status":"ACCEPTED"
curl -s http://localhost:8080/api/v1/requirements/<REQ_ID> -H "Authorization: Bearer <TOKEN_PUB>" | findstr "status"
```

### 5.7 数据库验证

```cmd
:: 验证订单已生成，状态为 IN_PROGRESS
docker exec campushub-mysql mysql -u ch_dev -pch_password campushub_db -e "SELECT order_id, req_id, receiver_id, amount, status FROM biz_order ORDER BY created_at DESC LIMIT 1;"

:: 验证需求状态已更新
docker exec campushub-mysql mysql -u ch_dev -pch_password campushub_db -e "SELECT req_id, status FROM biz_requirement WHERE req_id=<REQ_ID>;"
```

### 5.8 未认证拒绝

```cmd
:: 无 token → 期望: {"code":401,"message":"未认证或凭证失效"}
curl -s -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -d "{\"reqId\":<REQ_ID>}"
```

### 5.9 需求不存在

```cmd
:: 不存在的 reqId → 期望: {"code":404,"message":"需求不存在"}
curl -s -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN_PUB>" -d "{\"reqId\":1}"
```

### 5.10 前端浏览器测试

1. 打开 `http://localhost:5173/` → 登录 test_rec
2. 进入需求大厅 → 点击 test_pub 发布的需求
3. 详情页显示「接单」按钮（状态 PENDING 且非自己发布）
4. 点击「接单」→ 按钮变为「接单中...」
5. 成功 → 绿色提示「接单成功！订单编号：xxx」，按钮变为「当前不可接单」，状态更新为 ACCEPTED
6. 登录 test_pub 进入同一需求 → 按钮显示「当前不可接单」
7. 登录 test_pub 点击自己发布的需求 → 点击接单 → 红色提示「发布者不可接取自身发布的需求」

## 六、实现过程中发现的问题

### 6.1 并发接单保护（设计决策）

**风险**：两个用户同时对同一 PENDING 需求发起接单，仅靠应用层 status 检查无法完全防住。

**处理**：双保险机制——
1. **应用层**：先检查 `status != "PENDING"` 快速失败返回 4001
2. **数据库层**：`biz_order.uk_req_id` UNIQUE KEY 兜底，第二个 INSERT 会抛 `DuplicateKeyException`，catch 后同样返回 4001

```java
try {
    bizOrderMapper.insert(order);
} catch (DuplicateKeyException e) {
    throw new BusinessException(ApiCode.ORDER_TAKEN);
}
```

### 6.2 测试用 reqId 超出 Long.MAX_VALUE（非阻塞）

**现象**：测试不存在的需求时用 `reqId = 9999999999999999999`，后端返回 `{"code":400,"message":"请求体格式错误"}`。

**原因**：`9999999999999999999` > `Long.MAX_VALUE`（9223372036854775807），Jackson 反序列化时抛出 `JsonParseException`，被 `HttpMessageNotReadableException` 处理器捕获。

**处理**：使用有效 Long 范围内的测试 ID（如 `1`）。

### 6.3 tsconfig.json baseUrl 弃用警告（已修复）

**现象**：VSCode 中 `tsconfig.json` 报 `选项"baseUrl"已弃用，并将停止在 TypeScript 7.0 中运行`。

**原因**：TypeScript 5.x → 6.x 将 `baseUrl` 标记为弃用（Vite `@` 别名仍需使用 `paths`）。

**修复**：将 `ignoreDeprecations` 从 `"5.0"` 改为 `"6.0"` 消除警告。

### 6.4 reqId 类型选择（设计决策）

P3 规范定义 `reqId` 为 `String` 类型，但系统中实际使用 Snowflake `Long`。`CreateOrderRequest` 使用 `Long` 类型以保持内部一致性，Jackson 全局配置已确保序列化时转为字符串。

### 6.5 注册接口 `student_id` 空字符串唯一冲突（阻塞性 bug，已修复）

**现象**：在浏览器中注册新用户返回 `{"code":500,"message":"服务端异常"}`。后端日志显示：

```
Duplicate entry '' for key 'sys_user.uk_student_id'
```

**排查过程**：
1. 查看完整堆栈，确认是 `SysUserMapper.insert` 时 MySQL 报 `uk_student_id` 唯一索引冲突
2. 查看 INSERT 参数，`student_id` 值为空字符串 `''`
3. 查询数据库，发现用户 `testuserzjh` 的 `student_id = ''`（历史脏数据）
4. 检查 `AuthService.register()` 第 34 行：
```java
user.setStudentId(request.getStudentId() != null ? request.getStudentId() : "U_" + request.getUsername());
```
5. 发现只判了 `!= null`，但前端表单未填 `studentId` 时发送的是 `""`（空字符串），不是 `null`

**根因**：`RegisterRequest.studentId` 字段无 `@NotBlank` 约束，前端表单提交时未填此字段会发送 `""`。`AuthService` 的 `!= null` 判断无法过滤空字符串，导致第一个用户写入 `student_id=''`，后续用户再注册即触发唯一索引冲突。

**修复**：
1. `AuthService.register()` 将 `!= null` 改为 `StringUtils.hasText()`，同时过滤 `null` 和空字符串：
```java
import org.springframework.util.StringUtils;
user.setStudentId(StringUtils.hasText(request.getStudentId()) ? request.getStudentId() : "U_" + request.getUsername());
```
2. 修复数据库历史脏数据：
```sql
UPDATE sys_user SET student_id = CONCAT('U_', username) WHERE student_id = '';
```

## 七、关键设计点

- **一需求一订单**：`biz_order.uk_req_id` UNIQUE KEY + 应用层校验双重保障
- **事务原子性**：`@Transactional` 确保 INSERT order 与 UPDATE requirement status 同时成功或回滚
- **身份安全**：`receiverId` 从 JWT 提取，不由请求体传入，防止伪造接单者
- **状态驱动 UI**：前端接单成功后直接本地更新 `requirement.acceptable = false` 和 `status = 'ACCEPTED'`，无需重新请求详情
- **金额继承**：订单 `amount` 直接从需求 `budget` 复制，接单时不可修改
