# Mall System 迁移计划：Java 17 + Spring Boot 3.4 + Spring AI

> 版本：v1.0 ｜ 日期：2026-08-05 ｜ 状态：**计划评审中（未开工）**
> 适用范围：`E:\迅雷下载\mall_system_extended` 全部 9 个 Maven 服务
> 文档原则：所有"现状"数据均来自实测（pom.xml / 源码扫描），不是估计；所有版本号给出**确定性目标**，标注兼容性依据。

---

## 0. 文档目的

回答三个问题：

1. **为什么做**：Java 8 + Spring Boot 2.7.9 已 EOL；引入 Spring AI 1.0（2025-05 GA）要求 Spring Boot 3.4+；技术栈升级是 Java 后端面试的加分叙事。
2. **做什么**：全家桶升级（Boot 3.4 + Java 17 + Spring Cloud 2024 + SCA 2024）→ 行为不变 → 再在 ai-service 引入 Spring AI。
3. **怎么验收**：每个阶段有明确验收标准，全程 TDD（先跑绿基线，改一步验一步）。

**核心判断：真正的工程是 Spring Boot 2.7 → 3.x 迁移（约 80% 工作量）；Spring AI 引入是迁移完成后在 ai-service 上的局部改造（约 20%）。**

---

## 1. 现状基线（实测数据，2026-08-05 扫描）

### 1.1 技术栈基线

| 组件 | 当前版本 | 说明 |
|---|---|---|
| Java | **8** | `java.version=8`（每个服务 pom 里写死） |
| Spring Boot | **2.7.9** | 全部 9 个服务，2022 年版本，EOL |
| Spring Cloud | **2021.0.9** (Jubilee) | 对应 Boot 2.6/2.7 线 |
| Spring Cloud Alibaba | **2021.0.5.0** | 老版本，对 Boot 3 **完全不兼容** |
| MyBatis-Plus | **3.5.3.1** | `mybatis-plus-boot-starter`（javax 线） |
| jjwt | **0.11.5** | javax 线，0.12+ 换包且 API 大改 |
| RocketMQ | **2.2.3** (`rocketmq-spring-boot-starter`) | javax 线，product/order 两个服务用 |
| Seata | **1.6.1** (`seata-spring-boot-starter`) | javax 线，order-service 用，**本次迁移最大风险点** |
| Lombok | 1.18.30 | 兼容 Java 17，可不动 |
| mysql-connector-java | 8.0.33 | 需改名 `com.mysql:mysql-connector-j` |

### 1.2 代码基线

| 指标 | 数值 |
|---|---|
| Java 文件总数 | 622 |
| 代码行数 | ~76,000 |
| 使用 `javax.*` 的文件 | **93**（需迁 `jakarta.*`） |
| SecurityConfig | **7 个**（全部继承 `WebSecurityConfigurerAdapter`，6.x 已删除） |
| 测试文件 | 27（`src/test`，含 ChatControllerTest、ChatMessageDeliveryE2ETest 等） |
| 服务入口 | 8 个 `*Application.java` |

### 1.3 服务与依赖地图（迁移前）

| 服务 | 特有依赖 | 迁移关注点 |
|---|---|---|
| gateway | Spring Cloud Gateway、Nacos discovery、Sentinel-gateway、jjwt | 网关路由配置、Sentinel 网关适配 |
| backend | MyBatis-Plus、Redis、Security、WebSocket、Nacos config/discovery、Sentinel、Feign、jjwt | 核心服务，先行试点 |
| ai-service | 同 backend + 手写 DeepSeek/RAG（AIService/EmbeddingService/RagConfig/DeepSeekConfig） | **阶段二 Spring AI 改造目标** |
| chat-service | 同 backend | 与 ai-service 联动的聊天 |
| user-service | MyBatis-Plus、Redis、Security、Nacos discovery、Feign（**无 jjwt/nacos-config/sentinel**） | 最轻量，适合练手 |
| product-service | + **RocketMQ**、Sentinel | RocketMQ 客户端升级 |
| order-service | + **RocketMQ + Seata**、Sentinel、WebSocket、jjwt | **最难，Seata 分布式事务** |
| payment-service | 同 backend | — |
| common | jjwt 工具类 | JWT 重写影响所有服务 |

---

## 2. 目标版本矩阵（依赖矩阵）

### 2.1 为什么必须是这个组合（讲为什么）

