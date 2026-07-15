# 🛒 乡村振兴 - 农产品电商平台

<p align="center">
  <strong>一个面向乡村振兴的现代化多端 B2C 电商平台</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-2.7.9-6DB33F" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue.js-3.x-4FC08D" alt="Vue.js">
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.3-1693E6" alt="MyBatis-Plus">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7.x-DC382D" alt="Redis">
  <img src="https://img.shields.io/badge/微信小程序-原生-09B83E" alt="WeChat Mini Program">
  <img src="https://img.shields.io/badge/DeepSeek-AI-009688" alt="DeepSeek">
</p>

---

## 📋 项目概述

**乡村振兴** 是一个面向乡村振兴场景的农产品电商平台，采用**前后端分离 + 多端协同**架构，覆盖买家、商家、管理员、客服四类角色。系统包含完整的电商核心（商品/订单/支付/售后/营销），并融合 **RAG 知识库**、**AI 智能客服**、**STOMP 实时聊天**、**可观测性**等进阶能力。

### ✨ 核心特性

- 🎯 **多端一体**：Web 商城 (Vue3) + 商家管理端 (Vue3) + 管理后台 (Vue3+TS) + 微信小程序 (原生) + 共享 UI 库
- 🤖 **AI 智能服务**：DeepSeek-v4-flash + RAG 检索增强 + Embedding 向量语义召回 + 意图分类
- 💬 **实时通讯**：WebSocket + STOMP + SockJS，支持买家 ↔ 商家 ↔ 客服三方会话
- 🔐 **完整鉴权**：JWT + Spring Security + 自研 `PermissionInterceptor` 角色白名单
- ⚡ **高性能**：Redis 缓存 + HikariCP 连接池 + Thumbnailator 图片压缩 + MyBatis-Plus 分页
- 📊 **可观测性**：Actuator + Micrometer Prometheus 指标 + TraceId 链路追踪 + 结构化日志
- 🛡️ **安全防护**：BCrypt 密码哈希 + XSS 过滤 + 图形验证码 + 短信验证码 + 接口限流 (`@RateLimit`)

---

## 🛡️ 安全与密钥管理

> **本项目所有敏感信息（数据库密码、Redis 密码、AI Key、JWT 密钥、admin 默认密码）必须通过环境变量注入，绝不能硬编码到代码、配置或文档中。**

### 必须遵守的规则

1. **本地开发**：复制 `backend/.env.example` → `backend/.env`，填入你自己的密钥
2. **Git 提交**：`.env` 必须加入 `.gitignore`（已默认配置），只提交 `.env.example`
3. **生产部署**：使用 Docker secrets / K8s Secret / 云平台密钥管理服务
4. **密钥轮换**：JWT 密钥、admin 密码每 90 天轮换一次

### 占位符规范

README 中所有 `<YOUR_*>` 形式的占位符都表示**你必须替换的密钥**，例如：

| 占位符 | 含义 | 获取方式 |
|---|---|---|
| `<YOUR_DB_PASSWORD>` | MySQL root 密码 | 自己设定（≥ 16 位，含大小写+数字+符号） |
| `<YOUR_REDIS_PASSWORD_OR_EMPTY>` | Redis 密码 | `redis-cli config set requirepass <pwd>` |
| `<YOUR_DEEPSEEK_API_KEY>` | DeepSeek 平台 Key | https://platform.deepseek.com |
| `<YOUR_EMBEDDING_API_KEY>` | OpenAI 兼容的 Embedding Key | https://platform.openai.com |
| `<YOUR_256BIT_RANDOM_SECRET>` | JWT 签名密钥 | `openssl rand -base64 64` |
| `<INITIAL_PASSWORD>` | admin 初始密码 | `.env.example` 中查找或 `start-all.bat` 启动日志 |
| `<BCRYPT_HASH_OF_NEW_PASSWORD>` | BCrypt 密码哈希 | https://bcrypt-generator.com 生成 |

### 提交前自检

```bash
# 检查代码中是否误提交了密钥
git secrets --scan
# 或
grep -rE "(123456|admin123|sk-[a-zA-Z0-9]{20,})" --include="*.java" --include="*.yml" --include="*.md" .
```

---

## 🏗️ 系统架构

```
┌────────────────────────────────────────────────────────────────────┐
│                          客户端层 (5 个端)                          │
├──────────────┬──────────────┬──────────────┬───────────┬───────────┤
│   web-mall   │  seller-web  │   admin-web  │ mini-prog │shared-ui  │
│  用户商城     │  商家管理     │  后台管理     │ 微信小程序 │共享组件   │
│   Vue 3      │   Vue 3      │ Vue3 + TS    │ 原生 WXML │ Vue 组件  │
│   :5176      │    :5173     │   :3001      │  微信 IDE  │   复用    │
└──────┬───────┴──────┬───────┴──────┬───────┴─────┬─────┴─────┬─────┘
       │ HTTP/WS      │ HTTP/WS      │ HTTP/WS     │ HTTPS     │
       └──────────────┴──────┬───────┴─────────────┴───────────┘
                             │
                       Vite Proxy (/api, /uploads, /images, /ws-chat)
                             │
                    ┌────────▼────────┐
                    │   Spring Boot   │  ← :8081
                    │  Backend 后端   │
                    │  28 Controllers │
                    │  42 Services    │
                    └─┬────────────┬──┘
                      │            │
            ┌─────────▼─┐    ┌─────▼──────┐
            │   MySQL   │    │   Redis    │
            │  8.0.x    │    │   7.x      │
            │ minimall  │    │  Lettuce   │
            └───────────┘    └────────────┘
                    │
            ┌───────▼─────────┐
            │ Spring Boot    │
            │ Actuator +     │  ← /actuator/health, /actuator/prometheus
            │ Prometheus     │
            └────────────────┘
```

> **端口速查**：
> | 服务 | 端口 | 启动入口 |
> |---|---|---|
> | Backend | 8081 | `backend/` |
> | Admin Web | 3001 | `admin-web/` |
> | Seller Web | 5173 | `seller-web/` |
> | Web Mall | 5176 | `web-mall/` |
> | MySQL | 3306 | docker / 本地 |
> | Redis | 6379 | docker / 本地 |

---

## 🧰 技术栈

### 后端

