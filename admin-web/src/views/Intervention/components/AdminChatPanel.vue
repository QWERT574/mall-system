<template>
  <div class="admin-chat-panel">
    <div class="chat-layout">
      <div class="session-list">
        <div class="session-header">
          <h3>会话列表</h3>
          <el-tag size="small" type="success">在线: {{ activeCount }}</el-tag>
        </div>
        <div class="session-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索会话..."
            size="small"
            clearable
            prefix-icon="Search"
          />
        </div>
        <div class="session-items" v-loading="loading">
          <div
            v-for="session in filteredSessions"
            :key="session.id"
            class="session-item"
            :class="{ active: activeSession?.id === session.id }"
            @click="selectSession(session)"
          >
            <div class="session-info">
              <div class="session-title">
                会话 #{{ session.id }}
                <el-tag
                  v-if="session.status === 0"
                  size="small"
                  type="danger"
                >待处理</el-tag>
                <el-tag
                  v-else-if="session.status === 1"
                  size="small"
                  type="warning"
                >处理中</el-tag>
                <el-tag
                  v-else
                  size="small"
                  type="info"
                >已关闭</el-tag>
              </div>
              <div class="session-meta">
                <span>用户: {{ session.userId }}</span>
                <span v-if="session.sellerId"> | 商家: {{ session.sellerId }}</span>
              </div>
              <div class="session-time">{{ session.createdAt }}</div>
            </div>
          </div>
          <el-empty v-if="!loading && filteredSessions.length === 0" description="暂无会话" />
        </div>
      </div>

      <div class="chat-area">
        <div v-if="activeSession" class="chat-content">
          <div class="chat-header">
            <h4>会话 #{{ activeSession.id }}</h4>
            <div>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="activeSession.status === 2"
                @click="handleCloseSession"
              >
                关闭会话
              </el-button>
            </div>
          </div>

          <div class="messages" ref="messagesRef">
            <div
              v-for="msg in messages"
              :key="msg.id || msg.timestamp"
              class="message-item"
              :class="msg.senderType === 'admin' ? 'admin-msg' : 'user-msg'"
            >
              <div class="message-header">
                <el-tag size="small" :type="msg.senderType === 'admin' ? 'success' : 'primary'">
                  {{ msg.senderType === 'admin' ? '管理员' : msg.senderType === 'seller' ? '商家' : '用户' }}
                </el-tag>
                <span class="message-time">{{ msg.createdAt || msg.timestamp }}</span>
              </div>
              <div class="message-content">{{ msg.content }}</div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="newMessage"
              type="textarea"
              :rows="2"
              placeholder="输入消息..."
              @keydown.enter.prevent="sendMessage"
            />
            <div class="input-actions">
              <span class="char-count">{{ newMessage.length }} / 500</span>
              <el-button type="primary" :disabled="!newMessage.trim()" @click="sendMessage">
                发送
              </el-button>
            </div>
          </div>
        </div>

        <div v-else class="no-session">
          <el-empty description="请选择一个会话" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getChatSessions,
  getChatMessages,
  sendAdminMessage,
  closeChatSession,
  getActiveSessionCount,
  type ChatSessionRecord,
  type ChatMessageRecord
} from '@/api/adminChat'

const userStore = useUserStore()
const loading = ref(false)
const sessions = ref<ChatSessionRecord[]>([])
const activeSession = ref<ChatSessionRecord | null>(null)
const messages = ref<(ChatMessageRecord | any)[]>([])
const newMessage = ref('')
const activeCount = ref(0)
const searchKeyword = ref('')
const messagesRef = ref<HTMLElement | null>(null)

const isLoggedIn = computed(() => {
  const hasToken = !!userStore.token
  const hasUserInfo = !!userStore.userInfo
  const hasUserId = !!(userStore.userInfo && (userStore.userInfo.id || userStore.userInfo.userId))
  return hasToken && (hasUserInfo || hasUserId)
})

