# **评价、信用结算与管理后台实现说明**

分支：P4-yrh | 基于 develop

## **一、功能概要**

实现系统业务逻辑的闭环与管控，主要包括以下功能：

1. **评价提交流程**：支持订单双方在订单完成后进行 1-5 星评价，对未完成订单、非参与者评价及重复评价进行校验与限制。  
2. **信用分结算引擎**：根据评价星级动态更新被评价人的系统信用分（5星+2，4星+1，3星+0，2星-1，1星-2，最低限制为0分）。采用 @Transactional 保证双表操作的原子性。  
3. **轻量管理后台**：为 sys\_user 表引入 role 角色机制，提供管理员专属接口以实现违规/过期需求的一键下架。

## **二、新增文件**

```Plaintext

backend/src/main/java/com/campushub/  
├── evaluation/  
│   ├── controller/EvaluationController.java     \# POST /api/v1/orders/{orderId}/evaluations  
│   ├── service/EvaluationService.java             
│   ├── service/impl/EvaluationServiceImpl.java  \# 评价与信用分结算核心逻辑  
│   └── dto/EvaluationSubmitReq.java             \# 评价请求 DTO (含参数校验与 Lombok 注解)  
├── admin/  
│   ├── controller/AdminController.java          \# PUT /api/v1/admin/requirements/{reqId}/cancel  
│   ├── service/AdminService.java  
│   └── service/impl/AdminServiceImpl.java       \# 管理员权限校验与需求下架逻辑
```

*(注：计算规则的单元测试文件位于 backend/src/test/java/com/campushub/evaluation/service/EvaluationServiceImplTest.java)*

## **三、修改文件**

| 文件 | 变更内容 |
| :---- | :---- |
| backend/src/main/resources/db/migration/mysql-init/init.sql | sys\_user 表新增 role 字段定义；替换演示数据为脱敏账户，并明确区分普通用户(0)与管理员(1) |
| backend/src/main/java/com/campushub/entity/SysUser.java | 补充 creditScore 与新增的 role 字段映射 |

## **四、业务逻辑设计**

### **4.1 评价与结算链路**

```Plaintext

POST /api/v1/orders/{orderId}/evaluations  
  │  
  ├── CurrentUser.requireUserId() 提取当前请求用户  
  ├── @Validated 校验评分 (1-5分)  
  │  
  └── EvaluationServiceImpl.submitEvaluation() @Transactional  
        ├── 1. 查询订单及关联需求，若不存在则返回 404  
        ├── 2. 状态校验：非 "COMPLETED" → 抛 4003 订单未完成  
        ├── 3. 身份校验：非发布者且非接单者 → 抛 4004 无权评价  
        ├── 4. 重复评价校验：查询 biz\_evaluation → 抛 400 防止重复评价  
        ├── 5. INSERT INTO biz\_evaluation (存入目标用户ID)  
        └── 6. 信用结算：读取目标用户原信用分，根据星级计算偏移量，确保分值不为负数，UPDATE sys\_user
```


### **4.2 违规下架链路**

```Plaintext

PUT /api/v1/admin/requirements/{reqId}/cancel  
  │  
  ├── CurrentUser.requireUserId() 提取当前用户  
  │  
  └── AdminServiceImpl.cancelRequirement()  
        ├── 1\. 查询 sys\_user 获取 role，若为空或 \!= 1 → 抛 403 越权操作  
        └── 2\. UPDATE biz\_requirement SET status \= 'CANCELED'
```


## **五、测试方法**

**以下测试基于本地全栈启动**

### **5.1 评价系统测试**

```DOS

:: 前置：获取已完成订单的参与者 token (假设订单 ID 为 1，参与者为 test\_user1)  
curl \-s \-X POST http://localhost:8080/api/v1/auth/login \-H "Content-Type: application/json" \-d "{\\"username\\":\\"test\_user1\\",\\"password\\":\\"demo\_hash\\"}"

:: 正常评价与加分 (5星) → 期望: {"code":200,"message":"评价提交成功"}  
curl \-s \-X POST http://localhost:8080/api/v1/orders/1/evaluations \-H "Content-Type: application/json" \-H "Authorization: Bearer \<TOKEN\>" \-d "{\\"star\\":5,\\"content\\":\\"非常顺利的交易！\\"}"

:: 重复评价拦截 → 期望: {"code":400,"message":"您已对该订单进行过评价"}  
curl \-s \-X POST http://localhost:8080/api/v1/orders/1/evaluations \-H "Content-Type: application/json" \-H "Authorization: Bearer \<TOKEN\>" \-d "{\\"star\\":4,\\"content\\":\\"再评一次\\"}"

:: 星级越界拦截 → 期望: {"code":400,"message":"评分最高为5分 / 评分最低为1分"}  
curl \-s \-X POST http://localhost:8080/api/v1/orders/1/evaluations \-H "Content-Type: application/json" \-H "Authorization: Bearer \<TOKEN\>" \-d "{\\"star\\":6,\\"content\\":\\"爆表好评\\"}"
```

### **5.2 后台管理测试**

```DOS

:: 前置：获取普通用户与管理员的 token  
:: test\_user1 (普通) \-\> \<USER\_TOKEN\>  
:: test\_admin (管理) \-\> \<ADMIN\_TOKEN\>

:: 普通用户尝试下架需求 → 期望: {"code":403,"message":"越权操作：仅系统管理员可执行此操作"}  
curl \-s \-X PUT http://localhost:8080/api/v1/admin/requirements/20001/cancel \-H "Authorization: Bearer \<USER\_TOKEN\>"

:: 管理员执行违规下架 → 期望: {"code":200,"message":"违规需求已成功下架"}  
curl \-s \-X PUT http://localhost:8080/api/v1/admin/requirements/20001/cancel \-H "Authorization: Bearer \<ADMIN\_TOKEN\>"
```

## **六、实现过程中发现的问题与决策记录**

### **1\. DTO 注解依赖问题**

**现象**：创建 EvaluationSubmitReq 时，javax.validation 无法解析。

**处理**：适配 Spring Boot 3.x 规范，将 javax.validation 替换为 jakarta.validation。

### **2\. 管理员权限设计补充**

**现象**：SysUser 实体原设计未包含权限区分机制，无法直接满足管理后台的专属操作需求。

**处理**：修改了基建脚本 init.sql，引入 role 字段（0=普通用户，1=管理员），并在 AdminServiceImpl 中实施相应的权限校验。同时将初始的演示测试数据全部进行了脱敏处理。