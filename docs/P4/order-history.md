# 订单详情与订单历史列表实现说明

> 分支：`feature/order-history-zjh` | 基于 `develop` (296763e)

## 一、功能概要

实现订单详情查询 (`GET /api/v1/orders/{orderId}`) 与订单历史列表 (`GET /api/v1/orders?tab=received|published`)，区分"我接取的订单"和"我发布的订单"。越权访问他人订单返回 403。

## 二、新增文件

```
backend/src/main/java/com/campushub/order/dto/
├── OrderListItem.java          # 订单列表项 DTO
└── OrderDetailResponse.java    # 订单详情 DTO

frontend/src/
├── api/orders.js               # 订单 API 封装
├── views/OrderListView.vue     # 我的订单页（双 tab）
└── views/OrderDetailView.vue   # 订单详情页
```

## 三、修改文件

| 文件 | 变更内容 |
|------|----------|
| `backend/.../order/OrderService.java` | 新增 `getOrderDetail()` + `listRelatedOrders()`：详情含发布者/接单者/需求信息，列表按 tab 区分 received/published |
| `backend/.../order/OrderController.java` | 新增 `GET /api/v1/orders` 和 `GET /api/v1/orders/{orderId}` |
| `frontend/src/main.js` | 新增 `OrderListView`、`OrderDetailView` import，添加 `/orders` 和 `/orders/:orderId` 路由 |
| `frontend/src/App.vue` | 侧边栏新增「我的订单」导航链接 |

## 四、接口设计

### 4.1 订单详情

```
GET /api/v1/orders/{orderId}
认证: 需要
```

**响应**：`{ orderId, reqId, reqTitle, reqDescription, publisherId, publisherName, receiverId, receiverName, amount, status, createdAt, finishedAt }`

**权限校验**：当前用户必须是订单的发布者（`biz_requirement.publisher_id`）或接单者（`biz_order.receiver_id`），否则返回 403。

### 4.2 订单历史列表

```
GET /api/v1/orders?tab=received|published&page=1&pageSize=10
认证: 需要
```

**tab 参数**：
- `received`（默认）：查询 `biz_order.receiver_id = currentUserId`
- `published`：查询 `biz_requirement.publisher_id = currentUserId`（JOIN biz_requirement）

**分页**：默认 page=1, pageSize=10, 最大 50。

## 五、测试方法

> **以下命令均为 Windows cmd 格式**。

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

### 5.2 准备测试数据

```cmd
:: 注册三个测试用户
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"test_pub\",\"password\":\"test123456\"}"
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"test_rec\",\"password\":\"test123456\"}"
curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"username\":\"test_other\",\"password\":\"test123456\"}"

:: 登录获取 token
:: test_pub 登录 → 记下 token 为 <TOKEN_PUB>
:: test_rec 登录 → 记下 token 为 <TOKEN_REC>
:: test_other 登录 → 记下 token 为 <TOKEN_OTHER>

:: test_pub 发布需求
curl -s -X POST http://localhost:8080/api/v1/requirements -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN_PUB>" -d "{\"title\":\"order history test req\",\"description\":\"testing order queries\",\"budget\":15.00,\"type\":\"EXPRESS\"}"
:: 记下返回的 reqId 为 <REQ_ID>

:: test_rec 接单
curl -s -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN_REC>" -d "{\"reqId\":<REQ_ID>}"
:: 记下返回的 orderId 为 <ORDER_ID>
```

### 5.3 订单详情（正常）

```cmd
:: 接单者查看 → 期望: {"code":200,...}
curl -s http://localhost:8080/api/v1/orders/<ORDER_ID> -H "Authorization: Bearer <TOKEN_REC>"

:: 发布者查看 → 期望: {"code":200,...}
curl -s http://localhost:8080/api/v1/orders/<ORDER_ID> -H "Authorization: Bearer <TOKEN_PUB>"
```

### 5.4 越权访问

```cmd
:: 第三方查看他人订单 → 期望: {"code":403,"message":"无权限操作"}
curl -s http://localhost:8080/api/v1/orders/<ORDER_ID> -H "Authorization: Bearer <TOKEN_OTHER>"
```

### 5.5 订单不存在

```cmd
:: 不存在的订单ID → 期望: {"code":404,"message":"订单不存在"}
curl -s http://localhost:8080/api/v1/orders/1 -H "Authorization: Bearer <TOKEN_PUB>"
```

### 5.6 未认证

```cmd
:: 无 token → 期望: {"code":401,"message":"未认证或凭证失效"}
curl -s http://localhost:8080/api/v1/orders/<ORDER_ID>
```

### 5.7 订单列表

```cmd
:: test_rec 我接取的 → 期望: total >= 1
curl -s "http://localhost:8080/api/v1/orders?tab=received" -H "Authorization: Bearer <TOKEN_REC>"

:: test_pub 我发布的 → 期望: total >= 1
curl -s "http://localhost:8080/api/v1/orders?tab=published" -H "Authorization: Bearer <TOKEN_PUB>"

:: test_rec 我发布的(应无) → 期望: "total":"0"
curl -s "http://localhost:8080/api/v1/orders?tab=published" -H "Authorization: Bearer <TOKEN_REC>"
```

