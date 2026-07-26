# AGENTS.md

Guidance for coding agents (any provider) working in this repository. This file
is the single authoritative instruction source; `CLAUDE.md` is a thin pointer to
this file — edit here, not there.

## Project Overview

乡村振兴 (Rural Revitalization) — a B2C agricultural e-commerce platform with AI-powered customer service. Multi-tenant: buyers, sellers, admins, and customer-service agents.

The backend has evolved from a monolith into a **Spring Cloud microservice architecture** (current mainline): an API gateway plus six business microservices and a shared `common` module, registered via Nacos, with RocketMQ async messaging, Seata distributed transactions, and Sentinel gateway rate limiting. The original monolith `backend/` is retained as a **transitional service** (`minimall-service`) — it still owns a few routes the gateway forwards back to it (see Gateway routing below) and can also run standalone.

## Startup Matrix (authoritative entries)

Pick exactly one path; do not mix entries from different rows.

| Path | Authoritative entry | Prerequisites | When to use |
|---|---|---|---|
| **单体路径 (monolith)** | `docker-compose up -d` (MySQL + Redis + backend:8081) | Docker; `backend/.env` (copy from `.env.example`) | 快速本地联调、后端单点调试、devcontainer 默认路径 |
| **微服务路径 (microservices, mainline)** | ① `docker compose -f docker-compose-infra.yml up -d`（Nacos/RocketMQ/Seata）② `docker compose -f docker-compose-apps.yml up -d`（MySQL/Redis/gateway:8080 + 6 services） | Docker; 根目录 `.env`; infra 必须先于 apps 启动 | 主线架构验证、网关路由、跨服务联调 |

Notes:
- 单体路径的等价 Windows 入口：`start-all.bat`（Docker MySQL/Redis + Maven 启动 backend）；微服务路径的本地非容器入口：`start-microservices.ps1`。这些是便捷封装，权威入口以上表为准。
- devcontainer 仅覆盖**单体路径**（其 compose 只含 MySQL/Redis）；在 devcontainer 内走微服务路径需使用根目录的两个 compose 文件。
- 前端 dev 代理目标当前不一致：`web-mall` 指向网关 `:8080`（微服务路径），`admin-web` 与 `seller-web` 指向 `:8081`（单体路径）。切换路径时核对各自 `vite.config`。

## Development Commands

### Backend monolith (Spring Boot 2.7.9 / Java 8 / Maven) — transitional

```bash
# Build and run (port 8081; conflicts with user-service if both run natively)
cd backend
mvn spring-boot:run

# Build only
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Run with Docker profile
mvn spring-boot:run -Dspring-boot.run.profiles=docker

# Health check
curl http://localhost:8081/actuator/health
```

### Frontends (Vue 3 / Vite)

```bash
# Admin dashboard (port 3001, TypeScript)
cd admin-web && npm run dev

# Seller portal (port 5173)
cd seller-web && npm run dev

# Web mall (port 5176)
cd web-mall && npm run dev

# Build for production
cd <project> && npm run build
```

Each frontend proxies `/api`, `/uploads`, `/images`, and `/ws-chat` in dev mode; targets differ per app (see Startup Matrix note).

### Microservices (Spring Cloud 2021.0.9 / Spring Cloud Alibaba 2021.0.5.0)

```bash
# 1. Start middleware first: Nacos + RocketMQ (namesrv/broker) + Seata
docker compose -f docker-compose-infra.yml up -d

# 2. Start MySQL + Redis + gateway + 6 microservices
docker compose -f docker-compose-apps.yml up -d

# Or on Windows, build & start all microservices locally
.\start-microservices.ps1

# Build a single service (all share the same Maven layout)
cd <service> && mvn clean package -DskipTests
```

All microservice images are built with the shared `Dockerfile.microservice` (multi-stage: Maven + Temurin 17 JRE; sources still target Java 8).

### Docker (monolith path)

```bash
docker-compose up -d          # MySQL + Redis + Backend
docker-compose down           # Stop services
docker-compose up -d --build  # Rebuild backend image
docker-compose logs -f backend
```

### Windows Quick-Start Scripts

```
start-all.bat    # Start MySQL (Docker), Redis (Docker), and backend (Maven)
stop-all.bat     # Stop everything
restart-all.bat  # Restart everything
```

## Architecture

### Microservices (current mainline)

| Module | Port | Responsibility |
|---|---|---|
| `gateway/` | 8080 | Spring Cloud Gateway (WebFlux) — unified entry, JWT auth, routing, Sentinel rate limiting |
| `user-service/` | 8081 | Auth, users, addresses, captcha/SMS |
| `product-service/` | 8082 | Products, categories, activities, coupons, discounts; serves `/uploads` `/images` static resources |
| `order-service/` | 8083 | Orders, cart, after-sales |
| `payment-service/` | 8084 | Payments |
| `chat-service/` | 8085 | Customer-service sessions, chat, WebSocket (`/ws-chat`), admin chat/intervention |
| `ai-service/` | 8086 | AI customer service, FAQ, RAG knowledge base |
| `common/` | — | Shared module (Result wrapper, exceptions, utilities) used by all services |
| `backend/` | 8081 | Monolith (`minimall-service`), transitional — see routing below |

Infrastructure: **Nacos** (service discovery), **RocketMQ** (async messages, used by order/product), **Seata** (distributed transactions), **Sentinel** (gateway rate limiting). Started via `docker-compose-infra.yml`.

