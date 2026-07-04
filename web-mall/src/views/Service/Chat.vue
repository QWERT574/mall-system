<template>
  <div class="buyer-chat-page">
    <div class="chat-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18"><path fill-rule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z"/></svg>
      </button>
      <div class="header-center">
        <div class="header-avatar">客</div>
        <div class="header-info">
          <h2>{{ currentSellerName || '在线客服' }}</h2>
          <span class="online-status">
            <span class="online-dot"></span>
            在线
          </span>
        </div>
      </div>
      <button
        class="intervention-btn"
        @click="showInterventionDialog = true"
        :disabled="interventionApplied"
      >
        {{ interventionApplied ? '已申请介入' : '申请介入' }}
      </button>
    </div>

    <div class="session-list-view" v-if="!sessionId && sessionList.length > 0">
      <div class="session-list-header">
        <h3>我的客服会话</h3>
      </div>
      <div class="session-list-body">
        <div v-for="s in sessionList" :key="s.id" class="session-item" @click="openSession(s)">
          <div class="session-avatar">客</div>
          <div class="session-info">
            <div class="session-name">{{ s.sellerName || ('商家 #' + s.sellerId) }}</div>
            <div class="session-preview">{{ s.lastMessage || '暂无消息' }}</div>
          </div>
          <div class="session-meta">
            <span class="session-time">{{ formatTime(s.updatedAt) }}</span>
            <span v-if="s.userUnread > 0" class="session-badge">{{ s.userUnread }}</span>
          </div>
        </div>
      </div>
    </div>

    <template v-if="sessionId">
    <div class="messages-area" ref="messageListRef">
      <div class="welcome-card" v-if="messages.length === 0">
        <div class="welcome-icon">
          <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
            <circle cx="24" cy="24" r="20"/>
            <path d="M16 20c0-4.4 3.6-8 8-8s8 3.6 8 8"/>
            <circle cx="18" cy="26" r="2" fill="currentColor"/>
            <circle cx="30" cy="26" r="2" fill="currentColor"/>
            <path d="M20 32c1.3 1.3 2.7 2 4 2s2.7-.7 4-2"/>
          </svg>
        </div>
        <h3>您好，欢迎咨询！</h3>
        <p>请问有什么可以帮助您的？</p>
      </div>

      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message-row', msg.type]"
      >
        <template v-if="msg.type === 'system'">
          <div class="system-msg">{{ msg.content }}</div>
        </template>

        <template v-else>
          <!-- 头像始终放在前面 -->
          <div class="msg-avatar" :class="msg.type">
            {{ msg.type === 'user' ? '我' : (msg.type === 'admin' ? '管' : '客') }}
          </div>
          <div class="msg-content" :class="msg.type">
            <div v-if="msg.type === 'admin'" class="admin-header">
              <span class="admin-name">平台管理员</span>
              <span v-if="msg.targetType" class="target-badge">{{ msg.targetType }}</span>
            </div>
            <div class="msg-bubble">
              <img v-if="msg.imageUrl" :src="msg.imageUrl" class="msg-image" @click="previewImage(msg.imageUrl)" />
              <span v-else>{{ msg.content }}</span>
            </div>
            <div class="msg-meta">
              <span class="msg-time">{{ msg.time }}</span>
              <span v-if="msg.type === 'user' && msg.status" :class="['msg-status', msg.status]" @click="msg.status === 'failed' && retryMessage(msg.id)" :style="msg.status === 'failed' ? 'cursor:pointer;text-decoration:underline' : ''">
                {{ msg.status === 'sending' ? '发送中...' : msg.status === 'sent' ? '已发送' : msg.status === 'delivered' ? '已送达' : msg.status === 'read' ? '已读' : '点击重试' }}
              </span>
              <span v-if="msg.isRead !== undefined && msg.type === 'user' && msg.status === 'read'" :class="['read-indicator', msg.isRead ? 'read' : 'unread']">
                {{ msg.isRead ? '已读' : '未读' }}
              </span>
            </div>
          </div>
        </template>
      </div>
    </div>

    <div class="order-card-inline" v-if="orderInfo">
      <div class="order-icon">
        <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14"><path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/><path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z"/></svg>
      </div>
      <div class="order-info">
        <span class="order-id">订单 #{{ orderInfo.id }}</span>
        <span class="order-amount">¥{{ orderInfo.totalPrice || 0 }}</span>
      </div>
      <span :class="['order-status', `status-${orderInfo.status}`]">{{ getStatusText(orderInfo.status) }}</span>
    </div>

    <div class="input-area">
      <div class="input-wrapper">
        <el-input
          v-model="messageInput"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="输入您的问题..."
          @keydown.enter.exact.prevent="sendMessage"
          :maxlength="500"
          show-word-limit
        />
        <button class="send-btn" @click="sendMessage" :disabled="!messageInput.trim() || sending">
          <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18"><path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/></svg>
        </button>
      </div>
    </div>

    <el-dialog v-model="showInterventionDialog" title="申请平台介入" width="460px" destroy-on-close>
      <el-form :model="interventionForm" label-width="80px">
        <el-form-item label="申请原因">
          <el-input v-model="interventionForm.reason" type="textarea" :rows="4" placeholder="请详细描述您遇到的问题..." maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="补充凭证">
          <el-input v-model="interventionForm.evidenceImages" type="textarea" :rows="2" placeholder="如有图片证据，请输入图片URL，多个用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInterventionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitIntervention" :loading="submittingIntervention">提交申请</el-button>
      </template>
    </el-dialog>
    </template>

    <!-- 没有 sessionId 时显示空状态(引导用户从商品页/订单页联系商家) -->
    <div v-if="!sessionId" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="64" height="64">
          <path d="M12 14C12 11.8 13.8 10 16 10H48C50.2 10 52 11.8 52 14V42C52 44.2 50.2 46 48 46H30L20 54V46H16C13.8 46 12 44.2 12 42V14Z"/>
          <circle cx="22" cy="28" r="2" fill="currentColor"/>
          <circle cx="32" cy="28" r="2" fill="currentColor"/>
          <circle cx="42" cy="28" r="2" fill="currentColor"/>
        </svg>
      </div>
      <h3>暂无客服会话</h3>
      <p>您还没有任何客服会话</p>
      <p class="empty-tip">从商品页或订单页联系商家后,会话会出现在这里</p>
      <div class="empty-actions">
        <button class="empty-action-btn primary" @click="$router.push('/product')">浏览商品</button>
        <button class="empty-action-btn secondary" @click="$router.push('/order/list')">查看订单</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useChatNotification } from '@/composables/useChatNotification'
