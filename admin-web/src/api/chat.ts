import request from '@/utils/request'
import type { ChatSession, ChatMessage } from '@/stores/chat'

export function getOrCreateSession(params: {
  sellerId: number
  productId?: number
  orderId?: number
}): Promise<ChatSession> {
  return request.post<ChatSession>('/chat/session', null, { params }) as any
}

export function getUserSessions(): Promise<ChatSession[]> {
  return request.get<ChatSession[]>('/chat/sessions') as any
}

export function getSessionDetail(id: number): Promise<ChatSession> {
  return request.get<ChatSession>(`/chat/sessions/${id}`) as any
}

export function closeSession(id: number, reason?: string): Promise<void> {
  return request.put<void>(`/chat/session/${id}/close`, null, {
    params: { reason: reason || 'user_close' }
  }) as any
}

export function getMessages(sessionId: number, page = 1, size = 50): Promise<ChatMessage[]> {
  return request.get<ChatMessage[]>(`/chat/messages/${sessionId}`, {
    params: { page, size }
  }) as any
}

export function checkOnlineStatus(targetId: number, type = 'user'): Promise<{ isOnline: boolean }> {
  return request.get<{ isOnline: boolean }>(`/chat/status/${targetId}`, {
    params: { type }
  }) as any
}

export function getOfflineMessages(): Promise<ChatMessage[]> {
  return request.get<ChatMessage[]>('/chat/offline/messages') as any
}

export function getUnreadCount(): Promise<{ totalUnread: number }> {
  return request.get<{ totalUnread: number }>('/chat/unread/count') as any
}

export function getQuickReplies(): Promise<{ id: number; category: string; content: string }[]> {
  return request.get<{ id: number; category: string; content: string }[]>('/chat/quick-replies') as any
}