**Gateway routing** (`gateway/src/main/resources/application.yml`): `/api/auth|user|address|captcha|sms` → user-service; `/api/product|category|activity|coupon|discount` → product-service; `/api/order|cart|aftersale` → order-service; `/api/payment` → payment-service; `/api/cs|chat` + `/api/admin/chat|intervention` + `/ws-chat` → chat-service; `/api/ai|faq|rag|knowledge` → ai-service; `/uploads` `/images` → product-service. Still routed to the **monolith** (`lb://minimall-service`): `/api/upload|seller|review|system|debug|image|product-category` and `/actuator/**` — migrate these before decommissioning `backend/`.

### Backend monolith: Layered (Controller → Service → Mapper) — transitional

Base package: `com.example.minimall` — 233 Java source files.

**Request flow:**
```
Filter chain (JwtAuth → XSS → RateLimit → TraceId)
  → Controller (29 REST controllers)
    → Service interface (43 interfaces)
      → ServiceImpl (business logic, @Transactional)
        → Mapper (43 MyBatis-Plus interfaces, some backed by XML in resources/mapper/*.xml)
```

**Key cross-cutting layers:**
- **Auth**: `SecurityConfig` defines public route whitelist (products, categories, activities, coupons, AI/FAQ, actuators are public). JWT is required for everything else. Role-level access is enforced by `PermissionInterceptor` (not Spring Security annotations).
- **Unified response**: `Result<T>` wrapper used by all controllers. `GlobalExceptionHandler` catches `BusinessException` and other exceptions.
- **Observability**: `TraceIdFilter` injects a UUID into MDC (log pattern includes `%X{traceId}`). `PerformanceInterceptor` logs requests > 500ms. `ApiLoggingInterceptor` logs all HTTP traffic. Actuator/Prometheus endpoints are exposed.
- **Rate limiting**: `@RateLimit` annotation + `RateLimitInterceptor` backed by Redis.
- **WebSocket**: STOMP over SockJS at `/ws-chat`. Buyers, sellers, and CS agents communicate via `ChatStompController`, `ChatMessageHandler`, `AdminChatHandler`. Messages are persisted.

### AI / RAG Pipeline

```
User query → IntentClassifierService (intent routing)
  → RagService:
     1. Query expansion (synonyms + domain terms)
     2. EmbeddingService (API-based 1536-dim or local TF-IDF fallback)
     3. Hybrid search: vector cosine (HnswIndex) + keyword BM25 → RRF fusion
     4. Rerank (multi-dimensional feature scoring)
     5. Context assembly with conversation history
  → AIService: sends assembled prompt to DeepSeek-v4-flash
  → ContentFilterService: filter response for sensitive content
  → RagCacheService: cache results
  → AiLogService: log interaction to AIServiceLog table
```

- **EmbeddingService**: tries external API first (OpenAI-compatible, 1536 dims), falls back to local TF-IDF (lower precision but offline-capable).
- **HnswIndex**: in-memory hierarchical navigable small world graph for O(log n) cosine similarity search.
- **RagMonitorService** tracks hit rates and evaluation metrics.
- **AdminInterventionService**: allows human agents to take over AI conversations.

### Database: MySQL 8.0, 36 tables

- **ORM**: MyBatis-Plus 3.5.3.1 (NOT JPA). Entities are in `model/`, mappers in `mapper/`.
- **RBAC**: `User → UserRole → Role → RolePermission → Permission`
- **Soft deletes**: MyBatis-Plus `logic-delete` configured with `deleted` column (0 = active, 1 = deleted).
- **Schema init**: `resources/sql/init_database.sql` (705 lines) auto-runs on first Docker startup.
- Test profile uses MySQL (not H2) — requires a running local MySQL instance.

### Configuration: Environment-Driven

All sensitive values come from environment variables (loaded via `dotenv-java` from `backend/.env`). See `.env.example` for the template. Key env vars: `DB_HOST`, `DB_PASSWORD`, `REDIS_PASSWORD`, `DEEPSEEK_API_KEY`, `EMBEDDING_API_KEY`, `JWT_SECRET`, `RAG_ENABLED`.

### Frontend: Shared UI Library

`shared-ui/` contains CSS design tokens, base styles, and 5 reusable Vue components (`MlButton`, `MlCard`, `MlInput`, `MlModal`, `MlTag`). All three frontends reference it via Vite alias `@mall/shared-ui`. Admin-web is the only frontend using TypeScript.

### Mini-Program

WeChat native mini-program in `mini-program/`. No third-party UI framework. 21 pages, 5 tab bar items. Communicates with the backend over HTTPS.

## Testing

- Framework: JUnit 5 + Mockito + Spring Boot Test
- `backend/src/test/resources/application-test.yml` — test profile with reduced connection pools, NoLoggingImpl for MyBatis, and `spring.sql.init.mode: never`
- Run: `cd backend && mvn test`
- Tests require a running MySQL instance at `localhost:3306` with database `minimall`
- CI: `.github/workflows/backend-test.yml` runs `mvn test` with MySQL/Redis service containers

## Key Files

| File | Purpose |
|---|---|
| `backend/src/main/resources/application.yml` | All backend configuration |
| `backend/src/main/resources/sql/init_database.sql` | Full schema + seed data (36 tables) |
| `backend/pom.xml` | Maven dependencies |
| `backend/Dockerfile` | Multi-stage Docker build for the monolith (Maven + Temurin 17 JRE; sources target Java 8) |
| `Dockerfile.microservice` | Shared multi-stage build for all microservices |
| `docker-compose.yml` | Monolith-path orchestration (MySQL + Redis + backend) |
| `docker-compose-infra.yml` | Middleware: Nacos + RocketMQ + Seata |
| `docker-compose-apps.yml` | MySQL + Redis + gateway + 6 microservices |
| `.env.example` | Environment variable template |
| `README.md` | Comprehensive documentation (architecture, security, deployment) |