import { saveMessages, loadMessages as loadCachedMessages, savePendingMessage, getPendingMessages, removePendingMessage, getLastMessageId, setLastMessageId } from '@/utils/chatStorage'

const getToken = () => localStorage.getItem('token') || ''

const generateId = () => 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)

const route = useRoute()
const router = useRouter()
const messageListRef = ref(null)
const messages = ref([])
const messageInput = ref('')
const sending = ref(false)
const orderInfo = ref(null)
const sessionId = ref(null)
const sessionList = ref([])
const currentSellerName = ref('')

const loadSessionList = async () => {
  try {
    const res = await api.get('/chat/sessions')
    if (res.code === 0 && res.data) {
      sessionList.value = res.data.filter(s => s.status !== 2).sort((a, b) => {
        const ta = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
        const tb = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
        return tb - ta
      })
    }
  } catch (e) { console.error('加载会话列表失败:', e) }
}

const openSession = (session) => {
  sessionId.value = session.id
  currentSellerName.value = session.sellerName || ('商家 #' + session.sellerId)
  loadMessages()
  connectWebSocket()
  connectGlobalWebSocket()
  markMessagesAsRead()
}

const showInterventionDialog = ref(false)
const interventionForm = ref({ reason: '', evidenceImages: '' })
const interventionApplied = ref(false)
const submittingIntervention = ref(false)

const notification = useChatNotification()

const getCurrentUserId = () => userId.value || JSON.parse(localStorage.getItem('userInfo') || '{}').id || 0

const saveCache = () => {
  const uid = getCurrentUserId()
  const sid = sessionId.value
  if (uid && sid) saveMessages(uid, sid, messages.value)
}

const loadCache = () => {
  const uid = getCurrentUserId()
  const sid = sessionId.value
  if (uid && sid) {
    const cached = loadCachedMessages(uid, sid)
    if (cached.length) messages.value = cached
  }
}

const flushPendingQueue = async () => {
  const uid = getCurrentUserId()
  const pending = getPendingMessages(uid)
  if (!pending.length || !sessionId.value) return
  for (const msg of pending) {
    try {
      const res = await api.post('/chat/message', null, {
        params: { sessionId: msg.sessionId || sessionId.value, content: msg.content }
      })
      if (res.code === 0) {
        removePendingMessage(uid, msg._clientMsgId)
        const idx = messages.value.findIndex(m => m._clientMsgId === msg._clientMsgId || m.id === msg._clientMsgId)
        if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'sent', id: res.data.id }
      }
    } catch (e) { /* will retry on next reconnect */ }
  }
}

