# Admin-Web

商城管理后台系统，基于 Vue 3 + TypeScript + Element Plus + Vite 构建。

## 技术栈

- Vue 3.3+
- TypeScript 5.3+
- Element Plus 2.4+
- Vite 5.0+
- Pinia 2.1+
- Vue Router 4.2+
- Axios 1.6+
- ECharts 5.4+

## 项目结构

```
admin-web/
├── src/
│   ├── api/              # API接口
│   │   ├── auth.ts       # 认证相关接口
│   │   └── product.ts    # 商品相关接口
│   ├── assets/            # 静态资源
│   ├── components/         # 公共组件
│   ├── layouts/           # 布局组件
│   │   └── index.vue     # 主布局
│   ├── router/            # 路由配置
│   │   └── index.ts
│   ├── stores/            # 状态管理
│   │   └── user.ts       # 用户状态
│   ├── styles/            # 样式文件
│   ├── utils/             # 工具函数
│   │   └── request.ts    # 请求封装
│   ├── views/             # 页面组件
│   │   ├── login/         # 登录页
│   │   ├── dashboard/     # 仪表盘
│   │   ├── product/       # 商品管理
│   │   ├── order/         # 订单管理
│   │   ├── user/          # 用户管理
│   │   ├── activity/      # 活动管理
│   │   ├── aftersale/     # 售后管理
│   │   └── system/        # 系统设置
│   ├── App.vue
│   └── main.ts
├── public/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 功能模块

### 1. 登录认证
- 管理员登录
- JWT Token 认证
- 自动登录状态保持

### 2. 仪表盘
- 数据统计卡片
- 销售趋势图表
- 商品分类统计

### 3. 商品管理
- 商品列表
- 商品搜索
- 商品添加/编辑/删除
- 商品上下架
- 库存管理

### 4. 订单管理
- 订单列表
- 订单详情
- 订单状态管理
- 发货管理

### 5. 用户管理
- 用户列表
- 用户详情
- 用户状态管理
- 商家审核

### 6. 活动管理
- 活动列表
- 活动创建/编辑
- 活动参与情况

### 7. 售后管理
- 售后申请列表
- 售后处理
- 售后状态管理

### 8. 系统设置
- 系统配置
- 日志查看
- AI服务配置

## 快速开始

### 安装依赖

```bash
cd admin-web
npm install
```

### 开发环境运行

```bash
npm run dev
```

访问地址：http://localhost:3001

### 生产环境构建

```bash
npm run build
```

构建产物在 `dist` 目录

### 预览构建结果

```bash
npm run preview
```

## 环境变量

创建 `.env` 文件配置环境变量：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## API接口

所有API接口定义在 `src/api/` 目录下，通过 `request.ts` 统一封装。

### 请求拦截

- 自动添加 JWT Token 到请求头
- 统一错误处理
- 自动显示错误提示

### 响应拦截

- 统一处理响应数据
- 自动处理 401 未授权
- 统一错误提示

## 路由配置

路由配置在 `src/router/index.ts` 中：

- 登录页：`/login`
- 主布局：`/` (包含所有管理页面)
- 路由守卫：自动检查登录状态

## 状态管理

使用 Pinia 进行状态管理：

- `useUserStore`：用户状态（token、用户信息）
- 支持持久化存储到 localStorage

## 样式定制

全局样式在 `src/styles/index.css` 中，可以自定义主题颜色等。

## 浏览器支持

- Chrome (推荐)
- Firefox
- Safari
- Edge

## 开发建议

1. **代码规范**：遵循 Vue 3 Composition API 风格
2. **类型安全**：充分利用 TypeScript 类型检查
3. **组件复用**：提取公共组件到 `components/` 目录
4. **性能优化**：合理使用 `v-if` 和 `v-show`
5. **错误处理**：统一使用 try-catch 处理异步错误

## 常见问题

### 1. 接口请求失败
检查后端服务是否启动，以及 API 地址配置是否正确。

### 2. 登录后跳转失败
检查 localStorage 中是否正确存储了 token。

### 3. 页面样式异常
检查 Element Plus 样式是否正确引入。

## License

MIT
