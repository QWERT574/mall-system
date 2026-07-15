<template>
  <div class="chat-page">
    <div class="chat-container">
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>会话列表</h3>
          <el-badge :value="chatStore.totalUnread" :hidden="chatStore.totalUnread === 0">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </el-badge>
        </div>
        <SessionList
          :sessions="chatStore.sessions"
          :current-session-id="chatStore.currentSessionId"
          @select="handleSelectSession"
          @close="handleCloseSession"
        />
      </div>

      <div class="chat-main">
        <div v-if="!chatStore.currentSession" class="chat-placeholder">
          <el-icon :size="64" color="#c0c4cc"><ChatLineSquare /></el-icon>
          <p>选择一个会话开始聊天</p>
        </div>

        <template v-else>
          <div class="chat-header">
            <AgentStatusBar
              :session="chatStore.currentSession"
              :agents="chatStore.agents"
              :is-typing="chatStore.isTyping"
              :typing-user-id="chatStore.typingUserId"
              @transfer="showTransferDialog = true"
              @close="handleCloseSession(chatStore.currentSession!.id)"
            />
          </div>

          <div class="chat-body" ref="chatBodyRef">
            <MessageBubble
              v-for="msg in chatStore.currentMessages"
              :key="msg.id || msg.clientMsgId"
              :message="msg"
              :is-mine="isMyMessage(msg)"
            />
            <div v-if="chatStore.isTyping && chatStore.typingUserId !== currentUserId"
                 class="typing-indicator">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-text">对方正在输入...</span>
            </div>
          </div>

          <div class="chat-footer">
            <div class="chat-toolbar">
              <FaqPanel @insert="handleInsertFaq" />
              <el-button text @click="handleEmoji">
                <el-icon><PictureRounded /></el-icon>
              </el-button>
              <el-upload
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :before-upload="beforeImageUpload"
                :on-success="handleImageUploadSuccess"
                accept="image/*"
              >
                <el-button text>
                  <el-icon><Picture /></el-icon>
                </el-button>
              </el-upload>
              <el-upload
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :before-upload="beforeFileUpload"
                :on-success="handleFileUploadSuccess"
                accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.zip"
              >
                <el-button text>
                  <el-icon><FolderOpened /></el-icon>
                </el-button>
              </el-upload>
            </div>
            <div class="chat-input-area">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="3"
                placeholder="输入消息，按Enter发送，Shift+Enter换行"
                @keydown.enter.exact="handleSend"
                @input="handleTyping"
                resize="none"
              />
              <el-button
                type="primary"
                :disabled="!inputMessage.trim()"
                @click="handleSend"
                class="send-btn"
              >
                发送
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <TransferDialog
      v-model:visible="showTransferDialog"
      :session="chatStore.currentSession"
      :agents="chatStore.agents"
      @confirm="handleTransfer"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, ChatLineSquare, Picture } from '@element-plus/icons-vue'
import PictureRounded from '@element-plus/icons-vue/dist/es/components/PictureRounded.vue'
import FolderOpened from '@element-plus/icons-vue/dist/es/components/FolderOpened.vue'
import { useChatStore } from '@/stores/chat'
import { useStompClient } from '@/composables/useStompClient'
import { useChatNotification } from '@/composables/useChatNotification'
import { getUserSessions, getMessages, closeSession, getOfflineMessages } from '@/api/chat'
import { listAgents, transferSession } from '@/api/customerService'
import SessionList from './components/SessionList.vue'
import MessageBubble from './components/MessageBubble.vue'
import AgentStatusBar from './components/AgentStatusBar.vue'
import TransferDialog from './components/TransferDialog.vue'
import FaqPanel from './components/FaqPanel.vue'

const chatStore = useChatStore()
const notification = useChatNotification()
const { connect, disconnect, subscribe, send, unsubscribe } = useStompClient()

const chatBodyRef = ref<HTMLElement | null>(null)
const inputMessage = ref('')
const showTransferDialog = ref(false)
const currentUserId = ref<number>(0)

const uploadUrl = ref('/api/upload/chat')
const uploadHeaders = ref<Record<string, string>>({})

const typingTimer = ref<ReturnType<typeof setTimeout> | null>(null)