let stompClient = null
let globalStompClient = null
const wsBaseUrl = 'http://localhost:8081/ws-chat'
let globalSubscription = null
const userId = ref(null)
let readStatusTimer = null
let heartbeatTimer = null
const retryQueue = ref([])
let retryTimer = null
const receivedMessageIds = new Set()

const currentTime = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

const getStatusText = (status) => {
  const statusMap = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
  return statusMap[status] || '未知'
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight
}

const loadOrderInfo = async () => {
  const orderId = route.query.orderId
  if (!orderId) return
  try {
    const response = await api.get(`/order/detail/${orderId}`)
    if (response.code === 0 && response.data) orderInfo.value = response.data
  } catch (error) { console.error('加载订单信息失败:', error) }
}

const sendMessage = async () => {
  if (!messageInput.value.trim()) { ElMessage.warning('请输入消息内容'); return }
  if (!sessionId.value) { ElMessage.error('会话不存在'); return }
  sending.value = true
  const clientMsgId = generateId()
  const content = messageInput.value.trim()
  messages.value.push({ id: clientMsgId, type: 'user', content, imageUrl: null, time: formatTime(new Date().toISOString()), _ts: Date.now(), isRead: false, status: 'sending' })
  messageInput.value = ''
  await scrollToBottom()
  try {
    const response = await api.post('/chat/message', null, { params: { sessionId: sessionId.value, content } })
    if (response.code === 0) {
      const idx = messages.value.findIndex(m => m.id === clientMsgId)
      if (idx !== -1) {
        messages.value[idx] = { ...messages.value[idx], id: response.data.id, _clientMsgId: clientMsgId, status: 'sent', time: formatTime(response.data.createdAt), _ts: new Date(response.data.createdAt || Date.now()).getTime() }
        api.put('/chat/message/' + response.data.id + '/delivered').catch(() => {});
      }
      saveCache()
    } else {
      const idx = messages.value.findIndex(m => m.id === clientMsgId)
      if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'failed' }
      savePendingMessage(getCurrentUserId(), sessionId.value, { _clientMsgId: clientMsgId, content })
      ElMessage.error(response.message || '发送失败')
    }
  } catch (error) {
    const idx = messages.value.findIndex(m => m.id === clientMsgId)
    if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'failed' }
    savePendingMessage(getCurrentUserId(), sessionId.value, { _clientMsgId: clientMsgId, content })
    retryQueue.value.push({ clientMsgId, content, attempt: 1 })
    scheduleRetry()
    console.error('发送消息失败:', error); ElMessage.error('发送失败，将自动重试')
  } finally { sending.value = false }
}

const loadSession = async () => {
  const orderId = route.query.orderId
  const productId = route.query.productId
  const sellerId = route.query.sellerId
  if (sellerId) {
    try {
      const params = { sellerId, productId: productId || null, orderId: orderId || null }
      const response = await api.post('/chat/session', null, { params })
      if (response.code === 0 && response.data) {
        sessionId.value = response.data.id
        currentSellerName.value = response.data.sellerName || ('商家 #' + sellerId)
        await loadMessages()
      }
    } catch (error) { console.error('加载会话失败:', error) }
  } else {
    await loadSessionList()
  }
}

const loadMessages = async () => {
  if (!sessionId.value) return
  try {
    const response = await api.get('/chat/messages/' + sessionId.value)
    if (response.code === 0 && response.data) {
      const snapshot = [...messages.value]
      const snapshotIds = new Set(snapshot.map(m => m.id))
      const loadedMessages = response.data.filter(msg => {
        if (msg.senderType === 1 && msg.senderId !== userId.value) return false;
        if (msg.createdAt && new Date(msg.createdAt) < new Date(Date.now() - 30 * 86400000)) return false;
        return true;
      }).map(msg => {
        // 将加载的消息 ID 加入去重集合
        if (msg.id) receivedMessageIds.add(msg.id)
        return {
          id: msg.id, type: msg.senderType === 1 ? 'user' : msg.senderType === 3 ? 'admin' : 'service',
          content: msg.content || (msg.imageUrl ? '[图片]' : ''), imageUrl: msg.imageUrl || null,
          time: formatTime(msg.createdAt), _ts: new Date(msg.createdAt || 0).getTime(),
          targetType: msg.targetType || null, isRead: msg.isRead === 1,
          senderId: msg.senderId, receiverId: msg.receiverId
        }
      })
      const extraMessages = snapshot.filter(m => !loadedMessages.some(lm => lm.id === m.id))
      const merged = [...loadedMessages, ...extraMessages].sort((a, b) => (a._ts || 0) - (b._ts || 0))
      messages.value = merged
      await markMessagesAsRead()
      await scrollToBottom()
    }
  } catch (error) { console.error('加载消息失败:', error) }
}