const filteredSessions = computed(() => {
  if (!searchKeyword.value) return sessions.value
  const keyword = searchKeyword.value.toLowerCase()
  return sessions.value.filter(
    (s) =>
      s.id.toString().includes(keyword) ||
      s.userId.toString().includes(keyword) ||
      (s.sellerId && s.sellerId.toString().includes(keyword))
  )
})

async function fetchSessions() {
  if (!isLoggedIn.value) {
    sessions.value = []
    return
  }

  loading.value = true
  try {
    const res = await getChatSessions({ page: 1, size: 50 })
    const data = res
    sessions.value = data?.records || []
  } catch (e: any) {
    console.error('获取会话列表失败:', e)
  } finally {
    loading.value = false
  }
}

async function fetchActiveCount() {
  try {
    const res = await getActiveSessionCount()
    activeCount.value = res || 0
  } catch {
    console.warn('获取活跃会话数失败')
  }
}

async function selectSession(session: ChatSessionRecord) {
  activeSession.value = session
  messages.value = []
  try {
    const res = await getChatMessages(session.id)
    messages.value = res || []
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    ElMessage.error('获取消息失败')
  }
}

async function sendMessage() {
  if (!newMessage.value.trim() || !activeSession.value) return
  const content = newMessage.value.trim()
  newMessage.value = ''

  const adminId = userStore.userInfo?.id || userStore.userInfo?.userId || 1
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    await sendAdminMessage({
      sessionId: activeSession.value.id,
      content,
      adminId
    })
    messages.value.push({
      id: Date.now(),
      content,
      senderType: 'admin',
      createdAt: new Date().toISOString()
    })
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    ElMessage.error('发送消息失败')
  }
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

async function handleCloseSession() {
  if (!activeSession.value) return
  try {
    await ElMessageBox.confirm('确定要关闭此会话吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await closeChatSession(activeSession.value.id)
    ElMessage.success('会话已关闭')
    activeSession.value.status = 2
    fetchSessions()
  } catch {
    console.log('取消关闭')
  }
}

onMounted(async () => {
  await ensureUserInfo()
  if (isLoggedIn.value) {
    fetchSessions()
    fetchActiveCount()
  }
})

async function ensureUserInfo() {
  if (userStore.userInfo?.id) return

  const token = userStore.token || localStorage.getItem('admin_token')
  if (!token) return

  try {
    const res = await fetch('/api/auth/userinfo', {
      headers: { Authorization: `Bearer ${token}` }
    })
    const data = await res.json()
    if (data.code === 0 && data.data) {
      userStore.userInfo = data.data
      localStorage.setItem('admin_user', JSON.stringify(data.data))
    }
  } catch (e) {
    console.warn('获取用户信息失败:', e)
  }
}
</script>

<style scoped>
.admin-chat-panel {
  height: calc(100vh - 200px);
  min-height: 500px;
}

.chat-layout {
  display: flex;
  height: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.session-list {
  width: 300px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.session-header h3 {
  margin: 0;
  font-size: 16px;
}

.session-search {
  padding: 8px 16px;
}

.session-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 12px;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.session-item:hover {
  background: #ecf5ff;
}

.session-item.active {
  background: #409eff;
  color: white;
}

.session-item.active .session-meta,
.session-item.active .session-time {
  color: rgba(255, 255, 255, 0.8);
}

.session-title {
  font-weight: 600;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 2px;
}

.session-time {
  font-size: 11px;
  color: #c0c4cc;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h4 {
  margin: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--color-bg-page);
}

.message-item {
  margin-bottom: 16px;
  max-width: 70%;
}

.message-item.user-msg {
  margin-right: auto;
}

.message-item.admin-msg {
  margin-left: auto;
}

.message-header {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
}

.message-content {
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.5;
  word-break: break-all;
}

.user-msg .message-content {
  background: white;
  border: 1px solid #e4e7ed;
}

.admin-msg .message-content {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
  background: white;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.char-count {
  font-size: 12px;
  color: #c0c4cc;
}

.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