onMounted(async () => {
  const token = localStorage.getItem('admin_token') || ''
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUserId.value = user.id || user.userId || 0
      uploadHeaders.value = { Authorization: `Bearer ${token}` }
    } catch (e) {}
  }

  notification.requestPermission()

  try {
    const [sessionsRes, agentsRes] = await Promise.all([
      getUserSessions(),
      listAgents()
    ])
    chatStore.setSessions(sessionsRes || [])
    chatStore.setAgents(agentsRes || [])
  } catch (e) {
    console.error('[Chat] Failed to load initial data:', e)
  }

  connect({
    token,
    userType: 'seller',
    onConnected: () => {
      chatStore.setConnectionStatus(true)
      loadOfflineMessages()
      subscribeSessionTopics()
    },
    onDisconnected: () => {
      chatStore.setConnectionStatus(false)
    }
  })

  loadOfflineMessages()
})

onUnmounted(() => {
  unsubscribeAll()
  disconnect()
})

function subscribeSessionTopics() {
  chatStore.sessions.forEach(session => {
    subscribe(`/topic/chat/session/${session.id}`, (msg: any) => {
      handleIncomingMessage(msg)
    })
  })
  subscribe('/user/queue/notifications', (notification: any) => {
    handleNotification(notification)
  })
  subscribe('/user/queue/ack', (ack: any) => {
    handleAck(ack)
  })
}

function unsubscribeAll() {
  chatStore.sessions.forEach(session => {
    unsubscribe(`/topic/chat/session/${session.id}`)
  })
  unsubscribe('/user/queue/notifications')
  unsubscribe('/user/queue/ack')
}

function handleIncomingMessage(msg: any) {
  if (msg.type === 'new_message') {
    chatStore.addMessage(msg.sessionId, {
      id: msg.id,
      sessionId: msg.sessionId,
      senderId: msg.senderId,
      senderType: msg.senderType,
      content: msg.content,
      messageType: msg.messageType,
      imageUrl: msg.imageUrl,
      fileName: msg.fileName,
      fileSize: msg.fileSize,
      isAutoReply: msg.isAutoReply,
      isRead: msg.isRead,
      createdAt: msg.createdAt
    })
    if (msg.sessionId !== chatStore.currentSessionId) {
      notification.notify('新消息', msg.content?.substring(0, 50) || '收到新消息')
    }
    scrollToBottom()
  } else if (msg.type === 'message_read') {
    chatStore.markMessagesRead(msg.sessionId, [])
  } else if (msg.type === 'typing') {
    chatStore.setTyping(msg.userId, msg.isTyping)
  }
}

function handleNotification(notification: any) {
  if (notification.type === 'session_transferred') {
    handleTransferNotification(notification)
  }
}

function handleAck(ack: any) {
  if (ack.type === 'message_sent' && ack.clientMsgId) {
    if (chatStore.currentSessionId) {
      chatStore.updateMessageStatus(chatStore.currentSessionId, ack.serverMsgId, 'sent')
    }
  }
}

function handleTransferNotification(notification: any) {
  ElMessage.info(`会话 ${notification.sessionId} 已转接给您`)
  reloadSessions()
}

async function handleSelectSession(session: any) {
  chatStore.setCurrentSession(session)

  if (!chatStore.messagesMap.get(session.id)?.length) {
    try {
      const res = await getMessages(session.id)
      chatStore.setMessages(session.id, res || [])
    } catch (e) {
      console.error('[Chat] Failed to load messages:', e)
    }
  }

  await nextTick()
  scrollToBottom()

  subscribe(`/topic/chat/session/${session.id}`, (msg: any) => {
    handleIncomingMessage(msg)
  })

  send('/tcp/chat/read', { sessionId: session.id })
}

async function handleCloseSession(sessionId: number) {
  try {
    await closeSession(sessionId)
    chatStore.markSessionRead(sessionId)
    ElMessage.success('会话已关闭')
    reloadSessions()
  } catch (e) {
    ElMessage.error('关闭会话失败')
  }
}