const markMessagesAsRead = async () => {
  if (!sessionId.value) return
  try {
    const unreadMessages = messages.value.filter(msg => (msg.type === 'service' || msg.type === 'admin') && msg.isRead === false)
    if (unreadMessages.length > 0) {
      await api.post('/chat/read', null, { params: { sessionId: sessionId.value } })
      unreadMessages.forEach(msg => { msg.isRead = true })
      const unreadMsgIds = unreadMessages.filter(m => m.id && !String(m.id).startsWith('msg_')).map(m => m.id);
      for (const msgId of unreadMsgIds) {
          api.put('/chat/message/' + msgId + '/read').catch(() => {});
      }
    }
  } catch (error) { console.warn('标记已读失败:', error) }
}

const pad = (n) => String(n).padStart(2, '0');

const parseDate = (t) => {
  if (!t) return null;
  const d = new Date(t);
  if (isNaN(d.getTime())) return null;
  return d;
};

const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const d = parseDate(timeStr);
  if (!d) return timeStr;
  const now = new Date();
  const timeStr2 = pad(d.getHours()) + ':' + pad(d.getMinutes());
  if (d.toDateString() === now.toDateString()) return timeStr2;
  const yest = new Date(now); yest.setDate(yest.getDate() - 1);
  if (d.toDateString() === yest.toDateString()) return '昨天 ' + timeStr2;
  if (d.getFullYear() === now.getFullYear()) return pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + timeStr2;
  return d.getFullYear() + '/' + pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + timeStr2;
};

const goBack = () => { router.back() }

const submitIntervention = async () => {
  if (!interventionForm.value.reason.trim()) { ElMessage.warning('请输入申请原因'); return }
  submittingIntervention.value = true
  try {
    const response = await api.post('/chat/request-intervention', null, {
      params: { sessionId: sessionId.value, reason: interventionForm.value.reason.trim(), evidenceImages: interventionForm.value.evidenceImages.trim() || null }
    })
    if (response.code === 0) { ElMessage.success('介入申请已提交'); interventionApplied.value = true; showInterventionDialog.value = false; interventionForm.value = { reason: '', evidenceImages: '' } }
    else { ElMessage.error(response.message || '提交失败') }
  } catch (error) { console.error('提交介入申请失败:', error); ElMessage.error('提交失败，请稍后重试') } finally { submittingIntervention.value = false }
}

const previewImage = (imageUrl) => { window.open(imageUrl, '_blank') }

const startReadStatusRefresh = () => {
  readStatusTimer = setInterval(async () => {
    if (!sessionId.value) return
    try {
      const response = await api.get('/chat/messages/' + sessionId.value)
      if (response.code === 0 && response.data) {
        messages.value.forEach(localMsg => {
          if (localMsg.type !== 'user') return
          const serverMsg = response.data.find(m => m.id === localMsg.id || m.id === localMsg._clientMsgId)
          if (serverMsg && serverMsg.senderType === 1) {
            localMsg.isRead = serverMsg.isRead === 1
            if (serverMsg.isRead === 1 && localMsg.status === 'sent') {
              localMsg.status = 'read'
            }
          }
        })
      }
    } catch (error) { console.warn('刷新已读状态失败:', error) }
  }, 5000)
}

const stopReadStatusRefresh = () => { if (readStatusTimer) { clearInterval(readStatusTimer); readStatusTimer = null } }

const startHeartbeat = () => {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  api.post('/chat/online').catch(err => console.warn('标记上线失败:', err))
  heartbeatTimer = setInterval(() => {
    api.post('/chat/online').catch(err => console.warn('心跳刷新失败:', err))
  }, 5 * 60 * 1000)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
}

