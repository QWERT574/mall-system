import request from '@/utils/request'

export interface ChatMessageRecord {
  id: number
  sessionId: number
  senderId: number
  senderType: string
  content: string
  messageType: string
  createdAt: string
}

export interface ChatSessionRecord {
  id: number
  userId: number
  sellerId: number
  status: number
  createdAt: string
  updatedAt: string
}

export function getChatSessions(params: {
  page: number
  size: number
  status?: number
}) {
  return request.get('/chat/sessions', { params }) as Promise<any>
}

export function getChatSessionDetail(id: number) {
  return request.get(`/chat/sessions/${id}`) as Promise<any>
}

export function getChatMessages(sessionId: number) {
  return request.get(`/chat/messages/${sessionId}`) as Promise<any>
}

export function sendAdminMessage(data: {
  sessionId: number
  content: string
  adminId: number
}) {
  return request.post('/chat/send', data) as Promise<any>
}

export function closeChatSession(sessionId: number) {
  return request.post(`/chat/close/${sessionId}`) as Promise<any>
}

export function getActiveSessionCount() {
  return request.get('/chat/active-count') as Promise<any>
}