- **Spring AI 1.0.0 GA 的 BOM 基于 Spring Boot 3.4.x** → 所以 Boot 必须 ≥ 3.4，不能用更"稳"的 3.2 线（否则 Spring AI 装不上）。
- **Boot 3.4 对应的 Spring Cloud 线是 2024.0.x**（版本命名 = 年份.0.小版本，Boot 与 Cloud 版本严格对齐，混用必炸）。
- **Boot 3.4 对应的 Spring Cloud Alibaba 线是 2024.0.0**（SCA 官方版本表：2021→Boot2.6、2022→Boot3.0、2023→Boot3.2、2024→Boot3.4）。
- **Boot 3 强制 Java 17+**，所以 Java 8 → 17 是门票，不是可选项。
- **javax → jakarta**：Java EE 2017 年捐给 Eclipse 基金会改名 Jakarta EE，包名从 `javax.*` 改为 `jakarta.*`。Boot 3 全面切换，93 个文件受影响。

### 2.2 目标版本表（迁移后）

| 组件 | 目标版本 | 兼容性依据 / 注意 |
|---|---|---|
| Java | **17** | Boot 3.4 最低要求；LTS 长期支持 |
| Spring Boot | **3.4.x**（3.4.5+） | Spring AI 1.0.0 官方基于 3.4 |
| Spring Cloud | **2024.0.x**（2024.0.1+） | 与 Boot 3.4 官方对齐 |
| Spring Cloud Alibaba | **2024.0.0** | SCA 官方对应 Boot 3.4；Sentinel 仍为 1.8.x 适配 |
| Spring AI | **1.0.0**（BOM 管理） | `spring-ai-bom` 已实测存在于 Maven Central |
| Spring AI DeepSeek | **1.0.0**（`spring-ai-starter-model-deepseek`） | 已实测存在；DeepSeek 官方 starter，非 OpenAI 兼容绕行 |
| MyBatis-Plus | **3.5.12**（`mybatis-plus-spring-boot3-starter`） | **artifactId 变了**，boot3 专用 |
| jjwt | **0.12.6** | 包不变但 API 大改（见 4.3） |
| RocketMQ | **2.3.x**（`rocketmq-spring-boot-starter`） | 2.3.0+ 支持 Boot 3（jakarta） |
| Seata | **1.8.x**（`seata-spring-boot-starter`） | 1.7.0+ 才支持 Boot 3；**1.6.1 直接不兼容** |
| mysql 驱动 | `com.mysql:mysql-connector-j` | Boot 3 管理版本，**groupId 从 `mysql` 改为 `com.mysql`** |
| Lombok | 1.18.34+ | 保险起见升一下 |

> ⚠️ 小版本号（3.4.x 的 x、2024.0.x 的 x）以执行当天 Maven Central / 官方 release notes 为准。**大版本线（3.4 / 2024.0 / 2024.0.0 / 1.0.0）不要动**，这是被官方版本矩阵锁死的。

---

## 3. 分服务改动清单（按迁移顺序）

> 迁移顺序原则：**common 先行（JWT 被所有服务依赖）→ 轻量服务练手（user）→ 核心服务（backend）→ AI 服务 → 业务服务 → gateway 收尾（入口最后动，风险最小化）**。

### 3.0 common（第一个改）

| 改动 | 内容 |
|---|---|
| pom | `java.version` 8→17；jjwt 0.11.5→0.12.6；如果 common 内是纯工具类，无 javax 问题（需实测确认） |
| 代码 | JWT 工具类按 4.3 重写 |
| 验收 | `mvn -pl common clean package` 通过 |

### 3.1 user-service（练手，最轻量）

| 改动 | 内容 |
|---|---|
| pom | Boot 3.4.x + Cloud 2024.0.x + SCA 2024.0.0 + Java 17；MyBatis-Plus 换 boot3 starter |
| 代码 | javax→jakarta；SecurityConfig 重写；`spring.factories` → `AutoConfiguration.imports`（如有） |
| 验收 | 编译 + 启动 + 登录/注册接口 smoke test |

### 3.2 backend（核心，试点含 Nacos config + Sentinel + WebSocket）

| 改动 | 内容 |
|---|---|
| pom | 同 user + Nacos config 的 bootstrap 模式改造（见 4.6） |
| 代码 | 4.2–4.9 全部主题；WebSocket 配置（`WebSocketConfigurer` 无大变化，但 `HandlerInterceptor` 签名注意） |
| 验收 | 单测全绿 + 启动 + 核心接口 smoke（登录、商品、购物车） |

