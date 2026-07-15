# 乡村振兴 — 统一 UI 设计规范文档 v1.0

> 适用范围：admin-web / seller-web / web-mall / mini-program  
> 设计理念：有机自然 · 温暖信赖 · 乡村振兴  
> 最后更新：2026-05-21

---

## 目录

1. [色彩系统](#一色彩系统)
2. [排版规范](#二排版规范)
3. [组件库设计标准](#三组件库设计标准)
4. [图标系统](#四图标系统)
5. [布局与网格规则](#五布局与网格规则)
6. [响应式设计断点](#六响应式设计断点)
7. [现有界面审计报告](#七现有界面审计报告)
8. [分阶段整改方案](#八分阶段整改方案)
9. [UI 组件库使用指南](#九ui-组件库使用指南)
10. [设计评审机制](#十设计评审机制)

---

## 一、色彩系统

### 1.1 品牌主色 — 丰收金

传达温暖、信赖、自然丰收的品牌调性。

| Token | 色值 | 使用场景 |
|-------|------|---------|
| `--color-primary-50` | `#fdf8f1` | 浅色背景底色 |
| `--color-primary-100` | `#faeedc` | 选中态背景、标签背景 |
| `--color-primary-200` | `#f4dbb0` | 悬浮态背景 |
| `--color-primary-300` | `#ecc886` | 禁用态边框 |
| `--color-primary-400` | `#e0b462` | 次要装饰 |
| **`--color-primary-500`** | **`#d4a574`** | **基准色：主按钮、品牌标识、CTA** |
| `--color-primary-600` | `#c08b52` | 按钮 hover 态 |
| `--color-primary-700` | `#a6723a` | 按钮 active 态、链接色 |
| `--color-primary-800` | `#8c5b28` | 深色强调 |
| `--color-primary-900` | `#6b4423` | 最深色、文字强调 |

### 1.2 辅助色 — 自然绿

传达天然、健康、有机的产品理念。

| Token | 色值 | 使用场景 |
|-------|------|---------|
| **`--color-green-500`** | **`#66bb6a`** | **基准色：成功状态、有机认证** |
| `--color-green-600` | `#4caf50` | 成功按钮 hover |
| `--color-green-700` | `#43a047` | 成功按钮 active |

### 1.3 强调色 — 暖橙

传达活力、促销、节日氛围。

| Token | 色值 | 使用场景 |
|-------|------|---------|
| **`--color-orange-500`** | **`#e07b39`** | **基准色：促销标签、价格高亮、限时活动** |

### 1.4 中性色

| Token | 色值 | 使用场景 |
|-------|------|---------|
| `--color-gray-50` | `#fafaf9` | 页面最浅底 |
| `--color-gray-100` | `#f5f5f4` | 次级背景 |
| `--color-gray-200` | `#e7e5e4` | 边框浅色 |
| `--color-gray-300` | `#d6d3d1` | 默认边框 |
| `--color-gray-500` | `#78716c` | 三级文字 |
| `--color-gray-600` | `#57534e` | 二级文字 |
| `--color-gray-800` | `#292524` | 一级文字 |
| `--color-gray-900` | `#1c1917` | 最深文字 |

### 1.5 功能色

| 语义 | 色值 | 浅色背景 |
|------|------|---------|
| 成功 Success | `#52c41a` | `#f6ffed` |
| 警告 Warning | `#faad14` | `#fffbe6` |
| 错误 Error | `#ff4d4f` | `#fff2f0` |
| 信息 Info | `#1677ff` | `#e6f4ff` |

### 1.6 背景色

| Token | 色值 | 应用场景 |
|-------|------|---------|
| `--color-bg-page` | `#faf8f5` | 全局页面背景 |
| `--color-bg-container` | `#ffffff` | 卡片、容器背景 |
| `--color-bg-elevated` | `#ffffff` | 弹窗、浮层背景 |
| `--color-bg-mask` | `rgba(0,0,0,0.45)` | 遮罩层 |

### 1.7 颜色使用规则

1. **主色优先级**：CTA 按钮、品牌标识 → `--color-primary-500`
2. **绿色使用限制**：仅用于成功/认证/完成状态，**不作为品牌主色使用**
3. **橙色使用限制**：仅用于促销/警告/限时，**不可用于常规 UI**
4. **中性色为主体**：80% 的 UI 面积应使用中性色，10% 主色，5% 绿色，5% 橙色
5. **WCAG AA 合规**：正文与背景对比度 ≥ 4.5:1，大号文字 ≥ 3:1

---

## 二、排版规范

### 2.1 字体家族

| 层级 | 字体栈 | 用途 |
|------|--------|------|
| 展示字体 | `'Noto Serif SC', 'Source Han Serif SC', serif` | H1-H3 大标题、品牌标语 |
| 正文/UI字体 | `'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif` | 正文、按钮、表单、导航 |
| 等宽字体 | `'JetBrains Mono', 'SF Mono', monospace` | 代码、数据表格 |

### 2.2 字号层级表

| 层级 | CSS 变量 | 实际大小 | 行高 | 字重 | 使用场景 |
|------|----------|---------|------|------|---------|
| Hero | `--font-size-5xl` | 60px | 1.25 | Bold | 首页超大标题 |
| H1 | `--font-size-4xl` | 48px | 1.25 | Bold | 页面主标题 |
| H2 | `--font-size-3xl` | 36px | 1.25 | Semibold | 二级标题 |
| H3 | `--font-size-2xl` | 30px | 1.25 | Semibold | 区块标题 |
| H4 | `--font-size-xl` | 24px | 1.5 | Semibold | 卡片标题 |
| H5 | `--font-size-lg` | 20px | 1.5 | Medium | 小标题 |
| H6 | `--font-size-md` | 18px | 1.5 | Medium | 微标题 |
| Body | `--font-size-base` | 16px | 1.75 | Regular | 正文 |
| Body-Small | `--font-size-sm` | 14px | 1.5 | Regular | 描述文字、次级信息 |
| Caption | `--font-size-xs` | 12px | 1.5 | Regular | 辅助信息、时间戳、角标 |

### 2.3 小程序字号转换

| Web | 小程序 (rpx) | 
|-----|-------------|
| 60px | 60rpx |
| 48px | 48rpx |
| 36px | 36rpx |
| 30px | 32rpx |
| 24px | 28rpx |
| 20px | 26rpx |
| 16px | 28rpx |
| 14px | 26rpx |
| 12px | 24rpx |

> **注意**：小程序 1rpx ≈ 0.5px（以 iPhone 6 为标准），设计中 body 文本使用 28rpx 保证可读性。

---

## 三、组件库设计标准

### 3.1 按钮 (Button)

#### 3.1.1 主按钮 (Primary)

```css
.ml-btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  padding: 12px 28px;
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-600));
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-family-body);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.ml-btn-primary:hover {
  background: linear-gradient(135deg, var(--color-primary-600), var(--color-primary-700));
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.ml-btn-primary:active {
  transform: translateY(0);
  box-shadow: var(--shadow-xs);
}

.ml-btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
```

#### 3.1.2 按钮尺寸

| 尺寸 | 高度 | 内边距 | 字号 | 圆角 |
|------|------|--------|------|------|
| Large | 48px | 16px 32px | 18px | `--radius-lg` |
| **Medium** | **40px** | **12px 28px** | **16px** | **`--radius-md`** |
| Small | 32px | 8px 20px | 14px | `--radius-sm` |

#### 3.1.3 按钮变体

| 变体 | 背景 | 边框 | 文字色 | 使用场景 |
|------|------|------|--------|---------|
| Primary | 丰收金渐变 | 无 | 白色 | 主要操作 |
| Secondary | 透明 | 主色 2px | 主色 | 次要操作 |
| Success | 绿色渐变 | 无 | 白色 | 确认/完成 |
| Danger | 红色渐变 | 无 | 白色 | 删除/危险操作 |
| Text | 透明 | 无 | 主色 | 轻量操作 |
| Ghost | 透明 | 无 | 灰色 | 最低优先级 |

### 3.2 卡片 (Card)

```css
.ml-card {
  background: var(--color-bg-container);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.ml-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

/* 卡片变体 */
.ml-card--flat { box-shadow: none; }
.ml-card--bordered { border-color: var(--color-border-default); }
.ml-card--interactive { cursor: pointer; }
```

#### 卡片规格

| 属性 | Web 端 | 小程序端 |
|------|--------|---------|
| 圆角 | `--radius-xl` (16px) | 24rpx |
| 内边距 | 24px | 28rpx |
| 边框 | 1px solid `--color-border-light` | 1rpx solid rgba(102,187,106,0.08) |
| 阴影 | `--shadow-sm` | 0 4rpx 20rpx rgba(102,187,106,0.12) |

### 3.3 表单 (Form)

#### 输入框规范

```css
.ml-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  background: var(--color-bg-container);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  font-family: var(--font-family-body);
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  transition: all var(--transition-fast);
  outline: none;
}

.ml-input:hover { border-color: var(--color-primary-400); }
.ml-input:focus {
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px var(--color-primary-100);
}
.ml-input::placeholder { color: var(--color-text-tertiary); }
.ml-input--error { border-color: var(--color-error); }
.ml-input--error:focus {
  box-shadow: 0 0 0 3px var(--color-error-light);
}
.ml-input:disabled {
  background: var(--color-gray-100);
  cursor: not-allowed;
}
```

#### 表单项布局

| 属性 | 值 |
|------|----|
| 标签字号 | 14px / Semibold |
| 标签与输入框间距 | 8px |
| 表单项间距 | 24px |
| 错误提示字号 | 12px |
| 标签位置 | 顶部对齐（默认）/ 左侧对齐（大屏表单） |

### 3.4 导航 (Navigation)

#### 管理后台侧边栏 (admin-web)

- 宽度：220px
- 背景：`#ffffff`
- 选中态：左侧 3px 品牌色指示条 + 浅金背景
- 菜单项高度：48px
- 图标尺寸：20px
- 字号：14px

#### 卖家端侧边栏 (seller-web)

- 宽度：240px
- 品牌区：包含 Logo + "乡村振兴 · 商家中心"
- 分组标题：12px / uppercase / 灰色
- 选中态：品牌色背景 + 白色文字
- 菜单项圆角：`--radius-md`

#### 小程序底部导航

```json
{
  "color": "#78716c",
  "selectedColor": "#d4a574",
  "backgroundColor": "#ffffff",
  "borderStyle": "white"
}
```

### 3.5 表格 (Table)

| 属性 | Web | 管理后台 |
|------|-----|---------|
| 表头背景 | `--color-gray-50` | `--color-gray-50` |
| 表头字号 | 14px / Medium | 14px / Medium |
| 行高 | 48px (紧凑) / 56px (默认) | 48px |
| 分割线 | `--color-border-light` | `--color-border-light` |
| 悬停行背景 | `--color-primary-50` | `--color-primary-50` |
| 斑马纹 | `--color-gray-50` 交替 | `--color-gray-50` 交替 |

### 3.6 对话框 (Dialog / Modal)

| 属性 | 值 |
|------|----|
| 宽度 | 520px (默认) / 720px (大) / 400px (小) |
| 圆角 | `--radius-2xl` |
| 标题字号 | 20px / Semibold |
| 内边距 | 24px |
| 遮罩颜色 | `--color-bg-mask` |
| 关闭按钮位置 | 右上角 16px |
| 底部按钮对齐 | 右对齐 / 间距 12px |

### 3.7 标签/徽章 (Tag / Badge)

| 类型 | 背景 | 文字色 | 使用场景 |
|------|------|--------|---------|
| 促销 | 暖橙浅色 | 暖橙色 | 限时折扣、秒杀 |
| 有机 | 绿色浅色 | 绿色 | 有机认证 |
| 新品 | 主色浅色 | 主色 | 新上架商品 |
| 状态-成功 | 绿色浅色 | 绿色 | 已完成、已支付 |
| 状态-进行中 | 蓝色浅色 | 蓝色 | 配送中、处理中 |
| 状态-取消 | 红色浅色 | 红色 | 已取消、已退款 |

---

## 四、图标系统

### 4.1 图标风格

| 端 | 图标方案 | 风格 |
|----|---------|------|
| admin-web | `@element-plus/icons-vue` (SVG) | 线性 (Outline)，20px |
| seller-web | 内联 SVG | 线性 (Outline)，20px / 描边 1.5px |
| web-mall | Emoji + SVG | 混合风格 |
| mini-program | Emoji (作为图标) | 颜色丰富 |

### 4.2 统一策略

1. **Web 端**（admin-web / seller-web / web-mall）：统一使用 `@element-plus/icons-vue`，20px 线性图标
2. **小程序**：逐步从 Emoji 切换到自定义 SVG 图标组件
3. **图标颜色**：默认继承文字色，激活态使用品牌主色
4. **图标尺寸**：
   - 导航菜单：20px
   - 按钮内图标：18px
   - 装饰性图标：24px - 48px
   - 小程序图标：36rpx - 48rpx

### 4.3 核心图标清单

| 类别 | 图标 |
|------|------|
| 导航 | Home, Category, Cart, User, Order |
| 操作 | Plus, Edit, Delete, Search, Filter, Download, Upload |
| 状态 | Check, Close, Warning, Info, Clock, Star |
| 数据 | Goods, ShoppingCart, Money, TrendCharts, DataAnalysis |
| 系统 | Setting, Lock, Unlock, Bell, Message, Switch |

---

## 五、布局与网格规则

### 5.1 Web 端 12 列网格

```
| 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 |
|<- gutter 24px ->|
```

- 最大内容宽度：1200px
- 页面水平内边距：移动端 16px / 平板 24px / 桌面 32px

### 5.2 管理后台布局

```
┌─────────────────────────────────────────────┐
│  Header (64px)                               │
├────────┬────────────────────────────────────┤
│Sidebar │  Content Area                      │
│220px   │  max-width: auto                   │
│        │  padding: 24px                     │
│        │                                    │
└────────┴────────────────────────────────────┘
```

### 5.3 小程序布局

- 页面内边距：16rpx
- 卡片间距：28rpx
- 商品网格：2 列（默认）/ 3 列（≥600rpx）/ 4 列（≥800rpx）
- 底部安全区：`env(safe-area-inset-bottom)`
- 顶部安全区：`env(safe-area-inset-top)`

### 5.4 间距使用规范

| 间距 | 使用场景 |
|------|---------|
| 4px / 8px | 紧密关联元素（图标-文字、标签组） |
| 12px / 16px | 表单元素间距、卡片内间距 |
| 20px / 24px | 卡片之间、区块之间 |
| 32px / 40px | 大区块分隔 |
| 48px+ | 页头到内容、大段落分隔 |

---

## 六、响应式设计断点

| 断点 | 宽度 | 适用设备 | 网格列 |
|------|------|---------|--------|
| xs | < 480px | 小手机 | 4列 |
| sm | ≥ 640px | 大手机 | 6列 |
| md | ≥ 768px | 平板 | 8列 |
| lg | ≥ 1024px | 小桌面 | 12列 |
| xl | ≥ 1280px | 桌面 | 12列 |
| 2xl | ≥ 1536px | 大桌面 | 12列 |

### 各端适配策略

- **admin-web**：桌面优先（≥1024px），侧边栏在 < 768px 时折叠为汉堡菜单
- **seller-web**：桌面优先（≥1024px），侧边栏在 < 768px 时折叠
- **web-mall**：移动优先，最大宽度 1200px 居中
- **mini-program**：微信小程序 rpx 自适应，无需额外断点处理

---

## 七、现有界面审计报告

### 7.1 审计总览

经对 admin-web、seller-web、web-mall、mini-program 四个端的完整界面审计，发现以下关键不一致问题：

### 7.2 色彩系统混乱

| 端 | 当前主色 | 当前辅助色 | 与规范偏差 |
|----|---------|-----------|-----------|
| **admin-web** | Element Plus 默认蓝 `#409EFF` | 无 | **严重不一致**——完全未使用品牌色 |
| **seller-web** | `#5c946e` (绿) | `#e6b142` (金) | **中度偏差**——主辅色颠倒，应主金辅绿 |
| **web-mall** | `#d4a574` (金) | `#7a9e7e` (绿) | **基本吻合**——与规范主辅色一致 |
| **mini-program** | `#66bb6a` (绿) | 价格红 `#f44336` | **严重偏差**——主色应该是金色而非绿色；Tab Bar 选中色 `#d97706` 与页面绿色不一致 |

### 7.3 字体家族不一致

| 端 | 当前字体 | 规范字体 | 状态 |
|----|---------|---------|------|
| admin-web | `'PingFang SC', 'Microsoft YaHei', sans-serif` | `'Noto Sans SC', 'PingFang SC', ...` | 接近，需添加 Noto Sans SC 首选项 |
| seller-web | `Inter, 'Noto Sans SC', sans-serif` | 同上 | 需移除英文字体 Inter 首选项 |
| web-mall | `'Nunito', 'Noto Serif SC'` | 正文应为 `'Noto Sans SC'` 而非 Serif | 正文字体需修改，标题字体可保留 Serif |
| mini-program | `'PingFang SC', 'Hiragino Sans GB', ...` | 同上 | 接近，需添加 Noto Sans SC 首选项 |

### 7.4 组件差异明细

| 组件 | admin-web | seller-web | web-mall | mini-program | 
|------|-----------|------------|----------|-------------|
| 主按钮 | Element Plus 蓝，圆角 4px | 绿色渐变，圆角 8px | 金色渐变，圆角 12px，光泽动画 | 绿色渐变，圆角 40rpx |
| 卡片 | Element Plus 默认，hover 阴影 | 白底+绿边框，hover 上移4px | 白底+细边框，hover 上移6px+顶部彩色条 | 绿色渐变背景+绿色阴影，顶部绿色线 |
| 输入框 | Element Plus 默认 | 无自定义 | 2px 主色边框，focus 外发光 | 浅绿底+绿色边框 |
| 导航 | Element Plus 默认蓝菜单 | 自定义侧边栏 SVG 图标 | 顶部导航 | 底部 Tab Bar（绿底金选中色） |
| 标签 | Element Plus 默认 | 无自定义 | 多色有机标签（🌿🍃⭐🌾）| 无自定义标签组件 |

### 7.5 间距/网格不统一

- admin-web 使用 Element Plus 的 20px gutter，seller-web 使用 24px，web-mall 无统一系统
- 小程序使用 rpx 单位，间距值缺乏一致性（20rpx / 24rpx / 28rpx / 32rpx 混用）

### 7.6 图标系统差异

- admin-web/seller-web 使用 `@element-plus/icons-vue`，样式统一
- web-mall 使用 emoji + Element Plus 图标，混搭不一致
- mini-program **完全使用 emoji** 作为图标，缺乏专业感

### 7.7 小程序 Tab Bar 配置问题

当前 `app.json` 配置的 `selectedColor: "#d97706"` 为琥珀色，但页面内容使用 `#66bb6a` 绿色，存在视觉断裂。需统一为品牌主色 `#d4a574`。

---

## 八、分阶段整改方案

### 第一阶段：基础统一（第 1-2 周）

**目标**：统一色彩和排版，零破坏性变更

| 任务 | 端 | 优先级 | 描述 |
|------|----|-------|------|
| 1.1 | 全局 | P0 | 创建 `shared-ui/design-tokens.css` 作为所有端的 CSS 变量源文件 ✅ 已完成 |
| 1.2 | admin-web | P0 | 在 `index.css` 中引入 `design-tokens.css`，覆盖 Element Plus 主色变量 |
| 1.3 | seller-web | P0 | 重写 `theme.css`，使用新的 CSS 变量替换硬编码色值 |
| 1.4 | web-mall | P1 | 调整 `theme.css`，将柔和色调变量映射到新命名规范 |
| 1.5 | mini-program | P0 | 在 `app.wxss` 中创建对应的 CSS 变量（小程序支持的属性），定义主题色 |
| 1.6 | mini-program | P0 | 修改 `app.json` 中 `tabBar.selectedColor` 为 `#d4a574` |
| 1.7 | 全局 | P1 | 统一各端 `font-family` 声明，引入 `Noto Sans SC`（通过 CDN 或自托管） |

### 第二阶段：组件标准化（第 3-5 周）

**目标**：建立统一组件库，逐端替换

| 任务 | 端 | 优先级 | 描述 |
|------|----|-------|------|
| 2.1 | 全局 | P0 | 在 `shared-ui/` 目录创建基础组件 CSS（按钮、卡片、输入框、标签、表格、对话框） |
| 2.2 | web-mall | P0 | 将现有 `.btn-primary`、`.card` 等类替换为标准化 `ml-btn-primary`、`ml-card` 类 |
| 2.3 | seller-web | P1 | 将自定义按钮和卡片组件迁移到标准化组件 |
| 2.4 | admin-web | P1 | 在 Element Plus 基础上叠加品牌样式定制（使用 CSS 变量覆盖） |
| 2.5 | mini-program | P0 | 重写 `app.wxss` 中的通用样式类，使用统一的命名和色值 |
| 2.6 | 全局 | P1 | 创建 Vue 共享组件包 `@mall/shared-ui`（Button、Card、Input、Modal、Tag） |

### 第三阶段：图标与细节统一（第 6-7 周）

**目标**：统一图标系统，优化细节

| 任务 | 端 | 优先级 | 描述 |
|------|----|-------|------|
| 3.1 | mini-program | P0 | 将 Emoji 图标替换为 SVG 图标组件（至少覆盖 Tab Bar 和导航图标） |
| 3.2 | web-mall | P1 | 将 Emoji 装饰性图标替换为 SVG |
| 3.3 | 全局 | P1 | 统一 Loading 动画、空状态插画风格 |
| 3.4 | 全局 | P2 | 统一滚动条样式（Webkit 自定义） |

### 第四阶段：布局与响应式优化（第 8-10 周）

**目标**：统一间距、网格、响应式行为

| 任务 | 端 | 优先级 | 描述 |
|------|----|-------|------|
| 4.1 | 全局 | P1 | 统一所有端的间距系统，使用 4px 基准 |
| 4.2 | admin-web | P1 | 优化侧边栏响应式行为，增加折叠/展开动画 |
| 4.3 | seller-web | P1 | 优化侧边栏响应式行为 |
| 4.4 | web-mall | P2 | 检查所有页面的响应式断点，确保断点行为一致 |
| 4.5 | mini-program | P1 | 统一页面内边距和组件间距为固定倍数 |

### 优先级说明

- **P0（紧急）**：必须在第一阶段完成，影响品牌统一性
- **P1（重要）**：第二阶段完成，影响用户体验一致性
- **P2（优化）**：第三阶段完成，锦上添花

---

## 九、UI 组件库使用指南

### 9.1 架构说明

```
shared-ui/
├── design-tokens.css        # 设计令牌（CSS 变量），所有端的唯一色值来源
├── base.css                 # 全局重置和基础样式
├── components/
│   ├── button.css           # 按钮组件样式
│   ├── card.css             # 卡片组件样式
│   ├── form.css             # 表单组件样式
│   ├── table.css            # 表格组件样式
│   ├── tag.css              # 标签/徽章组件样式
│   ├── modal.css            # 对话框组件样式
│   └── nav.css              # 导航组件样式
├── utilities/
│   ├── spacing.css          # 间距工具类
│   ├── typography.css       # 排版工具类
│   └── animations.css       # 动画工具类
└── index.css                # 汇总入口
```

### 9.2 各端引入方式

#### admin-web / seller-web / web-mall (Vite 项目)

```javascript
// main.js
import '@mall/shared-ui/design-tokens.css'
import '@mall/shared-ui/index.css'
```

#### mini-program

```css
/* app.wxss 顶部引入（手动复制变量定义）*/
@import './shared-design-tokens.wxss';
```

> 由于小程序不支持 CSS `@import` 跨目录引用完整变量系统，需在 `app.wxss` 中手动维护一份精简的变量定义副本，每次设计令牌更新时同步。

### 9.3 HTML 类名规范

采用 BEM 变体命名法，统一前缀 `ml-`（取自 **M**al**l**）：

```html
<!-- 按钮 -->
<button class="ml-btn ml-btn--primary ml-btn--lg">提交订单</button>
<button class="ml-btn ml-btn--secondary">取消</button>
<button class="ml-btn ml-btn--danger ml-btn--sm">删除</button>
<button class="ml-btn ml-btn--text">查看更多</button>
<button class="ml-btn ml-btn--primary" disabled>已禁用</button>

<!-- 卡片 -->
<div class="ml-card ml-card--interactive">
  <div class="ml-card__header">标题</div>
  <div class="ml-card__body">内容</div>
  <div class="ml-card__footer">底部</div>
</div>

<!-- 输入框 -->
<div class="ml-form-item">
  <label class="ml-form-item__label">商品名称</label>
  <input class="ml-input" placeholder="请输入商品名称" />
  <span class="ml-form-item__error">请输入商品名称</span>
</div>

<!-- 标签 -->
<span class="ml-tag ml-tag--success">已完成</span>
<span class="ml-tag ml-tag--warning">促销中</span>
<span class="ml-tag ml-tag--organic">有机认证</span>
```

### 9.4 开发流程

1. **设计令牌优先**：任何新样式首先检查是否可映射到现有 token
2. **使用变量而非硬编码**：禁止在组件中硬编码色值，一律引用 CSS 变量
3. **组件驱动**：界面使用统一组件，如需新组件，先在 `shared-ui/` 中创建
4. **跨端测试**：组件变更需在 4 个端分别验证

---

## 十、设计评审机制

### 10.1 评审流程

```
需求/设计稿
     │
     ▼
┌──────────────┐     不通过
│ 设计自查      │─────────────┐
│ (检查规范合规) │              │
└──────┬───────┘              │
       │ 通过                  │
       ▼                      │
┌──────────────┐     不通过   │
│ 设计评审会议   │──────────┐ │
│ (设计负责人)   │           │ │
└──────┬───────┘           │ │
       │ 通过               │ │
       ▼                   │ │
┌──────────────┐           │ │
│ 开发实现       │           │ │
└──────┬───────┘           │ │
       │                   │ │
       ▼                   │ │
┌──────────────┐     不通过 │ │
│ UI 走查 (设计→开发) │─────┘ │
└──────┬───────┘           │ │
       │ 通过               │ │
       ▼                   │ │
┌──────────────┐           │ │
│ 合并上线       │           │ │
└──────────────┘           │ │
       ▲                   │ │
       └───────────────────┘ │
             修改后重新提交    │
                             │
       ┌─────────────────────┘
       ▼
  记录问题到规范文档
```

### 10.2 评审检查清单

每次 UI 变更提交时必须通过的检查项：

| # | 检查项 | 标准 |
|---|--------|------|
| 1 | 色值来源 | 所有颜色使用 CSS 变量，无硬编码色值 |
| 2 | 字体一致 | 使用 `--font-family-body` 或 `--font-family-display` |
| 3 | 间距规范 | 间距为 4px 倍数，使用 `--spacing-*` 变量 |
| 4 | 圆角规范 | 使用 `--radius-*` 变量 |
| 5 | 阴影规范 | 使用 `--shadow-*` 变量 |
| 6 | 过渡动画 | 使用 `--transition-*` 变量 |
| 7 | 组件复用 | 优先使用 `ml-*` 标准组件 |
| 8 | 响应式 | 至少覆盖移动端 (≥640px) 和桌面端 (≥1024px) |
| 9 | 可访问性 | 对比度 ≥ 4.5:1，表单有 label，按钮有 hover/focus 态 |
| 10 | 跨端一致性 | 4 个端的同一功能视觉风格一致 |

### 10.3 工具推荐

| 工具 | 用途 |
|------|------|
| **Stylelint** + 自定义规则 | 自动检测硬编码色值和样式违规 |
| **Chromatic / Storybook** | 视觉回归测试 |
| **Figma** | 设计稿统一管理，团队组件库同步 |
| **axe DevTools** | 可访问性自动检测 |
| **CSS Variables Inspector** | 浏览器开发者工具中查看变量使用情况 |

### 10.4 设计规范维护

- **规范文档**：存储在 `shared-ui/` 目录下，与代码同步版本控制
- **变更流程**：任何 token 变更需提交 PR，附变更理由和影响范围
- **定期审计**：每 2 周进行一次 UI 一致性走查
- **版本发布**：token 变更视为 breaking change，需发布新版本号

---

## 附录

### A. 小程序精简变量定义 (app.wxss 复制用)

```css
/* 乡村振兴 小程序主题变量 - 与 shared-ui/design-tokens.css 同步 */
page {
  --color-primary:       #d4a574;
  --color-primary-dark:  #a6723a;
  --color-green:         #66bb6a;
  --color-green-dark:    #4caf50;
  --color-orange:        #e07b39;
  --color-text-primary:  #292524;
  --color-text-secondary:#57534e;
  --color-text-tertiary: #78716c;
  --color-border:        #e7e5e4;
  --color-bg-page:       #faf8f5;
  --color-bg-card:       #ffffff;
  --color-success:       #52c41a;
  --color-error:         #ff4d4f;
  --color-warning:       #faad14;
  --radius-sm: 8rpx;
  --radius-md: 16rpx;
  --radius-lg: 24rpx;
  --shadow-card: 0 4rpx 20rpx rgba(107, 68, 35, 0.08);
}
```

### B. Element Plus 主题覆盖配置 (vite.config)

```typescript
// 在 admin-web/vite.config.ts 和 seller-web/vite.config.js 中添加
import { defineConfig } from 'vite'

export default defineConfig({
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
          @use "@mall/shared-ui/design-tokens.css";
          $el-color-primary: var(--color-primary-500);
        `
      }
    }
  }
})
```

### C. 变更日志

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-21 | v1.0 | 初始版本，建立完整 UI 设计规范体系 |

---

*本文档为此项目的 UI 设计唯一权威来源，所有开发、设计、评审活动均应以本文档为基准。*