### 5.8 数据库验证

```cmd
:: 查看订单数据
docker exec campushub-mysql mysql -u ch_dev -pch_password campushub_db -e "SELECT o.order_id, o.req_id, r.title, o.receiver_id, o.amount, o.status FROM biz_order o JOIN biz_requirement r ON o.req_id = r.req_id ORDER BY o.created_at DESC LIMIT 5;"
```

### 5.9 前端浏览器测试

1. 打开 `http://localhost:5173/` → 登录 test_rec
2. 点击侧边栏「我的订单」→ 默认显示"我接取的" tab，有 1 条
3. 点击订单行 → 进入详情页，显示订单号、需求标题、发布者、接单者、金额、状态
4. 切换「我发布的」tab → 列表为空
5. 登录 test_pub →「我的订单」→「我发布的」tab → 有 1 条

## 六、实现过程中发现的问题

### 6.1 越权校验涉及跨表查询

**问题**：判断一个用户是否有权查看某个订单，需要同时判断 `biz_order.receiver_id`（接单者）和 `biz_requirement.publisher_id`（发布者），即涉及两张表。

**方案**：在 `getOrderDetail` 的 SQL 中一次性 JOIN 三张表（order + requirement + 两个 user），查询结果中同时包含 `publisherId` 和 `receiverId`，在内存中比较当前用户是否匹配其中任一角色。不匹配则抛 403。

```java
if (!row.publisherId().equals(userId) && !row.receiverId().equals(userId)) {
    throw new BusinessException(ApiCode.FORBIDDEN);
}
```

### 6.2 列表 SQL 中 tab 参数的条件构建

**问题**："我发布的"列表需要 JOIN 到 `biz_requirement` 表，按 `publisher_id` 过滤；"我接取的"只需按 `biz_order.receiver_id` 过滤。两个查询的 WHERE 条件不同但其他部分一致。

**方案**：根据 `tab` 参数动态拼接 roleClause，避免写两份重复 SQL：

```java
String roleClause = "published".equals(tab)
    ? " r.publisher_id = :userId "
    : " o.receiver_id = :userId ";
```

### 6.3 "我发布的" tab 仅显示已被接单的需求（逻辑缺陷，已修复）

**现象**：用户发布需求后，在「我的订单 → 我发布的」中看不到刚发布的需求。只有等他人接单生成订单后，该 tab 才显示内容。

**排查过程**：
1. 查询数据库，确认需求已发布（status=PENDING）但 `biz_order` 表中无对应记录
2. 检查 `listRelatedOrders` 的 SQL，发现 "published" 分支使用 `biz_order JOIN biz_requirement` 查询，INNER JOIN 排除了没有订单的需求

**根因**：原实现将"我发布的"理解为"我发布的需求所对应**已生成的订单**"，但从用户视角，"我发布的"应该包含所有发布的需求（无论是否已被接单）。

**修复**：将 "published" 分支改为 `biz_requirement LEFT JOIN biz_order LEFT JOIN sys_user`：
- 已接单的需求：返回订单信息（orderId、接单者、订单状态等）
- 未接单的需求：orderId/receiverId/receiverName 为 null，status 为 "PENDING"，amount 取需求预算

```sql
-- 修复后
SELECT r.req_id, r.title AS req_title, r.budget, r.status AS req_status,
       o.order_id, o.receiver_id, o.amount, o.status AS order_status,
       o.created_at AS order_created_at, r.created_at AS req_created_at,
       COALESCE(ru.nickname, ru.username) AS receiver_name
FROM biz_requirement r
LEFT JOIN biz_order o ON r.req_id = o.req_id
LEFT JOIN sys_user ru ON o.receiver_id = ru.user_id
WHERE r.publisher_id = :userId
ORDER BY r.created_at DESC
```

**前端配合**：`OrderListItem` 中 `orderId`/`receiverId`/`receiverName` 改为可空。列表中 PENDING 项跳转至 `/requirements/{reqId}` 而非 `/orders/{orderId}`，无接单者时显示"暂无接单"。新增 `statusLabel()` 函数将英文状态码转为中文。

## 七、关键设计点

- **权限模型**：订单的"相关人员"包括发布者和接单者两方，任一方均可查看详情
- **列表区分**：通过 `tab` 参数区分 received/published，前端用双 tab 切换
- **数据关联**：详情一次性 JOIN 三张表（order + requirement + user × 2），单次查询完成，避免 N+1
- **403 vs 404**：不存在的订单返回 404（不暴露 ID 是否存在），存在但无权限返回 403
- **分页复用**：列表使用与需求列表相同的 `PageResponse` 结构，前端分页组件一致