### 3.3 ai-service / chat-service（AI 服务）

| 改动 | 内容 |
|---|---|
| pom | 同 backend；**阶段二**加 `spring-ai-bom` + `spring-ai-starter-model-deepseek` |
| 代码 | 阶段一仅迁移；阶段二把 `AIService` 的裸 HTTP 调用换成 `ChatClient`（见 5） |
| 验收 | 阶段一：聊天/RAG 接口行为不变；阶段二：流式输出 + 结构化输出验证 |

### 3.4 product-service / order-service（RocketMQ + Seata）

| 改动 | 内容 |
|---|---|
| pom | RocketMQ 2.2.3→2.3.x；order 额外 Seata 1.6.1→1.8.x |
| 代码 | RocketMQ 生产者/消费者注解兼容（`@RocketMQMessageListener` 基本不变）；**Seata 配置方式变更**（见 4.7） |
| 验收 | 下单 → 扣库存 → 支付全链路 smoke test；**Seata 事务回滚验证（重点）** |

### 3.5 payment-service

| 改动 | 同 backend |
| 验收 | 支付回调 + 对账接口 smoke test |

### 3.6 gateway（最后）

| 改动 | 内容 |
|---|---|
| pom | Boot 3.4 + Cloud 2024.0.x（Gateway 版本随 Cloud）；SCA Sentinel-gateway 适配 |
| 代码 | 路由配置 `spring.cloud.gateway.routes` 基本兼容；Sentinel 网关规则配置核对；**CORS/跨域配置在 Boot 3 下 API 变化** |
| 验收 | 全链路：经网关访问各服务鉴权通过 |

---

## 4. 代码级迁移清单（横向，按主题）

### 4.1 javax → jakarta（93 个文件）

```bash
# 常见替换（机械部分，脚本批量 + 编译错误兜底，禁止纯手工）
javax.servlet.*          → jakarta.servlet.*
javax.servlet.http.*     → jakarta.servlet.http.*
javax.validation.*       → jakarta.validation.*
javax.annotation.*       → jakarta.annotation.*
javax.persistence.*      → jakarta.persistence.*
```

**为什么用脚本而不是手改**：93 个文件、每处只是 import 行替换，手改易漏；正确姿势是 `sed` 批量替换后**靠 javac 编译错误清单兜底**（改漏的 import 会直接编译失败，一个都跑不掉）。

### 4.2 Spring Security 5.7 → 6.x（7 个 SecurityConfig，最大的坑）

**变化本质**：6.x 删除了 `WebSecurityConfigurerAdapter` 基类，改为**声明式 `SecurityFilterChain` Bean**；`antMatchers()` → `requestMatchers()`（Ant 语法默认关闭，需显式 `.pathMatchers()`）。

```java
// ❌ Boot 2.7 写法（当前项目）
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
            .and().csrf().disable();
    }
}

// ✅ Boot 3.4 写法
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
    // 密码编码器、CORS 等也全部改为 @Bean 声明
}
```

注意点：`UserDetailsService`、`AuthenticationManager`、`PasswordEncoder` 全部改为 `@Bean`；`WebSecurityConfigurerAdapter` 若在依赖里显式引用，编译会直接报错（好事情，逼你改干净）。

### 4.3 JWT（jjwt 0.11.5 → 0.12.6，common 模块重写）

**API 变化**：`parserBuilder()` → `parser().verifyWith(key)`；`setSigningKey` 换成 `verifyWith`/`signWith`；`Jwts.builder()` 的签名方法参数类型从 `byte[]/String` 改为 `Key`。

```java
// ❌ 0.11.x：parserBuilder().setSigningKey(secret).parseClaimsJws(token)
// ✅ 0.12.x：
SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
Claims claims = Jwts.parser().verifyWith(key).build()
        .parseSignedClaims(token).getPayload();
```

**为什么 jjwt 必炸**：0.12.0 起 jjwt 全面转向 jakarta + 新解析 API，旧写法编译直接失败。common 是所有服务的依赖，所以 common 第一优先。

### 4.4 MyBatis-Plus（3.5.3.1 → 3.5.12）

```xml
<!-- ❌ 旧 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
<!-- ✅ 新：artifactId 换成 boot3 专用，版本 3.5.12 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.12</version>
</dependency>
```