| 技术 | 版本 | 用途 |
|---|---|---|
| Java | 8 | 编程语言 |
| Spring Boot | 2.7.9 | 应用框架 |
| Spring Security | 5.7.x | 安全框架 |
| MyBatis-Plus | 3.5.3.1 | ORM（不用 JPA） |
| MySQL | 8.0.33 | 关系型数据库 |
| Redis (Lettuce) | 7.x | 缓存 / 验证码 / 分布式 session |
| HikariCP | 4.x | JDBC 连接池（最大 50 线程） |
| JWT (jjwt) | 0.11.5 | Token 认证 |
| Springfox Swagger | 3.0.0 | API 文档 |
| WebSocket + STOMP | - | 实时通信 |
| DeepSeek API | v4-flash | AI 大模型 |
| Embedding API | text-embedding-3-small (1536 维) | RAG 向量化 |
| Thumbnailator | 0.4.19 | 图片压缩 |
| Apache HttpClient | 4.5.13 | HTTP 客户端 / 连接池 |
| Spring Boot Actuator + Micrometer Prometheus | - | 可观测性 |
| dotenv-java | 3.0.0 | 读 `.env` |
| Lombok | 1.18.30 | 代码简化 |

### 前端（三个独立工程）

| 项目 | Vue | Vite | TS | Pinia | Element Plus | Vite Plugins |
|---|---|---|---|---|---|---|
| `admin-web` | 3.3.4 | 5.0.8 | ✅ | 2.1.7 | 2.4.4 | unplugin-auto-import, sass-embedded |
| `seller-web` | 3.2.0 | 4.0.0 | ❌ | 3.0.4 | 2.4.4 | unplugin-auto-import |
| `web-mall` | 3.2.0 | 4.0.0 | ❌ | 3.0.4 | 2.4.4 | — |
| `shared-ui` | 3.x | 库 | — | — | — | 跨工程复用组件 |

> 三个前端都使用 **Element Plus 2.4.4** + **@stomp/stompjs 7.3.0** + **SockJS 1.6.1** + **Axios 1.x** + **vue-router 4.x**。
> 共同点：都通过 Vite proxy 把 `/api` `/uploads` `/images` 代理到 `http://localhost:8081`。

### 小程序

| 技术 | 说明 |
|---|---|
| 微信小程序原生框架 | 无第三方 UI 库 |
| 自定义组件 `components/mall-icon` | 图标组件 |
| TabBar 5 个 | 首页 / 分类 / 购物车 / 优惠 / 我的 |

### 工具 / 中间件

| 工具 | 用途 |
|---|---|
| Maven 3.8+ | 后端构建 |
| Node 16+（推荐 18 LTS） | 前端构建 |
| Git | 版本控制 |
| Docker + docker-compose | 容器化部署 |
| MySQL Workbench / Navicat | 数据库 GUI |
| RedisInsight | Redis GUI |
| VSCode / WebStorm / IntelliJ IDEA | IDE |

---

## 📦 模块说明

### 1️⃣ backend - 后端服务 [Java/Spring Boot]

**目录结构：**
```
backend/
├── src/main/java/com/example/minimall/
│   ├── MinimaMallApplication.java   # Spring Boot 启动类
│   ├── annotation/                 # @RateLimit 自定义注解
│   ├── common/                     # 统一返回 Result<T>
│   ├── config/                     # 14 个配置类（Security/WebSocket/Redis/MyBatisPlus/AI/RAG/...）
│   ├── constants/                  # RedisKeys / ResponseCode
│   ├── context/                    # AppContext（请求上下文）
│   ├── controller/                 # 29 个 REST 控制器（含 BaseController）
│   ├── dto/                        # 数据传输对象
│   ├── enums/                      # 枚举（订单/售后/优惠券/用户类型/...）
│   ├── exception/                  # BusinessException + GlobalExceptionHandler
│   ├── filter/                     # XssFilter
│   ├── initializer/                # DataInitializer（启动初始化）
│   ├── interceptor/                # 4 个拦截器（鉴权/限流/日志/性能）
│   ├── mapper/                     # 43 个 MyBatis-Plus Mapper 接口
│   ├── model/                      # 43 个实体（与 DB 表 1:1）
│   ├── security/                   # XSS 工具
│   ├── service/                    # 42 个业务接口
│   │   ├── impl/                   # 实现类
│   │   ├── AIService.java          # DeepSeek 调用 + RAG 编排
│   │   ├── RagService.java         # 检索增强
│   │   ├── HnswIndex.java          # HNSW 向量索引（O(log n) 检索）
│   │   ├── EmbeddingService.java   # 向量化（外部 API + 本地 TF-IDF 降级）
│   │   ├── KnowledgeBaseService.java
│   │   ├── IntentClassifierService.java
│   │   ├── RagMonitorService.java
│   │   ├── ProductContextOptimizer.java
│   │   ├── ContentFilterService.java
│   │   ├── SensitiveWordFilter.java
│   │   └── ...
│   ├── utils/                      # JwtUtil, PasswordValidator, LogisticsApiClient
│   ├── vo/                         # 9 个视图对象
│   └── websocket/                  # 5 个 WS 处理器（Chat/AdminChat/STOMP/Event/...）
├── src/main/resources/
│   ├── mapper/                     # MyBatis XML (22 个)
│   ├── sql/                        # SQL 脚本
│   │   ├── schema.sql              # 建表
│   │   ├── data.sql                # 测试数据
│   │   ├── init_database.sql       # 完整初始化（建表+数据，36 张表）
│   │   └── migrate_*.sql           # 4 个迁移脚本
│   ├── static/images/              # 静态资源
│   ├── application.yml             # 主配置（环境变量驱动）
│   └── logback-spring.xml          # 日志配置
├── src/test/                       # 单元 + 集成测试（JUnit 5 + Mockito + H2）
├── pom.xml                         # Maven
├── Dockerfile                      # Docker 镜像构建
├── api_test.ps1                    # PowerShell API 烟测脚本
└── .env.example                    # 环境变量模板
```

**核心功能模块：**
- 👤 用户认证（3 种登录：账号密码 / 短信验证码 / 微信小程序）
- 🛡️ RBAC 权限（`PermissionInterceptor` + `PermissionService` + `Role` + `RolePermission`）
- 📦 商品管理（分类 / 规格 / 标签 / 多图）
- 🛒 购物车 + 订单（事务 + 悲观锁防超卖）
- 💳 支付（演示版 Mock 支付，2 秒回调）
- 🎫 优惠券 + 满减活动
- 💬 客服聊天（买家 ↔ 商家 ↔ 客服三方 + STOMP）
- 🤖 AI 客服（RAG 检索 + DeepSeek 生成 + 流式 SSE）
- 📚 知识库管理（文档分块 + Embedding 索引 + FAQ 匹配）
- 🔧 售后服务（退款 / 退货 / 进度跟踪）
- 📊 数据统计 + 仪表盘
- 🛡️ 可观测性（Actuator + Prometheus + TraceId）

**端口**：`8081`（可通过 `SERVER_PORT` 环境变量覆盖）

---

### 2️⃣ admin-web - 管理后台 [Vue 3 + TypeScript]

**技术栈**：Vue 3.3.4 + Vite 5.0.8 + TypeScript 5.3.3 + Element Plus 2.4.4 + Pinia 2.1.7

**路由表（13 个页面）**：

