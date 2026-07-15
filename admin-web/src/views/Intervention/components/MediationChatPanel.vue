<template>
  <div class="mediation-chat-panel">
    <div class="mediation-header" v-if="interventionData">
      <div class="party-info buyer-info">
        <el-tag type="primary" size="large">
          <el-icon style="margin-right: 4px;"><User /></el-icon>
          买家: {{ buyerInfo?.nickname || buyerInfo?.username || ('用户#' + buyerInfo?.id) }}
        </el-tag>
      </div>
      <div class="vs-divider">
        <el-icon :size="20" color="#909399"><ChatDotRound /></el-icon>
        <span>调解中</span>
      </div>
      <div class="party-info seller-info">
        <el-tag type="warning" size="large">
          <el-icon style="margin-right: 4px;"><Shop /></el-icon>
          商家: {{ sellerInfo?.nickname || sellerInfo?.username || sellerInfo?.shopName || ('商家#' + sellerInfo?.id) }}
        </el-tag>
      </div>
    </div>

    <div class="chat-messages" ref="messagesRef" v-loading="loading">
      <div v-if="messages.length === 0 && !loading" class="no-messages">
        <el-empty description="暂无聊天记录" :image-size="60" />
      </div>
      <div
        v-for="msg in messages"
        :key="msg.id || msg.createdAt"
        class="message-item"
        :class="getMessageClass(msg)"
      >
        <div class="message-sender">
          <el-tag
            :type="getSenderTagType(msg)"
            size="small"
            effect="dark"
          >
            {{ getSenderLabel(msg) }}
          </el-tag>
          <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
        </div>
        <div class="message-bubble" :class="getMessageClass(msg)">
          <div v-if="msg.imageUrl" class="message-image">
            <el-image
              :src="msg.imageUrl"
              :preview-src-list="[msg.imageUrl]"
              fit="cover"
              style="max-width: 200px; max-height: 150px; border-radius: 6px;"
            />
          </div>
          <div class="message-text">{{ msg.content }}</div>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <div class="admin-badge">
        <el-tag type="success" effect="dark" size="small">
          <el-icon style="margin-right: 2px;"><Avatar /></el-icon>
          管理员调解
        </el-tag>
      </div>
      <div class="input-row">
        <el-input
          v-model="newMessage"
          type="textarea"
          :rows="2"
          placeholder="以管理员身份发送调解消息，买卖双方均可见..."
          @keydown.enter.ctrl="sendMessage"
          resize="none"
          maxlength="500"
          show-word-limit
        />
        <el-button
          type="success"
          :disabled="!newMessage.trim() || sending"
          :loading="sending"
          @click="sendMessage"
          style="margin-left: 12px; height: 100%;"
        >
          发送调解
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Shop, ChatDotRound, Avatar } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps<{
  interventionData: any
  buyerInfo: any
  sellerInfo: any
}>()

const emit = defineEmits<{
  (e: 'message-sent', message: any): void
}>()

const loading = ref(false)
const sending = ref(false)
const messages = ref<any[]>([])
const newMessage = ref('')
const messagesRef = ref<HTMLElement | null>(null)

const sessionId = ref<number | null>(null)

async function loadChatHistory() {
  if (!props.interventionData) return

  loading.value = true
  try {
    let sid = props.interventionData.sessionId || props.interventionData.id

    if (!sid) {
      const userId = props.buyerInfo?.id
      const sellerId = props.sellerInfo?.id
      if (userId && sellerId) {
        try {
          const session = await request.get('/chat/session', {
            params: { userId, sellerId }
          })
          if (session && session.id) {
            sid = session.id
          }
        } catch {}
      }
    }

    if (!sid) {
      messages.value = []
      loading.value = false
      return
    }

    sessionId.value = sid

    const res = await request.get(`/chat/messages/${sid}`, {
      params: { page: 1, size: 100 }
    })
    messages.value = Array.isArray(res) ? res : (res?.records || res?.data || [])
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    console.error('加载聊天记录失败:', e)
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  if (!newMessage.value.trim() || !sessionId.value) return

  const content = newMessage.value.trim()
  newMessage.value = ''
  sending.value = true

  try {
    const res = await request.post('/chat/send', null, {
      params: {
        sessionId: sessionId.value,
        content
      }
    })

    const msg = res || {
      id: Date.now(),
      content,
      senderType: 3,
      senderId: 0,
      createdAt: new Date().toISOString()
    }
    messages.value.push(msg)
    emit('message-sent', msg)
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    ElMessage.error('发送消息失败: ' + (e.message || '未知错误'))
    newMessage.value = content
  } finally {
    sending.value = false
  }
}

function getSenderLabel(msg: any): string {
  switch (msg.senderType) {
    case 1: return props.buyerInfo?.nickname || props.buyerInfo?.username || '买家'
    case 2: return props.sellerInfo?.nickname || props.sellerInfo?.username || props.sellerInfo?.shopName || '商家'
    case 3: return '管理员'
    default: return '未知'
  }
}

function getSenderTagType(msg: any): string {
  switch (msg.senderType) {
    case 1: return 'primary'
    case 2: return 'warning'
    case 3: return 'success'
    default: return 'info'
  }
}

function getMessageClass(msg: any): string {
  switch (msg.senderType) {
    case 1: return 'buyer'
    case 2: return 'seller'
    case 3: return 'admin'
    default: return 'other'
  }
}

function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

watch(() => props.interventionData, (val) => {
  if (val) {
    loadChatHistory()
  }
}, { immediate: true })

onMounted(() => {
  if (props.interventionData) {
    loadChatHistory()
  }
})
</script>

<style scoped>
.mediation-chat-panel {
  display: flex;
  flex-direction: column;
  height: 520px;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.mediation-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 20px;
  background: white;
  border-bottom: 2px solid #e4e7ed;
}

.party-info {
  display: flex;
  align-items: center;
}

.vs-divider {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  font-weight: 500;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.no-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.message-item {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}

.message-item.buyer {
  align-items: flex-start;
}

.message-item.seller {
  align-items: flex-end;
}

.message-item.admin {
  align-items: center;
}

.message-sender {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.message-item.seller .message-sender {
  flex-direction: row-reverse;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  word-break: break-all;
  position: relative;
}

.message-bubble.buyer {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-top-left-radius: 4px;
}

.message-bubble.seller {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-top-right-radius: 4px;
}

.message-bubble.admin {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  max-width: 80%;
  text-align: center;
}

.message-bubble.admin .message-text::before {
  content: '🔧 ';
}

.message-image {
  margin-bottom: 6px;
}

.message-text {
  font-size: 14px;
  color: #303133;
}

.chat-input-area {
  padding: 12px 20px 16px;
  background: white;
  border-top: 2px solid #e4e7ed;
}

.admin-badge {
  margin-bottom: 8px;
}

.input-row {
  display: flex;
  align-items: stretch;
  gap: 0;
}
</style>
