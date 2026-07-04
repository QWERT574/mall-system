# 🛒 乡村振兴 - 乡村振兴农产品电商平台

<p align="center">
  <strong>一个功能完善的现代化多端电商平台系统</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-2.7.9-green" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue.js-3.x-blue" alt="Vue.js">
  <img src="https://img.shields.io/badge/MySQL-8.0-orange" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7.x-red" alt="Redis">
  <img src="https://img.shields.io/badge/微信小程序-原生开发-09B83E" alt="WeChat Mini Program">
</p>

---

## 📋 项目概述

**乡村振兴** 是一个面向乡村振兴的综合性农产品电商平台，采用前后端分离架构，支持多端访问（Web端、商家端、管理后台、微信小程序）。系统整合了商品管理、订单处理、在线支付、智能客服、售后服务等核心电商功能，并融入了 AI 智能推荐和实时聊天等创新特性。

### ✨ 核心特性

- **🎯 多端支持**：用户 Web 端、商家管理端、后台管理系统、微信小程序四端互通
- **🤖 AI 智能服务**：集成 DeepSeek AI 提供智能客服和商品推荐
- **💬 实时通讯**：基于 WebSocket + STOMP 协议的即时聊天系统
- **🔐 安全可靠**：JWT 认证 + Spring Security 权限控制
- **⚡ 高性能**：Redis 缓存 + 连接池优化 + 图片压缩处理
- **📱 移动优先**：响应式设计 + 微信小程序原生体验

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
├─────────────┬─────────────┬──────────────┬─────────────────┤
│   web-mall  │  seller-web │   admin-web  │  mini-program   │
│  (用户商城)  │  (商家管理)  │ (后台管理)    │  (微信小程序)     │
│  Vue3+Vite  │ Vue3+Vite   │ Vue3+ViteTS  │   原生小程序      │
└──────┬──────┴──────┬──────┴──────┬───────┴────────┬────────┘
       │             │             │                │
       └─────────────┴──────┬──────┴────────────────┘
                           │ HTTP/WebSocket
                    ┌──────▼──────┐
                    │   Nginx     │
                    │  (反向代理)   │
                    └──────┬──────┘
                           │
              ┌────────────▼────────────┐
              │      Backend 后端服务     │
              │   Spring Boot 2.7.9     │
              │   Port: 8081            │
              └──────┬────────┬─────────┘
                     │        │
            ┌────────▼┐  ┌────▼─────┐
            │  MySQL  │  │  Redis   │
            │  8.0.33 │  │  缓存服务  │
            └─────────┘  └──────────┘