| 路径 | 名称 | 功能 |
|---|---|---|
| `/login` | 登录 | 管理员 / 商家账号登录 |
| `/dashboard` | 仪表盘 | 销售统计 + 图表 + 待办 |
| `/product` | 商品管理 | 商品 CRUD、上下架、规格 |
| `/order` | 订单管理 | 全平台订单、状态机、发货 |
| `/user` | 用户管理 | 买家/商家账号管理 |
| `/activity` | 活动管理 | 满减 / 折扣活动 |
| `/aftersale` | 售后管理 | 售后工单处理 |
| `/system` | 系统设置 | 系统参数配置 |
| `/seller` | 商家审核 | 商家入驻审核 |
| `/customer-service` | 客服聊天 | 客服视角实时聊天 |
| `/intervention` | 人工介入 | AI 答不了的复杂问题 |
| `/agent-management` | 客服管理 | 客服账号管理 |
| `/knowledge` | 知识库管理 | RAG 文档 / FAQ 维护 |

**端口**：`3001`

**特色**：
- 全 TS 类型安全
- 自动导入 Vue/Pinia/Router/Element Plus
- STOMP 实时聊天（管理端 + 客户端）

---

### 3️⃣ seller-web - 商家管理端 [Vue 3]

**技术栈**：Vue 3.2.0 + Vite 4.0.0 + Element Plus 2.4.4 + Pinia 3.0.4 + ECharts 5.4.3

**路由模式**：`createWebHashHistory`（URL 带 `#`，无需 Nginx rewrite）

**核心功能**（来自 `seller-web/src/router/index.js`）：

| 路径 | 名称 | 功能 |
|---|---|---|
| `/login` | Login | 商家登录（账号密码 / 短信） |
| `/register` | Register | 商家注册 |
| `/dashboard` | Dashboard | 销售数据 + 订单概览 |
| `/products` | ProductList | 商家自有商品列表 |
| `/product/create` | ProductCreate | 新增商品 |
| `/product/edit/:id` | ProductEdit | 编辑商品 |
| `/orders` | OrderList | 订单列表 / 详情 / 发货 |
| `/aftersale` | AfterSale | 退换货审核 |
| `/coupons` | CouponManage | 优惠券管理（CRUD） |
| `/discounts` | DiscountManage | 折扣/满减活动 |
| `/customer-service` | CustomerService | 与买家实时沟通（STOMP） |
| `/reviews` | ReviewManagement | 商品评价查看 / 回复 |
| `/profile` | Profile | 店铺信息、账户设置 |

**端口**：`5173`

**Token 存储**：`localStorage.seller_token` + `localStorage.seller_user`

---

### 4️⃣ web-mall - 用户商城 [Vue 3]

**技术栈**：Vue 3.2.0 + Vite 4.0.0 + Element Plus 2.4.4 + Pinia 3.0.4 + vue-router 4.6.4

**路由模式**：`createWebHashHistory`（URL 带 `#`，无需 Nginx rewrite）

**完整功能**（来自 `web-mall/src/router/index.js`）：

| 路径 | 名称 | 功能 |
|---|---|---|
| `/` | Home | 首页（Banner、推荐、分类入口、搜索） |
| `/product/list` | ProductList | 分类筛选、关键词搜索、排序、分页 |
| `/product/:id` | ProductDetail | 规格选择、加购物车、立即购买、客服 |
| `/cart` | Cart | 数量调整、删除、全选、价格合计 |
| `/order` | Order | 订单列表（全部 / 待付款 / 待发货 / 待收货） |
| `/order-list` | OrderList | 订单列表（带 Tab） |
| `/payment/:id` | OrderDetail | 结算 / 支付 / 订单详情 |
| `/ai` | AI | AI 助手（DeepSeek 流式 SSE） |
| `/service/chat` | ServiceChat | 智能客服（AI + 人工切换） |
| `/profile` | Profile | 个人中心 |
| `/security` | Security | 账户安全 / 密码 |
| `/address` | AddressList | 收货地址列表 |
| `/address-edit` | AddressEdit | 地址编辑 |
| `/login` | Login | 账号 / 短信登录 |
| `/register` | Register | 注册 |
| `/reset-password` | ResetPassword | 短信验证码重置 |
| `/coupon` | Coupon | 优惠券中心 |
| `/discount` | Discount | 折扣活动 |
| `/discount-detail/:id` | DiscountDetail | 折扣详情 |
| `/activity/list` | ActivityList | 活动列表 |
| `/activity/detail/:id` | ActivityDetail | 活动详情 |
| `/aftersale/list` | AfterSaleList | 售后列表 |
| `/aftersale/create` | AfterSaleCreate | 申请售后 |
| `/aftersale/detail/:id` | AfterSaleDetail | 售后详情 |
| `/review/order/:id` | OrderReview | 订单评价 |

**端口**：`5176`

**路由模式**：`createWebHashHistory`（URL 带 `#`，无需 Nginx rewrite）

**Token 存储**：`localStorage.token` + `localStorage.user`

**.env.development**：
```env
VITE_API_BASE_URL=http://localhost:8081/api
VITE_REQUEST_TIMEOUT=10000
VITE_UPLOAD_MAX_SIZE=5242880
VITE_PAGE_SIZE=20
```

**.env.production**：
```env
VITE_API_BASE_URL=https://api.yourdomain.com/api
VITE_REQUEST_TIMEOUT=15000
VITE_UPLOAD_MAX_SIZE=5242880
VITE_PAGE_SIZE=20
```

---

### 5️⃣ mini-program - 微信小程序 [原生]

**TabBar 5 个页面**（`app.json` 中定义）：

| 顺序 | pagePath | 名称 |
|---|---|---|
| 1 | `pages/home/home` | 首页 |
| 2 | `pages/category/category` | 分类 |
| 3 | `pages/cart/cart` | 购物车 |
| 4 | `pages/coupon/index` | 优惠 |
| 5 | `pages/user/index` | 我的 |

**全部 21 个页面**（含非 TabBar，按 `app.json.pages` 顺序）：

| 类型 | 页面 | 用途 |
|---|---|---|
| Tab | `pages/home/home` | 首页（推荐 / Banner / 搜索） |
| Tab | `pages/category/category` | 分类浏览 |
| Tab | `pages/cart/cart` | 购物车 |
| Tab | `pages/coupon/index` | 优惠中心 |
| Tab | `pages/user/index` | 我的 |
| 商品 | `pages/product/product` | 商品详情 |
| 订单 | `pages/order/list`, `pages/order/order` | 订单列表 / 详情 |
| 用户 | `pages/user/address`, `pages/user/addressEdit` | 收货地址 |
| 用户 | `pages/user/profile`, `pages/user/security` | 资料 / 安全 |
| AI | `pages/ai/ai` | AI 助手 |
| 售后 | `pages/aftersale/{list, create, detail}` | 售后三件套 |
| 营销 | `pages/discount/index`, `pages/activity/{list, detail}` | 折扣 / 活动 |
| 客服 | `pages/chat/chat` | 客服聊天 |
| 评价 | `pages/review/review` | 商品评价 |

