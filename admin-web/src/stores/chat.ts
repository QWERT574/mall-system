import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface ChatMessage {
  id: number
  sessionId: number
  senderId: number
  senderType: number | string
  content: string
  messageType: number
  imageUrl?: string
  fileName?: string
  fileSize?: number
  isAutoReply?: boolean
  isRead?: number
  createdAt: string
  clientMsgId?: string
  status?: string
  senderName?: string
}

export interface ChatSession {
  id: number
  userId: number
  sellerId: number
  agentId?: number
  productId?: number
  orderId?: number
  status: number
  sessionType: number
  autoReplyEnabled?: number
  userUnread: number
  sellerUnread: number
  lastMessageAt: string
  createdAt: string
  userName?: string
  sellerName?: string
  productName?: string
  agentName?: string
  lastMessage?: string
  isOnline?: boolean
  type?: string
}

export interface CustomerServiceAgent {
  id: number
  userId: number
  agentName: string
  agentCode: string
  skillGroup: string
  status: number
  maxConcurrent: number
  currentSessions: number
  totalServed: number
  avatarUrl?: string
  username?: string
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<number | null>(null)
  const currentSession = ref<ChatSession | null>(null)
  const messagesMap = ref<Map<number, ChatMessage[]>>(new Map())
  const agents = ref<CustomerServiceAgent[]>([])
  const connected = ref(false)
  const isTyping = ref(false)
  const typingUserId = ref<number | null>(null)

  const currentMessages = computed(() => {
    if (!currentSessionId.value) return []
    return messagesMap.value.get(currentSessionId.value) || []
  })

  const totalUnread = computed(() => {
    return sessions.value.reduce((sum, s) =>
      sum + (s.userUnread || 0), 0)
  })

  function setSessions(list: ChatSession[]) {
    sessions.value = list
  }

  function setCurrentSession(session: ChatSession | null) {
    currentSession.value = session
    currentSessionId.value = session?.id ?? null
  }

  function setMessages(sessionId: number, messages: ChatMessage[]) {
    messagesMap.value.set(sessionId, messages)
  }

  function addMessage(sessionId: number, message: ChatMessage) {
    const msgs = messagesMap.value.get(sessionId) || []
    const exists = msgs.find(m => m.id === message.id ||
      (message.clientMsgId && m.clientMsgId === message.clientMsgId))
    if (!exists) {
      msgs.push(message)
      messagesMap.value.set(sessionId, [...msgs])
    } else {
      Object.assign(exists, message)
    }
  }

  function updateMessageStatus(sessionId: number, messageId: number, status: string) {
    const msgs = messagesMap.value.get(sessionId)
    if (msgs) {
      const msg = msgs.find(m => m.id === messageId)
      if (msg) msg.status = status
      messagesMap.value.set(sessionId, [...msgs])
    }
  }

  function markSessionRead(sessionId: number) {
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      session.userUnread = 0
      session.sellerUnread = 0
    }
  }

  function markMessagesRead(sessionId: number, messageIds: number[]) {
    const msgs = messagesMap.value.get(sessionId)
    if (msgs) {
      msgs.forEach(m => {
        if (messageIds.includes(m.id)) {
          m.isRead = 1
        }
      })
      messagesMap.value.set(sessionId, [...msgs])
    }
  }

  function clearMessages(sessionId: number) {
    messagesMap.value.delete(sessionId)
  }

  function setTyping(userId: number | null, typing: boolean) {
    typingUserId.value = userId
    isTyping.value = typing
  }

  function setConnectionStatus(status: boolean) {
    connected.value = status
  }

  function setAgents(list: CustomerServiceAgent[]) {
    agents.value = list
  }

  return {
    sessions,
    currentSessionId,
    currentSession,
    messagesMap,
    currentMessages,
    totalUnread,
    agents,
    connected,
    isTyping,
    typingUserId,
    setSessions,
    setCurrentSession,
    setMessages,
    addMessage,
    updateMessageStatus,
    markSessionRead,
    markMessagesRead,
    clearMessages,
    setTyping,
    setConnectionStatus,
    setAgents
  }
})