分页插件配置：`MybatisPlusInterceptor` + `PaginationInnerInterceptor` 的写法**不变**，但 `MybatisPlusConfig` 里如果引用了 `@MapperScan` 的包路径，保持原样即可。

### 4.5 spring.factories → AutoConfiguration.imports

Boot 3 不再读取 `META-INF/spring.factories` 里的自动配置声明（仅保留 SPI 类），改为：

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

如果项目里自定义了自动配置（`spring.factories` 里的 `EnableAutoConfiguration` 项），必须迁移；没有的话跳过。

### 4.6 Nacos Config bootstrap 模式（backend/ai/chat/order/payment 受影响）

SCA 2024 线仍支持 bootstrap，但 Boot 3 更推荐 `spring.config.import` 方式；**最小改动方案**：保留 `spring-cloud-starter-bootstrap` 依赖 + 现有 `bootstrap.yml`，这一条路径在 SCA 2024.0.0 下依然可用。若 Nacos 配置读取失败（启动报 config 未找到），再切换为：

```yaml
spring:
  config:
    import: nacos:${spring.application.name}.yaml?group=DEFAULT_GROUP
```

### 4.7 Seata（order-service，本次迁移最大风险点）

**为什么是最大风险**：Seata 1.6.1 是 javax 线，Boot 3 下直接启动失败。1.7.0 起支持 Boot 3，但配置方式有变化：

- `seata-spring-boot-starter` 1.6.1 → 1.8.x（groupId 不变 `io.seata`）
- 全局事务注解 `@GlobalTransactional` **不变**（业务代码基本不用动）
- 重点核对：Seata 服务端（TC）版本与客户端**大版本必须一致**（1.6.1 的 TC 要升 1.8.x）；`registry.conf` / `file.conf` 配置结构在 1.7+ 有调整
- 本地如果没有独立 Seata TC（单机测试用 file 模式），启动方式不变

**验收重点**：制造一个"下单后扣库存失败"的场景，验证 `@GlobalTransactional` 回滚（这是面试必问的分布式事务 demo，不能坏）。

### 4.8 RocketMQ（2.2.3 → 2.3.x）

`rocketmq-spring-boot-starter` 2.3.x 支持 Boot 3。业务代码层面：`@RocketMQMessageListener`、`RocketMQTemplate` 的 API **基本不变**（2.3 主要是 jakarta 迁移 + 兼容 Boot 3 自动配置）。风险低，但 MQ 消息格式/消费组配置需要 smoke test 验证。

### 4.9 其他零散 API 变化

| 位置 | 变化 |
|---|---|
| Redis | `RedisTemplate` 序列化配置写法不变；若用了 `spring.redis.*` 配置前缀，Boot 3 仍是 `spring.data.redis.*`（2.7 已支持新前缀，注意 application.yml 里的 key） |
| WebMvc | `WebMvcConfigurer` 接口默认方法有增删；`addCorsMappings` 等主流方法不变 |
| validation | `javax.validation` → `jakarta.validation`；`@Validated` 用法不变 |
| Jackson | Boot 3 默认 Jackson 2.15+，时间序列化行为有细微差异（`spring.jackson` 配置核对） |
| actuator | 端点路径 `/actuator` 不变；`management.endpoints` 配置基本兼容 |

---

## 5. 阶段二：Spring AI 引入方案（ai-service）

### 5.1 目标

- 保留现有 `AIController` / `FaqController` / 会话管理 / 自研检索链路（分块、向量化、重排是项目深度，不重写）
- 把 `AIService` 里的**裸 HTTP 调 DeepSeek**（HttpClient + 手写 JSON 解析）替换为 Spring AI `ChatClient`
- 新增能力演示：流式 SSE 输出、结构化输出（`entity()` 反序列化）

### 5.2 pom 改动（ai-service 专属）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- DeepSeek 官方 starter（已实测 Maven Central 有 1.0.0） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-deepseek</artifactId>
    </dependency>
</dependencies>
```

### 5.3 配置（application.yml）

```yaml
spring:
  ai:
    model:
      deepseek:
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com
        chat:
          options:
            model: deepseek-chat   # 或 deepseek-reasoner
            temperature: 0.7
```

### 5.4 代码改造示例

```java
// 注入统一 ChatClient（替代手写 HttpClient）
@Bean
ChatClient chatClient(ChatClient.Builder builder) {
    return builder.build();
}