```

---

## 📦 模块说明

### 1️⃣ backend - 后端服务 [Java/Spring Boot]

**技术栈：**
- **框架**：Spring Boot 2.7.9 + Spring Security + Spring Data JPA
- **ORM**：MyBatis-Plus 3.5.3.1（增强版 MyBatis）
- **数据库**：MySQL 8.0.33 + HikariCP 连接池
- **缓存**：Redis（Lettuce 客户端）+ Spring Cache
- **认证**：JWT (jjwt 0.11.5) + Spring Security
- **API 文档**：Swagger 3.0 (SpringFox)
- **实时通信**：WebSocket + STOMP 协议 + SockJS
- **AI 集成**：DeepSeek API (智能客服)
- **工具库**：Lombok, Thumbnailator (图片压缩), Apache HttpClient

**核心功能模块：**
- 👤 用户认证与权限管理（RBAC 角色权限）
- 📦 商品管理（分类、规格、标签、图片）
- 🛒 购物车与订单系统
- 💳 支付集成与退款处理
- 🎫 优惠券与促销活动
- 💬 在线客服聊天系统（AI + 人工）
- 🔧 售后服务与工单管理
- 📊 数据统计与分析
- ⚙️ 系统配置与管理

**项目结构：**
```
backend/
├── src/main/java/com/example/minimall/
│   ├── annotation/          # 自定义注解（限流等）
│   ├── common/              # 公共类（统一返回结果）
│   ├── config/              # 配置类（安全、上传、AI、Redis）
│   ├── context/             # 应用上下文
│   ├── dto/                 # 数据传输对象
│   ├── enums/               # 枚举定义
│   ├── filter/              # 过滤器（XSS防护）
│   ├── mapper/              # MyBatis Mapper 接口
│   ├── model/               # 数据库实体类
│   ├── security/            # 安全工具类
│   ├── service/             # 业务逻辑层
│   ├── utils/               # 工具类（JWT等）
│   └── vo/                  # 视图对象
├── src/main/resources/
│   ├── mapper/              # MyBatis XML 映射文件
│   ├── sql/                 # 数据库脚本（初始化、测试数据）
│   ├── static/images/       # 静态资源图片
│   └── application.yml      # 主配置文件
└── pom.xml                  # Maven 配置
```

**运行端口**：`8081`

---

### 2️⃣ admin-web - 管理后台 [Vue 3 + TypeScript]

**技术栈：**
- **前端框架**：Vue 3.3.4 + TypeScript 5.3.3
- **构建工具**：Vite 5.0.8
- **UI 组件库**：Element Plus 2.4.4
- **状态管理**：Pinia 2.1.7
- **路由**：Vue Router 4.2.5
- **HTTP 客户端**：Axios 1.6.2
- **图表库**：ECharts 5.4.3
- **实时通信**：STOMP.js 7.3.0 + SockJS
- **日期处理**：Day.js 1.11.10
- **CSS 预处理器**：Sass

**主要功能页面：**
| 路由路径 | 功能模块 | 说明 |
|---------|---------|------|
| `/login` | 登录页 | 管理员身份验证 |
| `/dashboard` | 仪表盘 | 数据概览与统计图表 |
| `/product` | 商品管理 | 商品 CRUD、上下架 |
| `/order` | 订单管理 | 订单查询、状态管理 |
| `/user` | 用户管理 | 用户信息、角色分配 |
| `/activity` | 活动管理 | 促销活动配置 |
| `/aftersale` | 售后管理 | 售后工单处理 |
| `/system` | 系统设置 | 参数配置 |
| `/seller` | 商家审核 | 商家入驻审核 |
| `/customer-service` | 客服聊天 | 在线客服对话界面 |
| `/intervention` | 人工介入 | 复杂问题处理 |
| `/agent-management` | 客服管理 | 客服人员管理 |

**项目特色：**
- ✅ 完整的后台管理系统
- ✅ 实时客服聊天功能（支持会话转接、FAQ面板、评分）
- ✅ 数据可视化仪表盘
- ✅ 自动导入组件和 API（unplugin）

**开发端口**：`3001` → 代理到 `8081`

---

### 3️⃣ seller-web - 商家管理端 [Vue 3]

**技术栈：**
- **前端框架**：Vue 3.2.0
- **构建工具**：Vite 4.0.0
- **UI 组件库**：Element Plus 2.4.4
- **状态管理**：Pinia 3.0.4
- **其他依赖**：ECharts, Axios, STOMP.js

**核心功能模块：**

| 模块 | 路径 | 功能描述 |
|------|------|---------|
| **认证模块** | `/auth/login`, `/auth/register` | 商家登录注册、密码重置 |
| **仪表盘** | `/dashboard` | 销售数据统计、订单概览 |
| **商品管理** | `/product/*` | 商品列表、添加编辑、规格管理 |
| **订单管理** | `/order/*` | 订单列表、详情、发货处理 |
| **售后处理** | `/aftersale` | 退换货申请审核 |
| **营销工具** | `/coupon`, `/discount` | 优惠券、折扣活动 |
| **客户服务** | `/customer-service/chat` | 与买家实时沟通 |
| **评价管理** | `/review` | 商品评价查看与回复 |
| **个人中心** | `/profile` | 店铺信息、账户设置 |

**项目亮点：**
- 📊 ECharts 数据可视化展示销售趋势
- 💬 内置客服聊天系统
- 🎨 Element Plus 企业级 UI 组件
- 🔐 完整的商家认证流程

---

### 4️⃣ web-mall - 用户商城前端 [Vue 3]

**技术栈：**
- **前端框架**：Vue 3.2.0
- **构建工具**：Vite 4.0.0
- **UI 组件库**：Element Plus 2.4.4
- **状态管理**：Pinia 3.0.4
- **路由**：Vue Router 4.6.4
- **安全工具**：自定义 security.js（加密、token 管理）

**用户端完整功能：**

#### 🏠 首页与浏览
- **首页** (`/`) - Banner轮播、推荐商品、分类入口
- **商品列表** (`/product/list`) - 分类筛选、搜索、排序
- **商品详情** (`/product/detail/:id`) - 规格选择、加入购物车、立即购买

#### 🛒 购物车与结算
- **购物车** (`/cart`) - 商品数量调整、删除、全选、价格计算
- **结算支付** (`/order/payment`) - 地址选择、优惠券使用、订单提交

#### 📦 订单管理
- **订单列表** (`/order/list`) - 全部订单、待付款、待收货、已完成
- **订单详情** (`/order/:id`) - 物流跟踪、确认收货、申请售后

#### 👤 个人中心
- **登录注册** (`/auth/login`, `/auth/register`)
- **个人信息** (`/profile`) - 头像上传、资料修改
- **地址管理** (`/address`) - 收货地址增删改查
- **密码重置** (`/auth/reset-password`)

#### 🎉 营销活动
- **优惠活动** (`/discount`) - 限时折扣、满减活动
- **优惠券中心** (`/coupon`) - 领取优惠券、我的优惠券

#### 💬 特色功能
- **智能客服** (`/service/chat`) - AI 客服 + 人工客服
- **AI 助手** (`/ai`) - DeepSeek AI 智能问答
- **售后服务** (`/after-sale/*`) - 申请售后、进度跟踪
- **商品评价** (`/review/order-review`) - 订单评价

**环境配置示例 (.env.development)：**
```env
VITE_API_BASE_URL=http://localhost:8081/api
VITE_REQUEST_TIMEOUT=10000
VITE_UPLOAD_MAX_SIZE=5242880
VITE_PAGE_SIZE=20
```

---

### 5️⃣ mini-program - 微信小程序 [原生开发]

**技术栈：**
- **开发语言**：JavaScript (ES6+)
- **框架**：微信小程序原生框架
- **AppID**：wxa17d14480861589e
- **UI 风格**：自定义 WXSS 样式

**应用名称**：**乡村振兴**

**TabBar 页面配置：**
| 图标 | 页面路径 | 功能 |
|------|---------|------|
| 🏠 | `pages/home/home` | 首页 |
| 📂 | `pages/category/category` | 分类浏览 |
| 🛒 | `pages/cart/cart` | 购物车 |
| 🎫 | `pages/coupon/index` | 优惠中心 |
| 👤 | `pages/user/index` | 个人中心 |

**完整页面清单：**

| 页面 | 路径 | 功能说明 |
|------|------|---------|
| 首页 | `pages/home/home` | 推荐商品、Banner、搜索入口 |
| 分类 | `pages/category/category` | 商品分类树形展示 |
| 商品详情 | `pages/product/product` | 商品信息、规格选择、购买 |
| 购物车 | `pages/cart/cart` | 购物车管理、结算 |
| 订单列表 | `pages/order/list` | 我的订单列表 |
| 订单详情 | `pages/order/order` | 订单详细信息 |
| 个人中心 | `pages/user/index` | 用户信息、功能入口 |
| 地址管理 | `pages/user/address` | 收货地址 CRUD |
| 个人资料 | `pages/user/profile` | 头像昵称修改 |
| AI助手 | `pages/ai/ai` | AI 智能问答 |
| 售后列表 | `pages/aftersale/list` | 售后记录 |
| 创建售后 | `pages/aftersale/create` | 发起售后申请 |
| 售后详情 | `pages/aftersale/detail` | 售后进度查看 |
| 优惠券 | `pages/coupon/index` | 优惠券领取和使用 |
| 折扣活动 | `pages/discount/index` | 促销活动展示 |

**核心工具函数：**
- **请求封装** (`utils/request.js`) - 统一 API 请求、拦截器
- **支付功能** (`utils/pay.js`) - 微信支付集成
- **上传功能** (`utils/upload.js`) - 图片上传处理

**窗口配置：**
```json
{
  "navigationBarTitleText": "乡村振兴",
  "navigationBarBackgroundColor": "#ffffff",
  "navigationBarTextStyle": "black",
  "backgroundColor": "#f9fafb"
}
```

---

## 🛠️ 技术栈总览

### 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 8 | 编程语言 |
| Spring Boot | 2.7.9 | 应用框架 |
| Spring Security | 5.7.x | 安全框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 框架 |
| MySQL | 8.0.33 | 关系型数据库 |
| Redis | 7.x | 缓存数据库 |
| JWT | 0.11.5 | Token 认证 |
| Swagger | 3.0 | API 文档 |
| WebSocket | - | 实时通信 |
| Lombok | 1.18.30 | 代码简化 |

### 前端技术栈（Web端）
| 技术 | 版本 | 使用场景 |
|------|------|---------|
| Vue.js | 3.x | 前端框架 |
| Vite | 4.x/5.x | 构建工具 |
| Element Plus | 2.4.4 | UI 组件库 |
| Pinia | 2.x/3.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 客户端 |
| ECharts | 5.4.3 | 数据可视化 |
| STOMP.js | 7.3.0 | WebSocket 客户端 |
| Sass | - | CSS 预处理 |

### 小程序技术栈
| 技术 | 说明 |
|------|------|
| 微信小程序原生框架 | 小程序基础框架 |
| WXML/WXSS | 页面结构和样式 |
| JavaScript ES6+ | 业务逻辑 |
| 微信支付 API | 支付功能 |
| 微信登录 API | 用户认证 |

---

## 🚀 快速开始

### 环境要求

- **Node.js** >= 16.0.0（推荐 18.x LTS）
- **Java JDK** = 8（必须为 JDK 8）
- **Maven** >= 3.8.0
- **MySQL** >= 8.0
- **Redis** >= 6.0
- **微信开发者工具**（仅小程序开发需要）

### 1️⃣ 克隆项目

```bash
git clone <repository-url>
cd mall_system_extended
```

### 2️⃣ 数据库初始化

```bash
# 进入后端目录
cd backend

# 执行数据库初始化脚本（按顺序执行）
mysql -u root -p < src/main/resources/sql/schema.sql
mysql -u root -p < src/main/resources/sql/data.sql
mysql -u root -p < src/main/resources/sql/init_database.sql
```

**或使用 MySQL 客户端工具导入 SQL 文件。**

### 3️⃣ 启动 Redis 服务

```bash
# Windows (需先安装 Redis)
redis-server

# 或使用 Docker
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 4️⃣ 配置环境变量

在 `backend` 目录下创建 `.env` 文件：

```env
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=minimall
DB_USERNAME=root
DB_PASSWORD=你的密码

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 服务端口
SERVER_PORT=8081

# DeepSeek AI 配置（可选）
DEEPSEEK_API_KEY=your-api-key
```

### 5️⃣ 启动后端服务

```bash
cd backend

# 方式一：Maven 运行（推荐）
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/minimall-0.0.1-SNAPSHOT.jar
```

✅ **验证后端启动成功：**
- 访问：http://localhost:8081
- Swagger 文档：http://localhost:8081/swagger-ui.html

### 6️⃣ 安装前端依赖

```bash
# 安装所有前端项目的依赖
npm install --prefix admin-web
npm install --prefix seller-web
npm install --prefix web-mall
```

### 7️⃣ 启动前端服务

#### 管理后台 (admin-web)

```bash
cd admin-web
npm run dev
```

访问：http://localhost:3001

默认管理员账号：`admin / admin123`

#### 商家端 (seller-web)

```bash
cd seller-web
npm run dev
```

访问：http://localhost:5173（或查看终端输出）

#### 用户商城 (web-mall)

```bash
cd web-mall
npm run dev
```

访问：http://localhost:5174（或查看终端输出）

### 8️⃣ 启动微信小程序

1. 安装 **微信开发者工具**
2. 打开开发者工具，导入 `mini-program` 目录
3. 在 `project.config.json` 中填入您的 AppID（或使用测试号）
4. 点击编译按钮即可预览

---

## 📁 项目目录结构

```
mall_system_extended/
│
├── backend/                     # 后端服务（Spring Boot）
│   ├── src/main/java/
│   │   └── com/example/minimall/
│   │       ├── controller/      # REST API 控制器
│   │       ├── service/         # 业务逻辑层
│   │       ├── mapper/          # 数据访问层
│   │       ├── model/           # 实体类
│   │       ├── config/          # 配置类
│   │       ├── dto/             # 数据传输对象
│   │       ├── vo/              # 视图对象
│   │       ├── utils/           # 工具类
│   │       └── security/        # 安全相关
│   ├── src/main/resources/
│   │   ├── mapper/              # MyBatis XML
│   │   ├── sql/                 # 数据库脚本
│   │   ├── static/              # 静态资源
│   │   └── application.yml      # 配置文件
│   ├── lib/                     # 本地依赖 JAR 包
│   ├── pom.xml                  # Maven 配置
│   └── Dockerfile               # Docker 构建文件
│
├── admin-web/                   # 管理后台前端（Vue 3 + TS）
│   ├── src/
│   │   ├── api/                 # API 接口封装
│   │   ├── views/               # 页面组件
│   │   ├── components/          # 公共组件
│   │   ├── stores/              # Pinia 状态管理
│   │   ├── router/              # 路由配置
│   │   ├── composables/         # 组合式函数
│   │   ├── layouts/             # 布局组件
│   │   ├── styles/              # 全局样式
│   │   └── utils/               # 工具函数
│   ├── package.json
│   └── vite.config.ts
│
├── seller-web/                  # 商家管理端（Vue 3）
│   ├── src/
│   │   ├── views/               # 页面视图
│   │   ├── components/          # 组件
│   │   ├── stores/              # 状态管理
│   │   ├── router/              # 路由
│   │   ├── utils/               # 工具
│   │   └── layouts/             # 布局
│   ├── public/images/           # 静态图片资源
│   └── package.json
│
├── web-mall/                    # 用户商城前端（Vue 3）
│   ├── src/
│   │   ├── views/               # 页面（按功能模块组织）
│   │   ├── components/          # 可复用组件
│   │   ├── stores/              # 状态管理（cart, user）
│   │   ├── router/              # 路由配置
│   │   ├── utils/               # 工具函数（api, security）
│   │   └── composables/         # 组合式函数
│   ├── public/images/           # 商品图片资源
│   ├── .env.development         # 开发环境变量
│   └── .env.production          # 生产环境变量
│
├── mini-program/                # 微信小程序
│   ├── pages/                   # 页面目录
│   │   ├── home/                # 首页
│   │   ├── category/            # 分类
│   │   ├── product/             # 商品
│   │   ├── cart/                # 购物车
│   │   ├── order/               # 订单
│   │   ├── user/                # 用户中心
│   │   ├── ai/                  # AI助手
│   │   ├── aftersale/           # 售后
│   │   ├── coupon/              # 优惠券
│   │   └── discount/            # 折扣活动
│   ├── utils/                   # 工具函数
│   ├── app.js                   # 小程序入口
│   ├── app.json                 # 全局配置
│   └── project.config.json      # 项目配置
│
├── docker-compose.yml           # Docker 编排配置
├── docs/                        # 项目文档
└── README.md                    # 本文件
```

---

## 🔌 API 接口文档

### Swagger UI 访问地址

启动后端服务后，访问以下地址查看完整的 API 文档：

**本地环境：**
```
http://localhost:8081/swagger-ui.html
```

### 主要 API 模块

| 模块 | 基础路径 | 功能说明 |
|------|---------|---------|
| 认证接口 | `/api/auth/**` | 登录、注册、Token刷新 |
| 用户接口 | `/api/users/**` | 用户信息、地址管理 |
| 商品接口 | `/api/products/**` | 商品CRUD、分类、搜索 |
| 订单接口 | `/api/orders/**` | 订单创建、查询、取消 |
| 购物车 | `/api/cart/**` | 购物车操作 |
| 支付接口 | `/api/payment/**` | 支付、退款 |
| 优惠券 | `/api/coupons/**` | 优惠券领取、使用 |
| 活动接口 | `/api/activities/**` | 促销活动 |
| 售后接口 | `/api/aftersales/**` | 售后申请、处理 |
| 聊天接口 | `/api/chat/**` | 客服消息、会话管理 |
| 上传接口 | `/api/upload/**` | 文件上传 |
| 系统接口 | `/api/system/**` | 系统配置 |

### 认证方式

所有需要认证的接口需要在请求头中携带 JWT Token：

```http
Authorization: Bearer <your-jwt-token>
```

**获取 Token：**
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

---

## 🐳 Docker 部署（可选）

项目提供 Docker 支持，可快速部署整个系统：

### 使用 docker-compose

```bash
# 在项目根目录执行
docker-compose up -d
```

这将自动启动：
- MySQL 8.0 数据库
- Redis 缓存服务
- Spring Boot 后端应用

### 单独构建后端镜像

```bash
cd backend
docker build -t minimall-backend .
docker run -d -p 8081:8081 \
  -e DB_HOST=host.docker.internal \
  -e REDIS_HOST=host.docker.internal \
  minimall-backend
```

---

## 📊 数据库设计

### 核心数据表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `users` | 用户表 | id, username, password, phone, role |
| `products` | 商品表 | id, name, price, stock, category_id, seller_id |
| `categories` | 分类表 | id, name, parent_id, level |
| `orders` | 订单表 | id, user_id, total_amount, status, order_no |
| `order_items` | 订单项表 | id, order_id, product_id, quantity, price |
| `cart` | 购物车表 | id, user_id, product_id, quantity |
| `coupons` | 优惠券表 | id, name, discount, valid_start, valid_end |
| `activities` | 活动表 | id, title, type, discount, status |
| `chat_sessions` | 会话表 | id, user_id, agent_id, status |
| `chat_messages` | 消息表 | id, session_id, sender_type, content |
| `aftersales` | 售后表 | id, order_id, reason, status |

### 数据库初始化脚本位置

```
backend/src/main/resources/sql/
├── schema.sql              # 数据库建表语句
├── init_database.sql       # 初始数据
├── data.sql                # 测试数据
├── add_test_data.sql       # 补充测试数据
└── after_sale_system.sql   # 售后系统相关表
```

---

## 🔧 开发指南

### 代码规范

#### 后端 (Java)
- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 减少样板代码
- Controller 层只做参数校验和调用 Service
- Service 层处理业务逻辑
- Mapper/DAO 层只做数据访问

#### 前端 (Vue 3)
- 使用 Composition API (`<script setup>`)
- 组件命名采用 PascalCase
- 使用 Pinia 进行状态管理
- API 统一封装在 `src/api/` 目录
- 样式使用 scoped 避免污染

#### 小程序
- 页面独立目录，包含四个文件（js/json/wxml/wxss）
- 使用 Promise 封装微信 API
- 工具函数统一放在 `utils/` 目录

### Git 提交规范

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 测试相关
chore: 构建/工具链
perf: 性能优化
```

示例：
```bash
git commit -m "feat: 添加商品搜索功能"
git commit -m "fix: 修复购物车数量计算错误"
```

### 分支策略

```
main          ← 生产环境稳定版本
  ├─ develop  ← 开发主分支
  │   ├─ feature/xxx    ← 新功能分支
  │   ├─ fix/xxx        ← Bug 修复分支
  │   └─ hotfix/xxx     ← 紧急修复分支
```

---

## ❓ 常见问题解答 (FAQ)

### Q1: 后端启动失败，提示数据库连接错误？

**A:** 请检查以下几点：
1. MySQL 服务是否已启动
2. `application.yml` 中的数据库配置是否正确
3. 数据库名称 `minimall` 是否已创建
4. 用户名密码是否匹配

```bash
# 测试数据库连接
mysql -u root -p -h localhost -e "SELECT 1"
```

### Q2: 前端无法连接后端 API？

**A:** 检查代理配置：
1. 确认后端服务运行在 `8081` 端口
2. 检查 Vite 配置中的 proxy 设置
3. 查看 `.env.development` 中的 `VITE_API_BASE_URL`

### Q3: Redis 连接失败？

**A:**
1. 确保 Redis 服务已启动：`redis-server`
2. 检查 Redis 端口是否为 `6379`
3. 如有密码，请在 `.env` 中配置 `REDIS_PASSWORD`

### Q4: 小程序无法请求后端接口？

**A:**
1. 微信小程序要求 HTTPS，开发阶段需勾选"不校验合法域名"
2. 在微信开发者工具 → 详情 → 本地设置中关闭域名校验
3. 确保后端已配置 CORS 跨域

### Q5: 如何重置管理员密码？

**A:** 直接修改数据库或使用初始账号：
- 默认管理员：`admin / admin123`
- 可通过 SQL 重置：`UPDATE users SET password='新密码' WHERE username='admin'`

### Q6: 图片上传失败？

**A:**
1. 检查 `uploads/` 目录是否存在且有写入权限
2. 确认图片大小不超过 10MB
3. 支持格式：JPEG, PNG, GIF, WebP

### Q7: Swagger 无法访问？

**A:**
1. 确认已添加 Swagger 依赖（pom.xml）
2. 检查 Security 配置是否放行了 swagger 路径
3. 访问地址：`http://localhost:8081/swagger-ui.html`

### Q8: 如何切换到生产环境？

**A:**
1. 修改 `.env.production` 中的 API 地址
2. 前端执行 `npm run build` 打包
3. 将 `dist` 目录部署到 Nginx
4. 后端修改 `application.yml` 中的生产环境配置

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！无论是新功能、Bug 修复、文档改进还是问题反馈。

### 如何贡献

1. **Fork** 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 **Pull Request**

### 贡献类型

- 🐛 **Bug 报告**：通过 Issues 提交详细的问题描述和复现步骤
- 💡 **功能建议**：提出新功能想法并说明使用场景
- 📝 **文档改进**：修正错误、补充说明、翻译文档
- ✨ **代码贡献**：实现新功能或修复已知问题
- 🎨 **UI 优化**：改进界面设计和用户体验

### 开发流程

1. 先讨论大范围改动（通过 Issue）
2. 遵循现有代码风格
3. 确保所有测试通过
4. 更新相关文档
5. 提交清晰的 PR 描述

---

## 📄 许可证

本项目采用 **MIT License** 开源许可证。

```
MIT License

Copyright (c) 2024 乡村振兴团队

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 联系方式

- **项目仓库**：<GitHub Repository URL>
- **问题反馈**：[Issues](../../issues)
- **讨论交流**：[Discussions](../../discussions)
- **邮箱**：contact@example.com

---

## 🙏 致谢

感谢以下开源项目和社区：

- [Spring](https://spring.io/) - 企业级 Java 开发框架
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Element Plus](https://element-plus.org/) - 基于 Vue 3 的组件库
- [MyBatis-Plus](https://baomidou.com/) - 强大的 MyBatis 增强工具
- [MySQL](https://www.mysql.com/) - 开源关系型数据库
- [Redis](https://redis.io/) - 高性能内存数据库
- [ECharts](https://echarts.apache.org/) - 数据可视化图表库
- [DeepSeek](https://platform.deepseek.com/) - AI 大模型服务

---

## 📈 项目路线图

### ✅ 已完成功能
- [x] 用户认证与权限系统
- [x] 商品管理与分类
- [x] 购物车与订单系统
- [x] 在线支付集成
- [x] 客服聊天系统（AI + 人工）
- [x] 售后服务模块
- [x] 优惠券与促销活动
- [x] 多端适配（Web + 小程序）
- [x] 管理后台完整功能
- [x] 商家管理端

### 🚧 开发中功能
- [ ] 物流追踪对接
- [ ] 数据分析报表增强
- [ ] 消息推送通知
- [ ] 商品评价系统优化

### 🎯 未来规划
- [ ] 移动 App（React Native/Flutter）
- [ ] 多语言国际化支持
- [ ] 分布式微服务架构升级
- [ ] 区块链溯源功能
- [ ] 直播带货功能
- [ ] 社交电商功能

---

<div align="center">

**⭐ 如果这个项目对您有帮助，请给一个 Star！⭐**

 Made with ❤️ by 乡村振兴团队

</div>