const scheduleRetry = () => {
  if (retryTimer) clearTimeout(retryTimer)
  if (retryQueue.value.length === 0) return
  const next = retryQueue.value[0]
  const delay = Math.min(1000 * Math.pow(2, next.attempt - 1), 30000)
  retryTimer = setTimeout(async () => {
    if (!sessionId.value) { retryQueue.value.shift(); scheduleRetry(); return }
    try {
      const response = await api.post('/chat/message', null, { params: { sessionId: sessionId.value, content: next.content } })
      if (response.code === 0) {
        const idx = messages.value.findIndex(m => m.id === next.clientMsgId)
        if (idx !== -1) {
          messages.value[idx] = { ...messages.value[idx], id: response.data.id, status: 'sent', time: formatTime(response.data.createdAt), _ts: new Date(response.data.createdAt || Date.now()).getTime() }
        }
        saveCache()
      } else {
        next.attempt++
        if (next.attempt <= 5) scheduleRetry()
        else {
          const idx = messages.value.findIndex(m => m.id === next.clientMsgId)
          if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'failed' }
        }
        return
      }
    } catch (e) {
      next.attempt++
      if (next.attempt <= 5) scheduleRetry()
      else {
        const idx = messages.value.findIndex(m => m.id === next.clientMsgId)
        if (idx !== -1) messages.value[idx] = { ...messages.value[idx], status: 'failed' }
      }
      return
    }
    retryQueue.value.shift()
    scheduleRetry()
  }, delay)
}

const retryMessage = (msgId) => {
  const idx = messages.value.findIndex(m => m.id === msgId)
  if (idx === -1) return
  const msg = messages.value[idx]
  if (msg.status !== 'failed') return
  messages.value[idx] = { ...msg, status: 'sending' }
  retryQueue.value.push({ clientMsgId: msgId, content: msg.content, attempt: 1 })
  scheduleRetry()
}

const connectWebSocket = () => {
  if (!sessionId.value) return
  const token = localStorage.getItem('token') || ''
  startHeartbeat()
  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsBaseUrl),
    connectHeaders: {
      Authorization: token ? `Bearer ${token}` : ''
    },
    reconnectDelay: 5000,
    onConnect: (frame) => {
              console.log('WebSocket connected:', frame)
              loadOfflineMessages()
              flushPendingQueue()
      stompClient.subscribe('/topic/chat/session/' + sessionId.value, (messageOutput) => {
        const receivedMessage = JSON.parse(messageOutput.body)
        if (receivedMessage.type === 'message_status') {
            const idx = messages.value.findIndex(m => m.id === receivedMessage.messageId);
            if (idx !== -1) {
                messages.value[idx] = { ...messages.value[idx], status: receivedMessage.status };
                if (receivedMessage.status === 3) messages.value[idx].isRead = true;
            }
            return;
        }
        if (receivedMessage.senderType === 1 && String(receivedMessage.senderId) === String(userId.value)) return
        if (receivedMessage.id && receivedMessageIds.has(receivedMessage.id)) return
        if (receivedMessage.id) receivedMessageIds.add(receivedMessage.id)
        const msgType = receivedMessage.senderType === 3 ? 'admin' : 'service'
        const newMsg = { id: receivedMessage.id, type: msgType, content: receivedMessage.content || (receivedMessage.imageUrl ? '[图片]' : ''), imageUrl: receivedMessage.imageUrl || null, time: formatTime(receivedMessage.createdAt), _ts: new Date(receivedMessage.createdAt || 0).getTime(), isRead: false }
        messages.value.push(newMsg)
        scrollToBottom()
        notification.notify('新消息', receivedMessage.content || '[图片]')
        newMsg.isRead = true
        saveCache()
      })
    },
    onStompError: (error) => { console.error('STOMP error:', error) },
    onDisconnect: (frame) => { console.log('WebSocket disconnected:', frame) }
  })
  stompClient.activate()
}