// 流式输出（现有 SSE 端点改造成本最低，前端不用动）
@Service
public class AIService {
    private final ChatClient chatClient;
    // ...
    public Flux<String> chatStream(String prompt) {
        return chatClient.prompt(prompt)
                .stream()
                .content();   // Flux<String>，SSE 天然支持
    }
}
```

### 5.5 面试叙事（trade-off 分析，比"我用了 Spring AI"高一个段位）

> 生成层（LLM 调用）框架化收益高：模型切换零成本、流式/结构化输出免手搓、官方 DeepSeek starter 开箱即用；检索层（分块策略、向量化、重排）保留自研，因为那是业务深度所在、框架化的收益低。**只把该框架化的框架化，不为了用框架而重写。**

---

## 6. 分阶段执行计划（TDD）

> 铁律：**每阶段开工前先跑绿当前基线**（`mvn test`），改动后回归对比；任何阶段不通过不进入下一阶段。

### 阶段 0：基线锁定（0.5 天）

- [ ] `mvn clean test` 全量跑绿，记录每个服务的测试数量与失败清单（现状若有失败，先修再迁移）
- [ ] `mvn clean package` 确认本地可构建
- [ ] 记录当前 Java 编译版本：`mvn -version`、`java -version`
- [ ] 快照备份：`git status` 确认工作区干净，创建迁移分支 `feature/java17-boot3`（如用 git）

### 阶段 1：common + user-service（练手，1 天）

- [ ] common：Java 17 + jjwt 0.12.6，重写 JWT 工具类
- [ ] user-service：pom 全家桶升级 + javax→jakarta + SecurityConfig 重写
- [ ] 验收：`mvn -pl common,user-service clean package` 通过；user-service 启动，登录/注册 smoke test 通过

### 阶段 2：backend（核心，1–1.5 天）

- [ ] 同 user 改动 + Nacos config bootstrap 验证 + Sentinel 配置核对
- [ ] 验收：单测全绿；启动后经网关直连验证核心接口；WebSocket 连接建立成功

### 阶段 3：ai-service + chat-service（1 天）

- [ ] 阶段一：仅迁移（行为不变），AI 聊天 + RAG 问答回归
- [ ] 验收：`ChatControllerTest`、`ChatMessageDeliveryE2ETest` 全绿

### 阶段 4：product + order + payment（1.5–2 天，最难）

- [ ] product：RocketMQ 2.3.x，消息收发验证
- [ ] order：RocketMQ + **Seata 1.8.x**，全局事务回滚验证（构造扣库存失败场景）
- [ ] payment：同 backend
- [ ] 验收：**下单 → 扣库存 → 支付全链路 smoke test**；Seata 回滚日志确认

### 阶段 5：gateway（0.5 天，入口最后动）

- [ ] 全家桶升级 + Sentinel 网关适配 + CORS 核对
- [ ] 验收：经网关访问全部服务鉴权链路通过

### 阶段 6：全量回归 + 部署验证（1 天）

- [ ] `mvn clean test` 全量绿（对比阶段 0 基线）
- [ ] docker-compose 本地起全链路 smoke test
- [ ] Railway / k8s 部署（见第 8 节）

### 阶段 7：Spring AI 引入（1–2 天，独立于 0–6）

- [ ] ai-service 加 spring-ai-bom + deepseek starter
- [ ] `AIService` 裸调用 → `ChatClient`（保留检索链路）
- [ ] 流式 SSE + 结构化输出新增 demo 端点
- [ ] 验收：聊天回归 + 流式响应验证 + 模型切换（DeepSeek→Ollama 本地）演示成功

---

## 7. 全局验收标准总表

| # | 验收项 | 标准 | 阶段 |
|---|---|---|---|
| A1 | 编译 | 9 个服务 `mvn clean package` 全部成功，无 warning 级错误 | 每阶段 |
| A2 | 单测 | 27 个测试文件全绿，数量 ≥ 迁移前 | 0/6 |
| A3 | 启动 | 8 个服务本地 docker-compose 全部 `UP`，日志无 ERROR | 6 |
| A4 | 核心链路 | 登录 → 浏览商品 → 加购 → 下单 → 支付 → 收货 全链路通 | 6 |
| A5 | AI 链路 | 聊天（含流式）+ RAG 问答行为与迁移前一致 | 3/7 |
| A6 | 分布式事务 | Seata 全局回滚验证通过（人为制造库存失败） | 4 |
| A7 | 消息链路 | RocketMQ 订单消息消费正常、无重复消费 | 4 |
| A8 | 网关链路 | 经 gateway 鉴权访问所有服务 200 | 5 |
| A9 | 部署 | Railway 后端 + GitHub Pages 前端可访问（JDK 17） | 6 |
| A10 | Spring AI | 流式输出、结构化输出、模型切换三项 demo 可演示 | 7 |

---

## 8. 部署联动（容易被忽略）

| 位置 | 改动 |
|---|---|
| `railway.toml` | 确认 Java 版本配置指向 17（Railway Nixpacks 默认跟随 `java.version`，但显式声明更稳） |
| `Dockerfile.backend` / `Dockerfile.microservice` | 基础镜像 `eclipse-temurin:8-*` → `eclipse-temurin:17-jre`（或 `17-jdk` 用于构建） |
| `k8s/*` | Deployment 镜像 tag 同步换 JDK 17 基础镜像 |
| `docker-compose*.yml` | 本地编排的基础镜像版本同步 |
| `.github/workflows`（如有） | CI 的 `actions/setup-java` 的 java-version 8 → 17 |

---

## 9. 风险登记册

| # | 风险 | 影响 | 概率 | 缓解措施 |
|---|---|---|---|---|
| R1 | Seata 1.6.1 → 1.8.x 配置/TC 版本不兼容 | 订单事务失效，**面试 demo 核心功能损坏** | 高 | 阶段 4 单独隔离验证；回滚场景优先测试；TC 与客户端版本一致 |
| R2 | Spring Security 6 重写引入鉴权漏洞/失效 | 接口 401/403 或越权 | 中 | 7 个 SecurityConfig 逐个重写 + 鉴权 smoke test 全覆盖 |
| R3 | Nacos config bootstrap 在 SCA 2024 下读取失败 | 服务启动即失败 | 中 | 保留 bootstrap 依赖；失败则切 `spring.config.import`；阶段 2 提前验证 |
| R4 | Sentinel 网关规则/限流失效 | 网关异常 | 中 | 阶段 5 专项验证；Sentinel 控制台规则持久化核对 |
| R5 | javax→jakarta 脚本替换误伤（如第三方库内部） | 编译/运行异常 | 低 | 只替换 `import javax.` 前缀行；编译错误兜底 |
| R6 | RocketMQ 2.3.x 消息序列化行为变化 | 消息消费异常 | 低 | 阶段 4 消息收发 smoke test |
| R7 | 76k 行回归遗漏 | 隐性功能损坏 | 中 | 阶段 0 基线 + 每阶段回归 + 全链路 smoke test 清单 |
| R8 | Spring AI 1.0.0 与 DeepSeek reasoner 兼容细节 | 流式/推理模型异常 | 低 | 阶段 7 先 demo 验证再接入业务 |

---

## 10. 回滚策略

- **分支隔离**：全程在 `feature/java17-boot3` 分支进行，`main` 保持可部署状态，任何阶段失败可随时切回。
- **逐服务回滚**：因为按服务逐个迁移，出问题的服务单独 revert 该服务的 pom + 代码即可，不影响已迁移服务。
- **Seata/RocketMQ 特殊**：若阶段 4 卡住超过预期，允许**降级方案**——order 的 Seata 先临时回退 1.6.1 + 该服务单独保持 Boot 2.7（不阻塞其他服务迁移），最后集中攻坚。
- **Spring AI 独立**：阶段 7 与 0–6 完全解耦，ai-service 的 Spring AI 改动可单独回退为手写 HTTP 调用（`AIService` 保留旧实现直到新实现验证通过，新旧并行切换）。

---

## 11. 参考资料

- Spring AI 官方文档：https://docs.spring.io/spring-ai/reference/
- Spring AI DeepSeek 章节：https://docs.spring.io/spring-ai/reference/api/chat/deepseek-chat.html
- Spring Boot 3.4 迁移指南：https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide（3.x 通用）
- Spring Cloud Alibaba 版本说明：https://sca.aliyun.com/docs/2023/overview/version-explain/
- Seata 版本发布说明（1.7+ Boot 3 支持）：https://github.com/apache/incubator-seata/releases
- Maven Central 实测：`spring-ai-bom` latest = 1.0.0；`spring-ai-starter-model-deepseek` latest = 1.0.0（2026-08-05 查询）

---

*文档结束。执行前请先完成阶段 0 基线并 review 版本矩阵，任何版本号调整需在文档中留痕。*