**自定义组件**：`components/mall-icon`（图标库，`usingComponents` 已全局注册）

**AppID**：`app.json` 中未硬编码，导入时使用"测试号"或填入自有 AppID（`project.config.json` 中可配置）。

---

### 6️⃣ shared-ui - 共享 UI 组件库

跨三个前端复用的统一设计体系，被 `admin-web` `seller-web` `web-mall` 通过 Vite alias `@mall/shared-ui` 引用：

```js
// vite.config.js
resolve: {
  alias: {
    '@': path.resolve(__dirname, './src'),
    '@mall/shared-ui': path.resolve(__dirname, '../shared-ui')
  }
}
```

**目录结构**：

```
shared-ui/
├── base.css                    # 基础样式
├── design-tokens.css           # 设计 token（颜色 / 间距 / 字号）
├── index.css                   # 入口聚合
├── UI_DESIGN_SPECIFICATION.md  # UI 设计规范
├── components/                 # 通用 CSS 组件样式
│   ├── button.css card.css form.css
│   ├── modal.css nav.css table.css
│   └── tag.css
├── utilities/                  # 通用 CSS 工具类
│   ├── animations.css
│   ├── layout.css
│   ├── micro-interactions.css
│   ├── spacing.css
│   └── typography.css
└── vue-components/             # 独立 Vue 组件（@mall/shared-ui）
    ├── MlButton.vue  MlCard.vue  MlInput.vue
    ├── MlModal.vue   MlTag.vue
    ├── index.js      package.json
```

**复用方式**：三端 `package.json` 通过 Vite alias 引用 `@mall/shared-ui`，按需 import 即可使用。

---

## 🚀 快速开始

### 0️⃣ 环境要求

| 工具 | 版本要求 | 说明 |
|---|---|---|
| Node.js | >= 16（推荐 18 LTS） | 前端 |
| Java JDK | **必须 JDK 8** | 后端 |
| Maven | >= 3.8 | 后端构建 |
| MySQL | >= 8.0 | 数据库 |
| Redis | >= 6.0 | 缓存 |
| 微信开发者工具 | 最新版 | 小程序（可选） |
| Docker Desktop | >= 4.x（可选） | 想用容器化方案时 |

