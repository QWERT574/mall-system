# 代码质量改进计划

> **目标：** 清理冗余代码 + 统一响应格式，展示专业开发流程

**发现的问题：**

| 问题 | 严重性 | 说明 |
|------|--------|------|
| `ApiResponse.java` 死代码 | 🔴 冗余 | 与 `Result.java` 完全重复（同字段、同方法），且未被任何文件引用 |
| `ResultTest.java` 测试缺失 | 🟡 测试覆盖 | 只有文件骨架，无真实测试逻辑 |
| AIService.java 2000+ 行 | 🟠 职责过大 | 混合了对话、RAG、日志、推荐等多种职责 |

---

## Task 1: 删除死代码 + 增加测试

**Objective:** 删除 `ApiResponse.java`，为 `Result.java` 补上真实测试

**文件：**
- 删除: `backend/src/main/java/com/example/minimall/dto/ApiResponse.java`
- 修改: `backend/src/test/java/com/example/minimall/common/ResultTest.java`

### 步骤

1. 确认 `ApiResponse` 真无引用 — 已确认（grep 结果仅为自身文件中的定义）
2. 删除 `ApiResponse.java`
3. 重写 `ResultTest.java` 覆盖全部方法
4. Maven 编译验证
5. Commit

---

## Task 2: AIController 响应格式统一

**Objective:** AIController 的 `createSuccessResponse`/`createErrorResponse` 手拼 Map -> 改为使用 `Result.success()`/`Result.error()`

**为什么要做：**
- 现有 `createSuccessResponse(Map)` 返回 `{code, message, data}` 结构
- `Result.success()` 也返回 `{code, message, data}` 结构（JSON 序列化后完全一致）
- 消除重复代码，统一全项目响应格式

**文件：**
- 修改: `AIController.java:422-436` — 替换两个私有方法
- 验证: 编译 + 测试全量

**风险：** 极低。JSON 输出结构不变（都是 `{code, message, data}`），前端无感知。

---

## Task 3: 从 AIService 提取 AiLogService

**Objective:** 将 AIService 中 8 个日志管理方法提取到独立的 `AiLogService`

**为什么要做：** 日志管理是独立的职责（CRUD on ai_service_log 表），与 AI 对话/RAG/推荐无任何共享状态，提取后：
- AIService 减少 70 行 + 1 个 Mapper 依赖
- 职责边界清晰，后续改日志逻辑不影响 AI 对话

**文件：**
- 创建: `backend/src/main/java/com/example/minimall/service/AiLogService.java`
- 修改: `backend/src/main/java/com/example/minimall/controller/AIController.java` — 注入 AiLogService，路由日志请求
- 修改: `backend/src/main/java/com/example/minimall/service/AIService.java` — 移除 aiServiceLogMapper + 8 个日志方法

**验证：** `mvn compile` + `mvn test`

**风险：** 低。日志 CRUD 与 AI 逻辑无耦合，提取后接口签名不变。