const connectGlobalWebSocket = () => {
  if (!userId.value) return
  // 如果已有连接，先断开
  if (globalStompClient && globalStompClient.active) {
    globalStompClient.deactivate()
    globalSubscription = null
  }
  
  globalStompClient = new Client({
    webSocketFactory: () => new SockJS(wsBaseUrl),
    connectHeaders: {
      Authorization: getToken() ? `Bearer ${getToken()}` : ''
    },
    reconnectDelay: 5000,
    onConnect: (frame) => {
      console.log('Global WebSocket connected:', frame)
      const userTopic = '/topic/chat/user/' + userId.value
      // 订阅用户个人频道，处理管理员消息和已读通知
      globalSubscription = globalStompClient.subscribe(userTopic, (messageOutput) => {
        const receivedData = JSON.parse(messageOutput.body)
        
        // 使用 Set 去重，防止重复消息
        if (receivedData.id && receivedMessageIds.has(receivedData.id)) {
          return
        }
        
        if (receivedData.senderType !== undefined && receivedData.senderType === 3) {
          // 管理员消息
          if (receivedData.id) receivedMessageIds.add(receivedData.id)
          const newMsg = { id: receivedData.id, type: 'admin', content: receivedData.content || (receivedData.imageUrl ? '[图片]' : ''), imageUrl: receivedData.imageUrl || null, time: formatTime(receivedData.createdAt), _ts: new Date(receivedData.createdAt || 0).getTime(), targetType: receivedData.targetType || null, isRead: true }
          messages.value.push(newMsg)
          scrollToBottom()
          notification.notify('管理员消息', receivedData.content || '[图片]')
          return
        }
        if (receivedData.type === 'read') {
          messages.value.forEach(msg => { if (msg.type === 'user' && msg.isRead !== undefined) msg.isRead = true })
          return
        }
        if (receivedData.type === 'message_status') {
            const idx = messages.value.findIndex(m => m.id === receivedData.messageId);
            if (idx !== -1) {
                messages.value[idx] = { ...messages.value[idx], status: receivedData.status };
                if (receivedData.status === 3) messages.value[idx].isRead = true;
            }
            return;
        }
        if (receivedData.type === 'intervention_requested') { ElMessage.warning('对方已申请平台介入'); interventionApplied.value = true; return }
        if (receivedData.type === 'admin_intervened') { ElMessage.info('管理员已介入此会话'); return }
        if (sessionId.value && sessionId.value === receivedData.sessionId) return
        else { ElMessage.info('您有新的客服消息'); notification.notify('客服消息', '您有新的客服消息，请查看') }
      })
    },
    onStompError: (error) => { console.error('Global STOMP error:', error) },
    onDisconnect: (frame) => { console.log('Global WebSocket disconnected:', frame) }
  })
  globalStompClient.activate()
}

const disconnectWebSocket = () => {
  if (stompClient && stompClient.active) { stompClient.deactivate(); api.post('/chat/offline').catch(err => console.warn('标记下线失败:', err)) }
}

const loadOfflineMessages = async () => {
  try {
    const response = await api.get('/chat/offline/messages')
    if (response.code === 0 && response.data && response.data.length > 0) {
      response.data.forEach(msg => {
        if (msg.id) receivedMessageIds.add(msg.id)
        const msgType = msg.senderType === 1 ? 'user' : msg.senderType === 3 ? 'admin' : 'service'
        const exists = messages.value.some(m => m.id === msg.id)
        if (!exists) { 
          messages.value.push({ 
            id: msg.id, 
            type: msgType, 
            content: msg.content || (msg.imageUrl ? '[图片]' : ''), 
            imageUrl: msg.imageUrl || null, 
            time: formatTime(msg.createdAt), _ts: new Date(msg.createdAt || 0).getTime(), 
            isRead: false,
            targetType: msg.targetType || null
          }) 
        }
      })
      ElMessage.success(`收到 ${response.data.length} 条离线消息`)
      scrollToBottom()
    }
  } catch (error) { console.error('加载离线消息失败:', error) }
}

const disconnectGlobalWebSocket = () => {
  if (globalSubscription) {
    try { globalSubscription.unsubscribe() } catch (e) {}
    globalSubscription = null
  }
  if (globalStompClient && globalStompClient.active) {
    globalStompClient.deactivate()
  }
  globalStompClient = null
}

const playNotificationSound = () => {
  notification.playSound()
}

onMounted(() => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const token = localStorage.getItem('token') || ''
  userId.value = userInfo.id
  notification.requestPermission()
  loadOrderInfo()
  loadSession().then(() => {
    loadCache()
    setTimeout(connectWebSocket, 1000)
    if (userId.value) setTimeout(connectGlobalWebSocket, 1500)
    setTimeout(startReadStatusRefresh, 2000)
  })
  scrollToBottom()
})

onUnmounted(() => { disconnectWebSocket(); disconnectGlobalWebSocket(); stopReadStatusRefresh(); stopHeartbeat(); if (retryTimer) { clearTimeout(retryTimer); retryTimer = null } })
</script>

<style scoped>


/* 柔和渐变风格 */
.buyer-chat-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f0f4ff 0%, #faf5ff 50%, #fff5f5 100%);
  font-family: 'Quicksand', sans-serif;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.session-list-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.session-list-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}
.session-list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}
.session-list-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
.session-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;
}
.session-item:hover {
  background: #f0f4ff;
}
.session-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}
.session-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}
.session-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
.session-preview {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}
.session-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}
.session-time {
  font-size: 11px;
  color: #bbb;
}
.session-badge {
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(139, 92, 246, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.06);
  color: #7c3aed;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover { background: rgba(139, 92, 246, 0.12); }

.header-center {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
}

.header-info h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e1b4b;
}

