# **CampusHub 开发环境配置与协作规范手册**

本手册旨在指导团队成员快速搭建开发环境，并明确开发过程中的技术规约，确保代码质量与 AI 协作流程的标准化。

## **1\. 环境准备**

在开始之前，请确保你的本地机器已安装以下软件版本：

| 组件 | 版本要求 | 说明 |
| :---- | :---- | :---- |
| **JDK** | 17 (LTS) | 核心后端运行环境 |
| **Node.js** | 18.x 或 20.x (LTS) | 前端开发环境，建议配合 nvm 使用 |
| **Docker** | Desktop 4.x+ | 用于一键运行基础设施 (MySQL, Redis) |
| **Build Tool** | Maven 3.8+ | 后端依赖管理 |
| **IDE** | IntelliJ IDEA / VS Code | 推荐安装 CheckStyle 与 AI 助手插件 |

## **2\. 基础设施一键启动**

项目使用 Docker 统一基础设施环境，避免本地数据库版本不一致的问题。

1. **启动服务**：在项目根目录下执行：  
   ```
   docker-compose up \-d
   ```

2. **服务清单**：  
   * **MySQL 8.0**: 端口 3306，初始数据库 campushub。  
   * **Redis 7.0**: 端口 6379，用于存放 Session 与缓存数据。  
3. **初始化数据**：首次启动后，请执行 `backend/src/main/resources/db/migration` 下的 SQL 脚本。

## **3\. 后端开发规范**

* **框架**：Spring Boot 3 \+ MyBatis-Plus。  
* **启动方式**：运行 `CampusHubApplication.java`。  
* **配置管理**：  
  * `application-dev.yml`：个人本地配置，**禁止**提交生产敏感信息。  
  * 核心参数必须通过环境变量或 Spring Profiles 切换。  
* **接口规范**：统一使用 RESTful 风格，所有接口返回值必须封装在 `Result<T>` 对象中。

## **4\. 前端开发规范**

* **框架**：Vue 3 (Composition API) \+ Vite \+ Element Plus。  
* **启动方式**：  
  ```
  cd frontend  
  npm install  
  npm run dev
  ```  

* **目录约束**：  
  * api/：所有后端请求必须封装在此，严禁在页面内直接使用 axios。  
  * components/：存放高复用组件；views/：按业务模块划分页面。

## **5\. 常见问题排查**
* 端口冲突：若本地已安装 MySQL 或 Redis 导致端口被占用，请修改 `docker-compose.yml` 中的 ports 映射左侧数值。

* 连接超时：确保后端 `application-dev.yml` 中的数据库地址为 `localhost`（若在宿主机运行）或 `campushub-db`（若在容器内运行）。

## **6\. AI 协作与工程规范**


1. 开发过程中严格遵守《AI协助契约》
2. 合并至 develop 前，使用AI工具对全量代码进行架构耦合扫描。针对订单状态机等核心逻辑，使用AI工具排查边界漏洞。