> 💡 **不想装 MySQL/Redis/JDK？** 直接看 [🐳 Docker 部署](#-docker-部署) 章节，一条命令全起。

### 1️⃣ 克隆项目

```bash
git clone <repository-url>
cd mall_system_extended
```

### 2️⃣ 数据库初始化（不用 Docker 时）

后端有 **3 个初始化 SQL + 4 个迁移 SQL**，按顺序执行：

```bash
cd backend

# 1. 建表
mysql -u root -p < src/main/resources/sql/schema.sql

# 2. 测试数据
mysql -u root -p < src/main/resources/sql/data.sql

# 3. 完整初始化（500 行，表+数据）
mysql -u root -p < src/main/resources/sql/init_database.sql
```

如有版本升级，按编号顺序执行迁移脚本：
```bash
mysql -u root -p < src/main/resources/sql/migrate_cart_unique_index.sql
mysql -u root -p < src/main/resources/sql/migrate_cart_unique_virtual_column.sql
mysql -u root -p < src/main/resources/sql/migrate_drop_order_sn.sql
mysql -u root -p < src/main/resources/sql/migrate_unique_order_no.sql
```

> 数据库名默认 `minimall`，用户名 `root`，密码从 `application.yml` / `.env` 读取（**切勿在 README 写明文密码**）。

### 3️⃣ 启动 Redis

```bash
# Windows（需先安装 Redis for Windows）
redis-server

# 或用 Docker
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 4️⃣ 配置环境变量

在 `backend/` 下复制 `.env.example` 为 `.env`：

```bash
cd backend
cp .env.example .env
# 编辑 .env，填入真实密码和 API Key
```

`.env` 内容模板（**所有值仅作占位示例，请替换为你自己的密钥后提交**）：
```env
# 数据库
DB_HOST=localhost
DB_PORT=3306
DB_NAME=minimall
DB_USERNAME=root
DB_PASSWORD=<YOUR_DB_PASSWORD>             # ← 改为你的 MySQL 密码

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<YOUR_REDIS_PASSWORD_OR_EMPTY>
REDIS_DATABASE=0

# 服务端口
SERVER_PORT=8081

# DeepSeek AI（必填，否则 AI 走降级）
DEEPSEEK_API_KEY=<YOUR_DEEPSEEK_API_KEY>   # ← 从 https://platform.deepseek.com 申请

# Embedding（用于 RAG，可选）
EMBEDDING_API_URL=https://api.openai.com/v1/embeddings
EMBEDDING_API_KEY=<YOUR_EMBEDDING_API_KEY> # ← 与 OpenAI 兼容的 Embedding Key
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_DIMENSIONS=1536

# JWT
JWT_SECRET=<YOUR_256BIT_RANDOM_SECRET>     # ← 用 openssl rand -base64 64 生成
JWT_EXPIRATION=86400000
```

### 5️⃣ 启动后端

```bash
cd backend
mvn spring-boot:run
# 或
mvn clean package -DskipTests
java -jar target/minimall-0.0.1-SNAPSHOT.jar
```

**启动成功标志**：
- 控制台打印 `Started MinimaMallApplication in X.XXX seconds`
- 端口 8081 已监听：`netstat -an | findstr :8081`
- Swagger 文档可访问：http://localhost:8081/swagger-ui/index.html

### 6️⃣ 安装前端依赖

```bash
# 三个前端都要装
npm install --prefix admin-web
npm install --prefix seller-web
npm install --prefix web-mall
```

### 7️⃣ 启动前端

每个前端用独立终端：

| 前端 | 命令 | 端口 | 默认账号 |
|---|---|---|---|
| 管理后台 | `cd admin-web && npm run dev` | 3001 | `admin / <INITIAL_PASSWORD>` |
| 商家端 | `cd seller-web && npm run dev` | 5173 | 商家账号（用 admin 登录后创建） |
| 用户商城 | `cd web-mall && npm run dev` | 5176 | 注册新账号 |

### 8️⃣ 启动微信小程序（可选）

1. 安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 打开工具 → 导入项目 → 目录选 `mini-program/`
3. AppID 用 `wxa17d14480861589e`（项目已有）或切换到"测试号"
4. 详情 → 本地设置 → **关闭** "不校验合法域名"（开发期）

### 9️⃣ 一键启动（Windows 批处理）

```bash
# 根目录有 start-all.bat，一键启动 4 个服务
start-all.bat
```

启动后会自动打开 3 个浏览器标签页（admin 3001 / seller 5173 / web-mall 5176）。

对应停止：`stop-all.bat`

---

## 📁 项目目录结构

```
mall_system_extended/
│
├── backend/                          # 后端（Spring Boot 2.7.9）
│   ├── src/main/java/com/example/minimall/
│   │   ├── MinimaMallApplication.java
│   │   ├── annotation/ common/ config/ constants/ context/
│   │   ├── controller/               # 29 个 REST 控制器
│   │   ├── dto/ enums/ exception/ filter/ initializer/
│   │   ├── interceptor/              # 4 个拦截器
│   │   ├── mapper/                   # 43 个 Mapper
│   │   ├── model/                    # 43 个实体
│   │   ├── security/ service/ utils/ vo/ websocket/
│   ├── src/main/resources/
│   │   ├── mapper/                   # 22 个 MyBatis XML
│   │   ├── sql/                      # 3 init + 4 migrate
│   │   ├── static/images/
│   │   ├── application.yml
│   │   └── logback-spring.xml
│   ├── src/test/                     # 单元 + 集成测试
│   ├── pom.xml
│   ├── Dockerfile                    # 多阶段构建（JDK17 + JRE17 精简镜像）
│   ├── .dockerignore                 # 构建上下文过滤
│   ├── api_test.ps1
│   └── .env.example
│
├── admin-web/                        # 管理后台（Vue3 + TS）
│   └── src/
│       ├── api/                      # 12 个 API 模块
│       ├── composables/              # useChatNotification, useStompClient
│       ├── layouts/ router/ stores/ styles/ types/ utils/ views/
│       ├── App.vue main.ts
│       └── auto-imports.d.ts components.d.ts
│
├── seller-web/                       # 商家管理端（Vue3，hash 路由）
│   └── src/{views, components, stores, router, utils, layouts, composables}
│
├── web-mall/                         # 用户商城（Vue3，hash 路由）
│   ├── src/{views, components, stores, router, utils, composables}
│   ├── .env.development
│   └── .env.production
│
├── shared-ui/                        # 跨端共享 UI（CSS 工具 + Vue 组件）
│   ├── base.css design-tokens.css index.css UI_DESIGN_SPECIFICATION.md
│   ├── components/                   # 7 个 CSS 组件样式
│   ├── utilities/                    # 5 个 CSS 工具类
│   └── vue-components/               # 5 个 Vue 组件
│
├── mini-program/                     # 微信小程序（原生，21 个页面）
│   ├── pages/{home, category, product, cart, coupon, user, ai, aftersale, discount, activity, chat, order, review}/
│   ├── components/mall-icon/
│   ├── app.{js, json, wxss}
│   └── cloudbaserc.json
│
├── docs/                             # 项目文档
│   ├── PERFORMANCE_REPORT.md
│   ├── RAG_TECHNICAL_DOCUMENTATION.md
│   ├── SYSTEM_OPTIMIZATION_DOCUMENTATION.md
│   └── usecase-*.{drawio, png, svg, puml, mmd}   # 5 个用例图
│
├── .devcontainer/                    # VSCode 容器化开发环境
│   ├── devcontainer.json
│   ├── docker-compose.yml
│   └── init.sh
│
├── start-all.bat                     # Windows 一键启动（本地开发）
├── stop-all.bat                      # Windows 一键停止
├── start-docker.bat                  # Windows Docker 启动
├── stop-docker.bat                   # Windows Docker 停止
├── docker-compose.yml                # 根级容器编排（MySQL+Redis+Backend）
├── .env.example                      # Docker 环境变量模板
├── 答辩准备.md                       # 毕业答辩 Q&A（113 个高频问题 + 7 大数据流追踪）
└── README.md                         # 本文件
```

---

## 🔌 API 接口文档

### Swagger UI

启动后端后访问：

```
http://localhost:8081/swagger-ui/index.html
```

> **注意**：本项目用 **Springfox 3.0.0**，访问路径是 `/swagger-ui/index.html`（**不是** `/swagger-ui.html`）。

### API 模块清单（共 28 个 Controller）

| 模块 | 基础路径 | 主要端点 | 鉴权 |
|---|---|---|---|
| 认证 | `/api/auth/**` | login / logout / register / sendCode / loginByCode / bindPhone / resetPassword / passwordRules / user | 部分公开 |
| 用户 | `/api/user/**` | list / detail / update | JWT |
| 商家 | `/api/seller/**` | list / audit / detail / register | JWT + seller |
| 商品 | `/api/product/**` | list / recommended / detail / categories / search | 公开 |
| 商品分类 | `/api/product-category/**` | tree / list | 公开 |
| 分类 | `/api/category/**` | list / top / children / create / update-status | 部分 JWT |
| 购物车 | `/api/cart/**` | list / add / update-quantity / update-checked / delete / clear | JWT |
| 订单 | `/api/order/**` | create / list / pay / cancel / confirm / ship / detail / status | JWT |
| 售后 | `/api/aftersale/**` | list / apply / approve / detail | JWT |
| 售后聊天 | `/api/aftersale/chat/**` | send / history / unread-count | JWT |
| 优惠券 | `/api/coupon/**` | list / create / claim / available / update | JWT |
| 活动 | `/api/activity/**` | list / detail | 公开 |
| 折扣 | `/api/discount/**` | list / detail | 公开 |
| 聊天 | `/api/chat/**` | session / messages / send / read / monitor | JWT |
| 客服 | `/api/cs/**` | agent / transfer / faq | JWT + agent |
| 管理聊天 | `/api/admin/chat/**` | sessions / messages | JWT + admin |
| 人工介入 | `/api/admin/intervention/**` | intervene / assign / process | JWT + agent |
| 上传 | `/api/upload/**` | image | JWT |
| 图片 | `/api/image/**` | static | 公开 |
| 验证码 | `/api/captcha/**` | image | 公开 |
| 短信 | `/api/sms/**` | send-code | 公开 |
| 评价 | `/api/review/**` | list / create | 部分公开 |
| AI | `/api/ai/**` | query / chat (SSE) / rag-query / rag-chat / logs / monitor/* | JWT |
| 知识库 | `/api/knowledge/**` | document / chunk / faq / stats | JWT + admin |
| FAQ | `/api/faq/**` | list / match | JWT |
| 系统 | `/api/system/**` | dashboard / config | JWT + admin |
| 调试 | `/api/debug/**` | reset / ping | dev only |

### 鉴权方式

所有需要登录的接口在 Header 携带 JWT：

```http
Authorization: Bearer <token>
```

**获取 Token**：
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "<YOUR_PASSWORD>",
  "captchaKey": "<CAPTCHA_UUID>",
  "captchaCode": "<CAPTCHA_TEXT>"
}
```

返回：
```json
{
  "code": 0,
  "data": {
    "token": "<JWT_TOKEN>",
    "user": { "id": 1, "username": "admin", "userType": 2 },
    "expiration": 1720867200000
  }
}
```

### 角色类型

| userType | 角色 | 登录入口 |
|---|---|---|
| 0 | 买家 | web-mall / mini-program |
| 1 | 商家 | seller-web |
| 2 | 管理员 | admin-web |

---

## 🐳 Docker 部署

### 方案 A：compose 一键起（推荐，本地/CI/演示）

**前置条件**：已安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)（含 docker compose）

```bash
# 1. 准备 .env（首次）
cp .env.example .env
# 编辑 .env，至少改 MYSQL_ROOT_PASSWORD、JWT_SECRET

# 2. 一键启动（后台）
docker compose up -d

# 3. 查看服务状态
docker compose ps

# 4. 实时看后端日志
docker compose logs -f backend

# 5. 停止（保留数据）
docker compose down

# 6. 停止 + 清数据（重置）
docker compose down -v
```

**Windows 快捷方式**：

```cmd
:: cmd 里直接双击或在 cmd 中运行
start-docker.bat       :: 启动（自动等待健康检查通过）
stop-docker.bat        :: 停止
```

> ⚠️ **如果用 PowerShell**（不是 cmd），必须加 `.\` 前缀：
> ```powershell
> .\start-docker.bat
> # 或
> cmd /c start-docker.bat
> ```
> 因为 PowerShell 默认不在当前目录查找可执行文件（安全设计）。

### compose 包含哪些服务

| 服务 | 镜像 | 端口映射 | 数据卷 | 用途 |
|---|---|---|---|---|
| `mysql` | `mysql:8.0` | `${MYSQL_PORT:-3306}:3306` | `mysql-data` | 数据库，首次启动自动执行 `init_database.sql` 建表+导数据 |
| `redis` | `redis:7-alpine` | `${REDIS_PORT:-6379}:6379` | `redis-data` | 缓存/会话/限流 |
| `backend` | 本地 `Dockerfile` 构建 | `${SERVER_PORT:-8081}:8081` | `backend-uploads` / `backend-logs` | Spring Boot 后端 |

**关键设计点**：

- ✅ **健康检查链路**：`mysql` 和 `redis` 先 `healthy` → `backend` 才开始启动
- ✅ **持久化**：4 个命名卷（MySQL/Redis/上传/日志）`down` 不丢数据
- ✅ **init 脚本自动建库**：`init_database.sql` 挂在 MySQL 的 `docker-entrypoint-initdb.d/`，**仅在首次启动**生效
- ✅ **网络隔离**：自建 `minimall-net` bridge，容器内用服务名通信（`DB_HOST=mysql` 而不是 IP）
- ✅ **Spring Profile = `docker`**：可通过此 profile 区分本地和容器内配置
- ✅ **JVM 调优**：`MaxRAMPercentage=75.0` 让容器自适应内存（不用写死 -Xmx）
- ✅ **非 root 运行**：容器内用 `app` 用户启动

### 常见操作

```bash
# 进入后端容器调试
docker compose exec backend sh

# 用本地 mysql 客户端连容器里的 mysql
docker compose exec mysql mysql -uroot -p minimall

# 重新构建后端镜像（代码改了之后）
docker compose build backend && docker compose up -d backend

# 查看资源占用
docker stats

# 清理悬空镜像（省空间）
docker image prune -f
```

### 方案 B：仅构建后端镜像（用宿主 MySQL/Redis）

```bash
cd backend
docker build -t minimall-backend .

docker run -d --name backend -p 8081:8081 \
  -e DB_HOST=<YOUR_DB_HOST> \
  -e DB_PASSWORD=<YOUR_DB_PASSWORD> \
  -e REDIS_HOST=<YOUR_REDIS_HOST> \
  -e REDIS_PASSWORD=<YOUR_REDIS_PASSWORD_OR_EMPTY> \
  -e DEEPSEEK_API_KEY=<YOUR_DEEPSEEK_API_KEY> \
  minimall-backend
```

### 方案 C：完整生产部署

```bash
# 后端 + 三大依赖（MySQL / Redis）走 docker compose
docker compose up -d mysql redis backend

# 三个前端 npm run build 产物用 Nginx 托管
cd admin-web && npm run build   # dist/
cd seller-web && npm run build
cd web-mall  && npm run build

# Nginx 反代配置参考 .devcontainer/init.sh
```

### 方案选择指南

| 场景 | 推荐 |
|---|---|
| 本地开发，需要 HMR | `start-all.bat`（不容器化前端） |
| 演示 / 答辩 / CI | **方案 A** compose |
| 服务器只有后端 | 方案 B（用宿主 MySQL/Redis） |
| 服务器全空 | 方案 C（compose + 前端 nginx） |

### 文件清单

```
.
├── docker-compose.yml          # 根级编排（MySQL+Redis+Backend）
├── .env.example                # 环境变量模板（不提交真 .env）
├── backend/
│   ├── Dockerfile              # 多阶段构建
│   └── .dockerignore           # 构建上下文过滤
├── start-docker.bat            # Windows 启动脚本
└── stop-docker.bat             # Windows 停止脚本
```

### 故障排查

| 现象 | 原因 | 解决 |
|---|---|---|
| `backend` 一直 `Restarting` | MySQL/Redis 没就绪 | `docker compose logs backend` 看具体报错 |
| 首次启动后表不存在 | `init_database.sql` 失败 | `docker compose logs mysql` 看 SQL 错误；删卷重启 `docker compose down -v && up -d` |
| `actuator/health` 502 | Spring 还没启动完 | 等 `start_period: 90s` 过去再查 |
| 端口被占用 | 本地已启 MySQL/Redis | 改 `.env` 中 `MYSQL_PORT` / `REDIS_PORT` / `SERVER_PORT` |
| 改了代码不生效 | 用了旧镜像 | `docker compose build --no-cache backend` 后再 `up -d` |

---

## 📊 数据库设计

### 核心表（36 张）

| 表名 | 说明 | 主要字段 |
|---|---|---|
| `user` | 用户表（含买家/商家/管理员） | id, username, phone, password, user_type, openid |
| `product` | 商品表 | id, name, price, stock, sales, category_id, seller_id, status |
| `category` | 商品分类（树形） | id, name, parent_id, sort |
| `product_spec` | 商品规格 | id, product_id, name, price, stock |
| `product_image` | 商品图片 | id, product_id, url, sort |
| `product_tag` | 商品标签 | id, name |
| `cart` | 购物车 | id, user_id, product_id, quantity, checked |
| `orders` | 订单主表 | id, order_no, user_id, total_price, status, address_id |
| `order_item` | 订单项 | id, order_id, product_id, quantity, price |
| `payment` | 支付记录 | id, order_id, amount, pay_status |
| `coupon` | 优惠券模板 | id, seller_id, name, threshold, discount_value, total_count |
| `user_coupon` | 用户领取的优惠券 | id, user_id, coupon_id, status, used_time |
| `activity` | 促销活动 | id, title, type, start_time, end_time |
| `discount_activity` | 满减/折扣活动 | id, name, threshold, discount_amount |
| `shipping_address` | 收货地址 | id, user_id, consignee, phone, address |
| `after_sale_service` | 售后服务 | id, order_id, type, reason, status |
| `product_review` | 商品评价 | id, product_id, user_id, content, rating |
| `chat_session` | 聊天会话 | id, user_id, agent_id, status |
| `chat_message` | 聊天消息 | id, session_id, from_user_id, content, type |
| `chat_notification` | 聊天通知 | id, user_id, content, is_read |
| `ai_service_log` | AI 调用日志 | id, user_id, query, response, service_type |
| `knowledge_document` | 知识库文档 | id, title, content, status |
| `knowledge_chunk` | 文档分块 | id, document_id, content, embedding |
| `knowledge_faq` | 知识库 FAQ | id, question, answer, embedding |
| `admin_intervention` | 人工介入记录 | id, session_id, reason, status |
| `logistics` | 物流 | id, order_id, company, tracking_no |
| `logistics_trace` | 物流轨迹 | id, logistics_id, trace, time |
| `role` | 角色 | id, code, name |
| `permission` | 权限 | id, code, name, type |
| `user_role` | 用户-角色关联 | user_id, role_id |
| `role_permission` | 角色-权限关联 | role_id, permission_id |
| `system_config` | 系统配置 | id, key, value |
| `service_record` | 服务记录 | id, user_id, type, content |

> 完整建表 SQL 见 [`backend/src/main/resources/sql/schema.sql`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/backend/src/main/resources/sql/schema.sql) 和 [`init_database.sql`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/backend/src/main/resources/sql/init_database.sql)

---

## 🤖 AI / RAG 模块

系统集成 DeepSeek 大模型 + 自研 RAG 检索增强，是答辩亮点。

### 流程图

```
用户提问 → POST /api/ai/chat
              ↓
       AIService.handleQueryStream()
              ↓
       ┌──────┴──────┐
       │             │
   开启 SSE     记录日志
   emitter=     (ai_service_log)
   new(60s)          ↓
       │       EmbeddingService.embed(query) → 向量(1536 维)
       │             ↓
       │       RagService.retrieve() → top-k=5 文档块
       │             ↓
       │       + FAQ top-k=3（intention 分类后路由）
       │             ↓
       │       组装 prompt：系统指令 + 上下文 + 历史(6 轮)
       │             ↓
       │       DeepSeek streamChat() → 逐 chunk 推
       │             ↓
       │       SseEmitter.send(chunk) → 浏览器 EventSource
       ↓             ↓
   返回 emitter    UPDATE ai_service_log SET response=完整文本
```

### 关键配置

`application.yml`：
```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
  model: deepseek-v4-flash
  temperature: 0.7

embedding:
  api-url: ${EMBEDDING_API_URL:https://api.deepseek.com/embeddings}
  model: text-embedding-3-small
  dimensions: 1536

rag:
  enabled: true
  chunk-size: 500
  chunk-overlap: 100
  top-k: 5
  faq-top-k: 3
  similarity-threshold: 0.65
  multi-turn-enabled: true
  conversation-history-turns: 6
```

### 降级策略

- 没配 `DEEPSEEK_API_KEY` → 返回 `ai.reply.common-queries` 静态文本（见 application.yml）
- 没配 `EMBEDDING_API_KEY` → 用本地 TF-IDF 降级
- RAG 关（`rag.enabled: false`）→ 直接调 DeepSeek

详见 [`docs/RAG_TECHNICAL_DOCUMENTATION.md`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/docs/RAG_TECHNICAL_DOCUMENTATION.md)

---

## 📊 可观测性

| 维度 | 实现 | 端点 |
|---|---|---|
| 健康检查 | Spring Boot Actuator | `GET /actuator/health` |
| 详细健康 | show-details: when-authorized | 需登录 |
| Prometheus 指标 | Micrometer | `GET /actuator/prometheus` |
| 应用指标 | /actuator/metrics | `/actuator/metrics/{name}` |
| 链路追踪 | TraceIdFilter + MDC | 日志中自动加 `traceId=` |
| 慢请求监控 | PerformanceInterceptor | 阈值 500ms 打 warn 日志 |
| SQL 日志 | logback + Slf4j | `org.apache.ibatis=DEBUG` 时开启 |
| API 日志 | ApiLoggingInterceptor | INFO 记录所有请求 |

**Prometheus 抓取示例**：
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'minimall'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
```

详见 [`docs/PERFORMANCE_REPORT.md`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/docs/PERFORMANCE_REPORT.md) 和 [`docs/SYSTEM_OPTIMIZATION_DOCUMENTATION.md`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/docs/SYSTEM_OPTIMIZATION_DOCUMENTATION.md)

---

## 🔧 开发指南

### 代码规范

#### 后端（Java）
- 遵循 **阿里巴巴 Java 开发手册**
- 使用 **Lombok** 减少样板代码
- Controller 只做参数校验 + 调 Service
- Service 处理业务 + 事务
- Mapper 只做数据访问
- 复杂查询写在 `resources/mapper/*.xml` 中
- 业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一处理

#### 前端（Vue 3）
- 使用 **Composition API**（`<script setup>`）
- 组件名 **PascalCase**
- API 统一在 `src/api/` 目录
- 全局组件自动导入（unplugin-auto-import）
- 样式 **scoped** 避免污染
- 多端复用组件放 `shared-ui/`

#### 小程序
- 页面独立目录（js/json/wxml/wxss 四个文件）
- 工具函数统一放 `utils/`
- 网络请求用 `wx.request` 封装

### Git 提交规范

使用 Conventional Commits：

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试
chore: 构建/工具链
perf: 性能优化
```

### 分支策略

```
main               ← 生产稳定
  ├─ develop       ← 开发主分支
  │   ├─ feature/xxx   ← 新功能
  │   ├─ fix/xxx       ← Bug 修复
  │   └─ refactor/xxx  ← 重构
```

### 🧪 测试

| 类型 | 工具 | 位置 |
|---|---|---|
| 单元测试 | JUnit 5 + Mockito | `backend/src/test/java/com/example/minimall/**/*Test.java` |
| 集成测试 | SpringBootTest + H2 内存库 | `backend/src/test/java/.../integration/` |
| WebSocket 端到端 | STOMP 客户端模拟 | `ChatMessageDeliveryE2ETest` |
| API 烟测（手动） | PowerShell | `backend/api_test.ps1` |
| 监控指标 | Micrometer Prometheus | `/actuator/prometheus` |

主要单测覆盖：`AIService`、`RagService`、`HnswIndex`、`EmbeddingService`、`IntentClassifierService`、`RagMonitorService`、`ContentFilterService`、`ProductContextOptimizer`、`ChatService`、`PermissionInterceptor`、`XssFilter`、`TraceIdFilter`、`GlobalExceptionHandler` 等。

---

## ❓ FAQ

### Q1: 后端启动失败，提示数据库连接错误？

**A:** 检查：
1. MySQL 已启动：`net start mysql` 或 `mysqld`
2. `application.yml` / `.env` 中 `DB_HOST` / `DB_PORT` / `DB_USERNAME` / `DB_PASSWORD` 正确（**不要把真实密码 commit 到 git**）
3. 数据库 `minimall` 已创建：`mysql -u root -p -e "CREATE DATABASE minimall"`
4. 已执行 `schema.sql` / `data.sql` / `init_database.sql`

### Q2: 前端无法连接后端 API（`ERR_CONNECTION_REFUSED`）？

**A:** 这是最常见的启动问题，原因有三个：
1. **后端根本没起来** —— 检查 `logs/backend.log` 是否有 `BUILD FAILURE` 或异常
   - **典型坑**：`AIService.java` 等源文件带了 **UTF-8 BOM** 会导致 Maven 编译失败
2. 后端编译成功但端口被占 —— `netstat -aon | findstr :8081`
3. 跨域：开发期 Vite 已配 proxy（`/api → :8081`），生产期需 Nginx

### Q3: Redis 连接失败？

**A:** `redis-server` 是否启动（端口 6379）；如设了密码，需在 `REDIS_PASSWORD` 配置。

### Q4: 商家创建优惠券报"卖家无权访问该接口"（403）？

**A:** 这是 `PermissionInterceptor` 的白名单 bug。已修复：SELLER_COUPON_PATTERN 放行 `POST /api/coupon`、`PUT/DELETE /api/coupon/{id}`。

详见 [`答辩准备.md` 九.1 节](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/%E7%AD%94%E8%BE%A9%E5%87%86%E5%A4%87.md)。

### Q5: 微信小程序无法请求后端？

**A:**
1. 微信小程序要求 HTTPS，开发期勾选"不校验合法域名"
2. 微信开发者工具 → 详情 → 本地设置 → 关闭域名校验
3. 后端 CORS 已在 `WebMvcConfig` 放行

### Q6: Swagger 打开 404？

**A:** 本项目用 Springfox 3.0.0，访问路径是：
```
http://localhost:8081/swagger-ui/index.html
```
**不是** `/swagger-ui.html`（Springfox 2.x 旧路径，3.x 已废弃）。
> 注：`start-all.bat` 启动提示中打印的是 `/swagger-ui.html`（历史遗留），正确路径见上。

### Q7: 如何重置 admin 密码？

**A:**
- 默认账号：`admin / <INITIAL_PASSWORD>`（启动脚本 / `.env.example` 提示，**首次登录后请立即修改**）
- 用初始账号登录后改密码，或直接 SQL：
  ```sql
  -- 用 BCrypt 在线工具生成新密码哈希后更新
  UPDATE user SET password='<BCRYPT_HASH_OF_NEW_PASSWORD>' WHERE username='admin';
  ```
- **务必把 `<INITIAL_PASSWORD>` 和 `<BCRYPT_HASH_OF_NEW_PASSWORD>` 替换为实际值后再执行**

### Q8: 图片上传失败？

**A:**
1. `data/uploads/` 目录存在且有写权限
2. 单文件 ≤ 10MB（`application.yml` 中 `upload.max-size`）
3. 支持格式：JPEG, PNG, GIF, WebP
4. 系统自动用 Thumbnailator 压缩到 1920×1920

### Q9: 日志全是乱码？

**A:** Windows 下 `start-all.bat` 默认 `chcp 936` (GBK)，但项目文件是 UTF-8。改 bat 第一行 `chcp 65001` 即可。详见 [`start-all.bat`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/start-all.bat)。

### Q10: 如何切到生产环境？

**A:**
1. 后端打包：`mvn clean package -DskipTests`，丢给 Docker / 服务器
2. 前端：`npm run build`，产物 `dist/` 部署到 Nginx
3. 修改 `application.yml` 中的生产配置（数据库、Redis、AI Key）
4. 配 HTTPS 证书（小程序强制要求）

---

## 📚 相关文档

| 文档 | 用途 |
|---|---|
| [`答辩准备.md`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/%E7%AD%94%E8%BE%A9%E5%87%86%E5%A4%87.md) | 毕业答辩 Q&A（113 个高频问题 + 10 大流程图 + 7 大数据流追踪） |
| [`docs/PERFORMANCE_REPORT.md`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/docs/PERFORMANCE_REPORT.md) | 性能压测报告（JMeter） |
| [`docs/RAG_TECHNICAL_DOCUMENTATION.md`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/docs/RAG_TECHNICAL_DOCUMENTATION.md) | RAG 技术详解 |
| [`docs/SYSTEM_OPTIMIZATION_DOCUMENTATION.md`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/docs/SYSTEM_OPTIMIZATION_DOCUMENTATION.md) | 系统优化说明 |
| [`docs/usecase-*.svg`](file:///e:/%E8%BF%85%E9%9B%B7%E4%B8%8B%E8%BD%BD/mall_system_extended/docs/) | 5 个用例图（订单/商品/用户/AI/售后） |

---

## 🤝 贡献指南

1. **Fork** 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: 添加 xxx 功能'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 **Pull Request**

### 贡献类型

- 🐛 Bug 报告（Issues 附复现步骤）
- 💡 功能建议（说明使用场景）
- 📝 文档改进
- ✨ 代码贡献
- 🎨 UI 优化

---

## 📄 许可证

本项目采用 **MIT License** 开源。

```
MIT License

Copyright (c) 2024 乡村振兴团队

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
```

---

## 🙏 致谢

- [Spring](https://spring.io/) - 企业级 Java 框架
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Element Plus](https://element-plus.org/) - Vue 3 组件库
- [MyBatis-Plus](https://baomidou.com/) - MyBatis 增强
- [DeepSeek](https://platform.deepseek.com/) - AI 大模型
- [ECharts](https://echarts.apache.org/) - 数据可视化

---

## 📈 路线图

### ✅ 已完成
- [x] 完整电商核心（商品/订单/支付/售后/营销）
- [x] 多端（Web × 3 + 小程序 + 共享 UI）
- [x] AI 客服 + RAG 知识库
- [x] STOMP 实时聊天（3 角色）
- [x] RBAC 权限
- [x] 可观测性（Actuator + Prometheus + TraceId）
- [x] 一键启动脚本

### 🚧 计划中
- [ ] 分布式微服务（Spring Cloud）
- [ ] 物流真实 API 对接
- [ ] 直播带货
- [ ] 区块链溯源
- [ ] 多语言国际化

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star！⭐**

Made with ❤️ by 乡村振兴团队

</div>