.online-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #10b981;
  font-weight: 500;
}

.online-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: blink 2s ease-in-out infinite;
}

.intervention-btn {
  padding: 7px 14px;
  border: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.06);
  color: var(--color-primary-600);
  font-size: 12px;
  font-family: 'Quicksand', sans-serif;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.intervention-btn:hover:not(:disabled) { background: rgba(245, 158, 11, 0.12); border-color: rgba(245, 158, 11, 0.4); }
.intervention-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.messages-area::-webkit-scrollbar { width: 3px; }
.messages-area::-webkit-scrollbar-thumb { background: rgba(139, 92, 246, 0.12); border-radius: 2px; }

.welcome-card {
  text-align: center;
  padding: 40px 20px;
  color: #7c3aed;
  animation: fadeIn 0.6s ease both;
}

.welcome-icon { margin-bottom: 16px; opacity: 0.4; }

.welcome-card h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: #1e1b4b;
}

.welcome-card p {
  margin: 0;
  font-size: 13px;
  color: #8b8ba7;
}

.message-row {
  display: flex;
  width: 100%;
  margin-bottom: 16px;
  align-items: flex-start;
  animation: msgSlide 0.3s ease both;
}

/* 用户消息 - 靠右对齐 */
.message-row.user {
  flex-direction: row-reverse;
}

/* 客服/管理员消息 - 靠左对齐 */
.message-row.service,
.message-row.admin {
  flex-direction: row;
}

.message-row.system { justify-content: center; }

