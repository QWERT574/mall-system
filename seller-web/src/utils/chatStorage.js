const STORAGE_PREFIX = 'chat_cache_'
const PENDING_PREFIX = 'chat_pending_'

export function saveMessages(userId, sessionId, messages) {
  try {
    const key = STORAGE_PREFIX + userId + '_' + sessionId
    const toSave = messages.slice(-100).map(m => ({
      id: m.id, type: m.type, content: m.content, imageUrl: m.imageUrl,
      time: m.time, _ts: m._ts, isRead: m.isRead, status: m.status,
      _clientMsgId: m._clientMsgId
    }))
    localStorage.setItem(key, JSON.stringify(toSave))
  } catch (e) {}
}

export function loadMessages(userId, sessionId) {
  try {
    const key = STORAGE_PREFIX + userId + '_' + sessionId
    const data = localStorage.getItem(key)
    return data ? JSON.parse(data) : []
  } catch (e) { return [] }
}

export function savePendingMessage(userId, sessionId, message) {
  try {
    const key = PENDING_PREFIX + userId
    const existing = getPendingMessages(userId)
    existing.push({ ...message, sessionId, savedAt: Date.now() })
    localStorage.setItem(key, JSON.stringify(existing.slice(-50)))
  } catch (e) {}
}

export function getPendingMessages(userId) {
  try {
    const key = PENDING_PREFIX + userId
    const data = localStorage.getItem(key)
    return data ? JSON.parse(data) : []
  } catch (e) { return [] }
}

export function removePendingMessage(userId, clientMsgId) {
  const pending = getPendingMessages(userId).filter(m => m._clientMsgId !== clientMsgId)
  localStorage.setItem(PENDING_PREFIX + userId, JSON.stringify(pending))
}

export function clearPendingMessages(userId) {
  localStorage.removeItem(PENDING_PREFIX + userId)
}

export function getLastMessageId(userId, sessionId) {
  try {
    const key = 'chat_sync_' + userId + '_' + sessionId
    return parseInt(localStorage.getItem(key) || '0', 10)
  } catch (e) { return 0 }
}

export function setLastMessageId(userId, sessionId, msgId) {
  try {
    const key = 'chat_sync_' + userId + '_' + sessionId
    localStorage.setItem(key, String(msgId))
  } catch (e) {}
}