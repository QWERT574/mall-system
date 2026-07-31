import request from '@/utils/request'

// ==================== 类型定义 ====================

export interface RagDashboard {
  totalQueries: number
  totalLlmCalls: number
  totalLlmSuccess: number
  totalRagHits: number
  totalRagMisses: number
  hitRate: number
  llmSuccessRate: number
  healthScore: number
  avgRetrievalTimeMs: number
  avgEmbeddingTimeMs: number
  intentDistribution: Record<string, { count: number; percentage: number }>
  recentQueries: string[]
}

export interface TopQuery {
  query: string
  count: number
}

export interface VectorSearchInfo {
  searchStrategy: {
    chunkCount: number
    faqCount: number
    hnswThreshold: number
    chunkHnswEnabled: boolean
    faqHnswEnabled: boolean
    chunkHnswSize?: number
    faqHnswSize?: number
    hnswM: number
    hnswEfConstruction: number
    hnswEfSearch: number
  }
  embeddingModel: string
  embeddingDimension: number
  usingExternalApi: boolean
  lexiconInfo: {
    domainLexiconSize: number
    synonymEntries: number
    semanticSlots: number
    dimension: number
  }
}

export interface EmbeddingQuality {
  model: string
  dimension: number
  positiveAvg: number
  negativeAvg: number
  discrimination: number
  passed: boolean
  positivePairs: { text1: string; text2: string; similarity: number }[]
  negativePairs: { text1: string; text2: string; similarity: number }[]
}

export interface IntentStatistics {
  total: number
  intents: Record<string, { count: number; percentage: number; label: string }>
}

export interface KnowledgeCoverage {
  coverageRate: number
  totalKeywords: number
  coveredCount: number
  uncoveredCount: number
  needAlert: boolean
  faqCount: number
  alertThreshold: number
}

export interface RagTestResult {
  response: string
  ragEnabled: boolean
  retrievalScore: number
  retrievalTimeMs: number
  responseTimeMs: number
  sourceCount: number
  sources: {
    type: string
    title?: string
    score: number
    snippet: string
  }[]
  intent?: string
  intentLabel?: string
  sessionToken?: string
}

// ==================== 监控 API ====================

/** 获取 RAG 监控仪表盘 */
export const getRagDashboard = () => {
  return request.get('/ai/monitor/dashboard')
}

/** 获取向量检索策略信息 */
export const getVectorSearchInfo = () => {
  return request.get('/ai/monitor/vector-search')
}

/** 获取 Embedding 语义质量 */
export const getEmbeddingQuality = () => {
  return request.get('/ai/monitor/embedding-quality')
}

/** 获取意图分类统计 */
export const getIntentStatistics = () => {
  return request.get('/ai/monitor/intents')
}

/** 获取知识库覆盖率 */
export const getKnowledgeCoverage = () => {
  return request.get('/ai/monitor/coverage')
}

/** 获取内容过滤审计日志 */
export const getFilterAuditLogs = (limit = 50) => {
  return request.get('/ai/monitor/filter-audit', { params: { limit } })
}

/** 手动触发种子FAQ初始化 */
export const initSeedFaqs = () => {
  return request.post('/ai/monitor/init-seed-faqs')
}

// ==================== RAG 测试 API ====================

/** 执行 RAG 测试查询 */
export const testRagQuery = (query: string, serviceType = 1) => {
  return request.post('/ai/rag-query', {
    query,
    serviceType,
    userId: 0
  })
}

// ==================== AI 日志 API ====================

export interface AILog {
  id: number
  userId: number
  query: string
  response: string
  serviceType: number
  createdAt: string
}

export interface AILogPage {
  total: number
  records: AILog[]
  current: number
  size: number
}

/** 获取 AI 服务日志（分页） */
export const getAILogs = (params: { page?: number; size?: number; userId?: number; serviceType?: number }) => {
  return request.get('/ai/logs', { params })
}

/** 删除单条 AI 日志 */
export const deleteAILog = (id: number) => {
  return request.delete(`/ai/logs/${id}`)
}

/** 清空 AI 日志 */
export const clearAILogs = (userId?: number, serviceType?: number) => {
  return request.delete('/ai/logs/clear', { params: { userId, serviceType } })
}