function handleSend() {
  const content = inputMessage.value.trim()
  if (!content || !chatStore.currentSessionId) return

  const clientMsgId = `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

  const localMsg = {
    id: 0,
    sessionId: chatStore.currentSessionId!,
    senderId: currentUserId.value,
    senderType: 2,
    content,
    messageType: 1,
    isRead: 0,
    createdAt: new Date().toISOString(),
    clientMsgId,
    status: 'sending'
  }
  chatStore.addMessage(chatStore.currentSessionId!, localMsg as any)

  send('/tcp/chat/send', {
    sessionId: chatStore.currentSessionId!,
    messageType: 1,
    content,
    clientMsgId
  })

  inputMessage.value = ''
  chatStore.setTyping(0, false)
  scrollToBottom()
}

function handleTyping() {
  if (!chatStore.currentSessionId) return
  send('/tcp/chat/typing', {
    sessionId: chatStore.currentSessionId,
    isTyping: !!inputMessage.value.trim()
  })

  if (typingTimer.value) clearTimeout(typingTimer.value)
  typingTimer.value = setTimeout(() => {
    send('/tcp/chat/typing', {
      sessionId: chatStore.currentSessionId,
      isTyping: false
    })
  }, 2000)
}

function handleTransfer(params: { sessionId: number; fromAgentId: number; toAgentId: number; reason?: string }) {
  transferSession(params).then(() => {
    ElMessage.success('会话已转接')
    showTransferDialog.value = false
  }).catch(() => {
    ElMessage.error('转接失败')
  })
}

function handleInsertFaq(content: string) {
  inputMessage.value = content
}

function handleEmoji() {
  inputMessage.value += '😊'
}

function handleImageUploadSuccess(response: any) {
  if (!chatStore.currentSessionId) return
  const url = response.data?.url || response.url
  send('/tcp/chat/send', {
    sessionId: chatStore.currentSessionId,
    messageType: 2,
    content: '[图片]',
    imageUrl: url,
    clientMsgId: `img_${Date.now()}`
  })
}

function handleFileUploadSuccess(response: any) {
  if (!chatStore.currentSessionId) return
  const url = response.data?.url || response.url
  send('/tcp/chat/send', {
    sessionId: chatStore.currentSessionId,
    messageType: 3,
    content: '[文件]',
    imageUrl: url,
    fileName: response.fileName || '文件',
    fileSize: response.fileSize || 0,
    clientMsgId: `file_${Date.now()}`
  })
}

function beforeImageUpload(file: File) {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过10MB')
    return false
  }
  return true
}

function beforeFileUpload(file: File) {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  return true
}

function isMyMessage(msg: any): boolean {
  return msg.senderId === currentUserId.value ||
    (String(msg.senderType) === '2' || String(msg.senderType) === '3' ||
     msg.senderType === 2 || msg.senderType === 3)
}

async function loadOfflineMessages() {
  try {
    const res = await getOfflineMessages()
    if (res && res.length > 0) {
      res.forEach((msg: any) => {
        chatStore.addMessage(msg.sessionId, msg)
      })
    }
  } catch (e) {}
}

async function reloadSessions() {
  try {
    const res = await getUserSessions()
    chatStore.setSessions(res || [])
  } catch (e) {}
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

watch(() => chatStore.currentSessionId, () => {
  scrollToBottom()
})
</script>

<style scoped lang="scss">
.chat-page {
  height: calc(100vh - 120px);
  padding: 0;
  background: var(--color-bg-page);
}

.chat-container {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.chat-sidebar {
  width: 320px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #ebeef5;

    h3 { margin: 0; font-size: 16px; }
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  p { margin-top: 16px; font-size: 15px; }
}

.chat-header {
  border-bottom: 1px solid #ebeef5;
  padding: 12px 20px;
  flex-shrink: 0;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--color-bg-page);
}

.chat-footer {
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
}

.chat-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.chat-input-area {
  display: flex;
  padding: 12px 16px;
  gap: 12px;

  :deep(.el-textarea__inner) {
    border: none;
    box-shadow: none;
    resize: none;
    &:focus { box-shadow: none; }
  }

  .send-btn {
    align-self: flex-end;
    flex-shrink: 0;
  }
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;

  .typing-dot {
    width: 7px; height: 7px;
    border-radius: 50%;
    background: #c0c4cc;
    animation: typing-bounce 1.4s infinite ease-in-out both;
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
    &:nth-child(3) { animation-delay: 0s; }
  }

  .typing-text {
    margin-left: 8px;
    font-size: 12px;
    color: var(--color-text-tertiary);
  }
}

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
