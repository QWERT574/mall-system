import request from '@/utils/request'

// ==================== 类型定义 ====================

export interface KnowledgeDocument {
  id: number
  title: string
  content: string
  sourceType: number      // 0-手动录入 1-文档导入 2-FAQ转化 3-历史对话
  category: string
  tags: string
  status: number          // 0-禁用 1-启用
  chunkCount: number
  vectorizedChunkCount: number
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeFaq {
  id: number
  question: string
  answer: string
  category: string
  keywords: string
  priority: number
  status: number
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface ConversationSession {
  id: number
  sessionToken: string
  userId: number
  serviceType: number
  status: number          // 0-活跃 1-已关闭
  messageCount: number
  createdAt: string
  updatedAt: string
}

export interface ConversationMessage {
  id: number
  sessionId: number
  role: string            // user / assistant
  content: string
  sources: string
  retrievalScore: number
  responseTimeMs: number
  createdAt: string
}

export interface KnowledgeStats {
  documentCount: number
  faqCount: number
  vectorizedChunkCount: number
  vectorStoreChunkCount: number
  vectorStoreFaqCount: number
}

export interface DocumentListParams {
  page: number
  size: number
  category?: string
  status?: number
}

export interface FaqListParams {
  page: number
  size: number
  category?: string
}

// ==================== 知识文档 API ====================

export const getDocumentList = (params: DocumentListParams) => {
  return request.get('/knowledge/documents', { params })
}

export const getDocumentDetail = (id: number) => {
  return request.get(`/knowledge/documents/${id}`)
}

export const createDocument = (data: Partial<KnowledgeDocument>) => {
  return request.post('/knowledge/documents', data)
}

export const updateDocument = (id: number, data: Partial<KnowledgeDocument>) => {
  return request.put(`/knowledge/documents/${id}`, data)
}

export const deleteDocument = (id: number) => {
  return request.delete(`/knowledge/documents/${id}`)
}

export const searchDocuments = (keyword: string) => {
  return request.get('/knowledge/documents/search', { params: { keyword } })
}

export const vectorizeDocument = (id: number) => {
  return request.post(`/knowledge/documents/${id}/vectorize`)
}

export const vectorizeAllDocuments = () => {
  return request.post('/knowledge/documents/vectorize-all')
}

export const setDocumentStatus = (id: number, status: number) => {
  return request.put(`/knowledge/documents/${id}/status`, null, { params: { status } })
}

// ==================== FAQ API ====================

export const getFaqList = (params: FaqListParams) => {
  return request.get('/knowledge/faqs', { params })
}

export const createFaq = (data: Partial<KnowledgeFaq>) => {
  return request.post('/knowledge/faqs', data)
}

export const updateFaq = (id: number, data: Partial<KnowledgeFaq>) => {
  return request.put(`/knowledge/faqs/${id}`, data)
}

export const deleteFaq = (id: number) => {
  return request.delete(`/knowledge/faqs/${id}`)
}

// ==================== 对话历史 API ====================

export const getConversations = (userId?: number) => {
  return request.get('/knowledge/conversations', { params: { userId } })
}

export const getConversationMessages = (sessionId: number) => {
  return request.get(`/knowledge/conversations/${sessionId}/messages`)
}

export const closeConversation = (sessionId: number) => {
  return request.post(`/knowledge/conversations/${sessionId}/close`)
}

// ==================== 统计 API ====================

export const getKnowledgeStats = () => {
  return request.get('/knowledge/stats')
}