.system-msg {
  padding: 6px 16px;
  border-radius: 12px;
  background: rgba(139, 92, 246, 0.06);
  color: #8b8ba7;
  font-size: 12px;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

/* 客服头像 - 紫色 */
.msg-avatar.service {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  color: #fff;
}

/* 管理员头像 - 琥珀色 */
.msg-avatar.admin {
  background: linear-gradient(135deg, var(--color-warning) 0%, #fbbf24 100%);
  color: #fff;
}

/* 用户头像 - 粉色 */
.msg-avatar.user {
  background: linear-gradient(135deg, #ec4899 0%, #f472b6 100%);
  color: #fff;
}

.msg-content {
  margin: 0 10px;
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

/* 用户消息内容靠右 */
.msg-content.user {
  align-items: flex-end;
}

/* 客服/管理员消息内容靠左 */
.msg-content.service,
.msg-content.admin {
  align-items: flex-start;
}

.admin-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.admin-name { font-size: 11px; color: var(--color-primary-600); font-weight: 600; }

.target-badge {
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(245, 158, 11, 0.1);
  color: var(--color-primary-600);
  font-size: 10px;
  font-weight: 500;
}

.msg-bubble {
  padding: 10px 16px;
  border-radius: 14px;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
  max-width: 100%;
}

/* 客服消息 - 白色背景，左下角尖角 */
.msg-content.service .msg-bubble {
  background: #fff;
  border: 1px solid rgba(139, 92, 246, 0.08);
  color: #1e1b4b;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.04);
}

/* 管理员消息 - 琥珀色背景，左下角尖角 */
.msg-content.admin .msg-bubble {
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.15);
  color: #1e1b4b;
  border-bottom-left-radius: 4px;
}

/* 用户消息 - 紫罗兰渐变，右下角尖角 */
.msg-content.user .msg-bubble {
  background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 12px rgba(124, 58, 237, 0.2);
}

.msg-image {
  max-width: 180px;
  max-height: 140px;
  border-radius: 10px;
  cursor: pointer;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

/* 用户消息的元信息靠右 */
.msg-content.user .msg-meta {
  justify-content: flex-end;
}

/* 客服/管理员消息的元信息靠左 */
.msg-content.service .msg-meta,
.msg-content.admin .msg-meta {
  justify-content: flex-start;
}

.msg-time { font-size: 10px; color: #8b8ba7; }

.msg-status {
  font-size: 10px;
  font-weight: 500;
}
.msg-status.sending { color: var(--color-warning); }
.msg-status.sent { color: #8b8ba7; }
.msg-status.read { color: #10b981; }
.msg-status.failed { color: var(--color-error); }

.read-indicator {
  font-size: 10px;
  color: #8b8ba7;
  font-weight: 500;
}
.read-indicator.read { color: #10b981; }
.read-indicator.unread { color: var(--color-warning); }

.order-card-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin: 0 16px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(139, 92, 246, 0.06);
}

.order-icon { color: #8b5cf6; display: flex; align-items: center; }

.order-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-id { font-size: 12px; font-weight: 600; color: #1e1b4b; }
.order-amount { font-size: 12px; color: #8b8ba7; }

.order-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.status-0 { background: rgba(245, 158, 11, 0.1); color: var(--color-primary-600); }
.status-1 { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.status-2 { background: rgba(139, 92, 246, 0.1); color: #7c3aed; }
.status-3 { background: rgba(16, 185, 129, 0.1); color: #059669; }
.status-4 { background: rgba(156, 163, 175, 0.1); color: #6b7280; }

.input-area {
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-top: 1px solid rgba(139, 92, 246, 0.06);
}

.input-wrapper {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

:deep(.el-textarea__inner) {
  background: rgba(139, 92, 246, 0.03);
  border: 1px solid rgba(139, 92, 246, 0.1);
  border-radius: 12px;
  color: #1e1b4b;
  font-family: 'Quicksand', sans-serif;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  resize: none;
}

:deep(.el-textarea__inner:focus) {
  border-color: rgba(139, 92, 246, 0.3);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.06);
}

:deep(.el-textarea__inner::placeholder) { color: #b4b4c7; }

.send-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}

.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 空状态(没有 sessionId 时) */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  color: #6b7280;
  animation: fadeIn 0.5s ease both;
}
.empty-icon {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8b5cf6;
  opacity: 0.4;
  margin-bottom: 16px;
}
.empty-state h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1e1b4b;
}
.empty-state p {
  margin: 0;
  font-size: 13px;
  color: #8b8ba7;
  line-height: 1.6;
}
.empty-state .empty-tip {
  font-size: 12px;
  color: #b4b4c7;
  margin-top: 2px;
  margin-bottom: 24px;
}
.empty-actions {
  display: flex;
  gap: 12px;
}
.empty-action-btn {
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: 1.5px solid transparent;
}
.empty-action-btn.primary {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  color: #fff;
}
.empty-action-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(139, 92, 246, 0.3);
}
.empty-action-btn.secondary {
  background: transparent;
  color: #8b5cf6;
  border-color: rgba(139, 92, 246, 0.25);
}
.empty-action-btn.secondary:hover {
  background: rgba(139, 92, 246, 0.06);
  border-color: rgba(139, 92, 246, 0.4);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes msgSlide {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

@media (max-width: 768px) {
  .chat-header { padding: 10px 12px; }
  .header-center { gap: 6px; }
  .header-avatar { width: 32px; height: 32px; font-size: 13px; border-radius: 10px; }
  .header-info h2 { font-size: 14px; }
  .intervention-btn { padding: 5px 10px; font-size: 11px; }
  .messages-area { padding: 12px; }
  .msg-content { max-width: 85%; }
  .msg-avatar { width: 30px; height: 30px; font-size: 11px; border-radius: 8px; }
  .msg-bubble { padding: 8px 12px; font-size: 13px; }
  .msg-image { max-width: 140px; max-height: 110px; }
  .input-area { padding: 8px 10px; }
  .send-btn { width: 38px; height: 38px; border-radius: 10px; }
  .session-item { padding: 10px 14px; }
  .session-avatar { width: 36px; height: 36px; font-size: 13px; }
  .order-card-inline { margin: 0 10px; padding: 8px 12px; }
}

@media (max-width: 480px) {
  .chat-header { padding: 8px 10px; }
  .back-btn { width: 32px; height: 32px; }
  .header-avatar { width: 28px; height: 28px; font-size: 12px; }
  .header-info h2 { font-size: 13px; }
  .online-status { font-size: 10px; }
  .intervention-btn { padding: 4px 8px; font-size: 10px; }
  .messages-area { padding: 8px; }
  .msg-content { max-width: 90%; margin: 0 6px; }
  .msg-avatar { width: 28px; height: 28px; font-size: 10px; }
  .msg-bubble { padding: 7px 10px; font-size: 13px; border-radius: 12px; }
  .msg-image { max-width: 120px; max-height: 90px; }
  .input-area { padding: 6px 8px; }
  .input-wrapper { gap: 6px; }
  .send-btn { width: 34px; height: 34px; border-radius: 8px; }
  :deep(.el-textarea__inner) { font-size: 13px; }
  .order-card-inline { margin: 0 8px; padding: 6px 10px; flex-wrap: wrap; gap: 6px; }
  .session-item { padding: 8px 10px; }
  .session-avatar { width: 32px; height: 32px; font-size: 12px; }
  .session-name { font-size: 13px; }
  .session-preview { font-size: 11px; }
}
</style>
