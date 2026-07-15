<template>
  <div class="ai-page">
    <div class="ai-container">
      <div class="ai-sidebar">
        <div class="sidebar-header">
          <div class="sidebar-icon">🤖</div>
          <h3 class="sidebar-title">AI 助手</h3>
        </div>

        <div class="service-types">
          <button
            v-for="svc in serviceTypes"
            :key="svc.type"
            :class="['service-btn', { active: activeType === svc.type }]"
            :style="activeType === svc.type ? { '--accent': svc.color, '--accent-light': svc.colorLight, '--accent-bg': svc.colorBg } : {}"
            @click="switchServiceType(svc.type)"
          >
            <span class="service-icon">{{ svc.icon }}</span>
            <div class="service-info">
              <span class="service-name">{{ svc.name }}</span>
              <span class="service-scene">{{ svc.scene }}</span>
            </div>
            <span v-if="activeType === svc.type" class="active-dot"></span>
          </button>
        </div>

        <div class="sidebar-actions">
          <button class="clear-btn" @click="clearMessages" :disabled="currentMessages.length === 0">
            清空当前对话
          </button>
          <button class="clear-all-btn" @click="clearAllMessages" :disabled="!hasAnyMessages()">
            清空全部对话
          </button>
          <p class="sidebar-tip">数据仅保存在本地</p>
        </div>
      </div>

      <div class="ai-main">
        <div class="chat-header" :style="{ '--accent': currentService.color, '--accent-light': currentService.colorLight }">
          <span class="header-icon">{{ currentService.icon }}</span>
          <div class="header-info">
            <span class="header-name">{{ currentService.name }}</span>
            <span class="header-desc">{{ currentService.scene }}</span>
          </div>
          <span class="header-badge" :style="{ background: currentService.colorLight, color: currentService.color }">
            {{ currentService.name }}模式
          </span>
        </div>

        <div class="chat-body">
          <div class="chat-content" ref="chatContent">
            <div v-if="currentMessages.length === 0" class="welcome-area">
              <div class="welcome-card">
                <div class="welcome-icon">
                  <span class="welcome-emoji">{{ currentService.icon }}</span>
                </div>
                <h2 class="welcome-title">{{ currentService.greeting }}</h2>
                <p class="welcome-desc">{{ currentService.desc }}</p>
                <div class="quick-suggestions">
                  <button
                    v-for="(q, i) in currentService.quickQuestions"
                    :key="i"
                    class="quick-chip"
                    @click="sendSuggestion(q)"
                  >
                    {{ q }}
                  </button>
                </div>
              </div>
            </div>

            <div
              v-for="msg in currentMessages"
              :key="msg.id"
              :class="['message-row', msg.type === 'user' ? 'msg-user' : msg.type === 'system' ? 'msg-system' : 'msg-ai']"
            >
              <div v-if="msg.type === 'system'" class="system-bubble">
                <span>{{ msg.content }}</span>
              </div>

              <div v-else-if="msg.type === 'user'" class="user-bubble" :style="{ '--accent': msg.serviceColor || currentService.color }">
                <p>{{ msg.content }}</p>
              </div>

              <div v-else class="ai-bubble">
                <div class="ai-avatar">{{ msg.serviceIcon || '🤖' }}</div>
                <div class="ai-body" :style="{ '--accent': msg.serviceColor || currentService.color }">
                  <div class="ai-content">
                    <template v-if="msg.streaming">{{ msg.content }}<span class="cursor-blink" :style="{ background: msg.serviceColor || currentService.color }"></span></template>
                    <template v-else>{{ msg.content }}</template>
                  </div>

                  <div v-if="msg.productCards && msg.productCards.length > 0" class="product-list">
                    <div
                      v-for="product in msg.productCards"
                      :key="product.id"
                      class="mini-product"
                      @click="goProduct(product.id)"
                    >
                      <img :src="product.image" :alt="product.name" class="mini-product-img">
                      <div class="mini-product-body">
                        <div class="mini-product-header">
                          <p class="mini-product-name">{{ product.name }}</p>
                          <span v-if="product.sales > 0" class="mini-product-sales">已售{{ product.sales }}+</span>
                        </div>
                        <p class="mini-product-category" v-if="product.category">{{ product.category }}</p>
                        <p class="mini-product-reason" v-if="product.reason">{{ product.reason }}</p>
                        <p class="mini-product-price">{{ product.price ? '¥' + product.price : '' }}</p>
                      </div>
                    </div>
                  </div>

                  <div v-if="msg.activityCards && msg.activityCards.length > 0" class="activity-list">
                    <div
                      v-for="activity in msg.activityCards"
                      :key="'act-' + activity.id"
                      class="activity-card"
                      @click="goDiscount(activity.id)"
                    >
                      <div class="activity-badge" :class="'badge-type-' + activity.type">
                        {{ activity.typeName }}
                      </div>
                      <div class="activity-body">
                        <p class="activity-name">{{ activity.name }}</p>
                        <div class="activity-rule" v-if="activity.threshold && activity.reduceAmount">
                          满{{ formatAmount(activity.threshold) }}减{{ formatAmount(activity.reduceAmount) }}
                        </div>
                        <div class="activity-rule" v-else-if="activity.discountRate">
                          {{ formatRate(activity.discountRate) }}折优惠
                        </div>
                        <p class="activity-desc" v-if="activity.description">{{ activity.description }}</p>
                      </div>
                      <span class="activity-arrow">去看看 &gt;</span>
                    </div>
                  </div>

                  <div v-if="!msg.streaming && msg.followUpQuestions && msg.followUpQuestions.length > 0" class="follow-up-bar">
                    <span class="follow-up-label">你可能还想问：</span>
                    <button
                      v-for="(fq, fi) in msg.followUpQuestions"
                      :key="fi"
                      class="follow-up-chip"
                      @click="sendSuggestion(fq)"
                    >
                      {{ fq }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="isTyping" class="message-row msg-ai">
              <div class="ai-bubble">
                <div class="ai-avatar">{{ currentService.icon }}</div>
                <div class="ai-body">
                  <div class="ai-content">正在思考中<span class="cursor-blink" :style="{ background: currentService.color }"></span></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-bar">
            <div class="input-wrapper">
              <input
                v-model="inputContent"
                class="chat-input"
                :placeholder="currentService.placeholder"
                @keydown.enter="sendMessage"
                :disabled="isTyping"
              />
              <button
                class="send-btn"
                :disabled="!inputContent.trim() || isTyping"
                :style="{ '--accent': currentService.color }"
                @click="sendMessage"
              >
                发送
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()

const activeType = ref(1)
const inputContent = ref('')
const isTyping = ref(false)
const chatContent = ref(null)
const messageId = ref(0)

const serviceTypes = [
  {
    type: 1,
    name: '商品查询',
    icon: '🛒',
    scene: '找商品、比价格、看推荐',
    color: '#5a8f3d',
    colorLight: '#e8f5e0',
    colorBg: 'rgba(90, 143, 61, 0.06)',
    greeting: '您好！我是商品查询助手',
    desc: '帮您找到心仪的农产品，推荐好物、查询价格、对比库存、了解产地特色。',
    placeholder: '描述您想要的商品，例如：推荐一些有机蔬菜...',
    quickQuestions: [
      '推荐一些有机蔬菜',
      '有没有当季新鲜水果',
      '特产礼品有哪些推荐',
      '什么商品性价比最高',
      '有优惠活动吗'
    ],
    followUpQuestions: [
      '这些商品产地在哪里',
      '有没有更多同类商品',
      '能介绍一下营养价值吗',
      '帮我比一下价格'
    ]
  },
  {
    type: 2,
    name: '物流查询',
    icon: '📦',
    scene: '查物流、跟进度、问时效',
    color: '#3b82c4',
    colorLight: '#e0ecf8',
    colorBg: 'rgba(59, 130, 196, 0.06)',
    greeting: '您好！我是物流查询助手',
    desc: '帮您追踪订单状态、查询物流进度、了解配送时间和快递公司信息。',
    placeholder: '请输入订单号或描述物流问题...',
    quickQuestions: [
      '我的订单什么时候发货',
      '快递到哪了怎么查',
      '一般几天能收到货',
      '支持哪些快递公司',
      '可以修改收货地址吗'
    ],
    followUpQuestions: [
      '发货后能改地址吗',
      '周末送货吗',
      '怎么联系快递员',
      '货到付款怎么操作'
    ]
  },
  {
    type: 3,
    name: '售后咨询',
    icon: '💬',
    scene: '退款、换货、投诉维权',
    color: '#d97706',
    colorLight: '#fef3e0',
    colorBg: 'rgba(217, 119, 6, 0.06)',
    greeting: '您好！我是售后咨询助手',
    desc: '帮您处理退款退货、换货流程、售后维权、投诉反馈等问题。',
    placeholder: '请描述您遇到的售后问题...',
    quickQuestions: [
      '如何申请退款',
      '换货流程是什么',
      '商品坏了怎么办',
      '退款多久到账',
      '怎样联系人工客服'
    ],
    followUpQuestions: [
      '退货需要自己出运费吗',
      '超过7天还能退吗',
      '退款会退到哪里',
      '怎么投诉商家'
    ]
  }
]

const currentService = computed(() => {
  return serviceTypes.find(s => s.type === activeType.value) || serviceTypes[0]
})

const allTypeMessages = ref({ 1: [], 2: [], 3: [] })

const currentMessages = computed(() => {
  return allTypeMessages.value[activeType.value] || []
})

onMounted(() => {
  let globalId = 0
  let hasAny = false
  serviceTypes.forEach(svc => {
    try {
      const key = `ai_history_type_${svc.type}`
      const saved = localStorage.getItem(key)
      if (saved) {
        const parsed = JSON.parse(saved)
        const filtered = parsed.filter(m => !m.streaming)
        allTypeMessages.value[svc.type] = filtered.slice(-50)
        if (filtered.length > 0) hasAny = true
        filtered.forEach(m => {
          const parts = m.id ? m.id.split('_') : []
          if (parts.length >= 2) {
            const num = parseInt(parts[1]) || 0
            if (num > globalId) globalId = num
          }
        })
      }
          } catch (e) {}
  })
  messageId.value = globalId
  scrollToBottom()
})

const getImageUrl = (image) => {
  if (!image) return '/images/vegetable.jpg'
  if (image.startsWith('http')) return image
  const baseUrl = api.defaults.baseURL.replace('/api', '')
  return `${baseUrl}${image.startsWith('/') ? '' : '/'}${image}`
}

const switchServiceType = (type) => {
  if (activeType.value === type) return
  activeType.value = type
  inputContent.value = ''
  scrollToBottom()
}

const clearMessages = () => {
  allTypeMessages.value[activeType.value] = []
  localStorage.removeItem(`ai_history_type_${activeType.value}`)
}

const clearAllMessages = () => {
  serviceTypes.forEach(svc => {
    allTypeMessages.value[svc.type] = []
    localStorage.removeItem(`ai_history_type_${svc.type}`)
  })
  messageId.value = 0
}

const hasAnyMessages = () => {
  return serviceTypes.some(svc => (allTypeMessages.value[svc.type] || []).length > 0)
}

const addMessage = (msg) => {
  allTypeMessages.value[activeType.value] = [...allTypeMessages.value[activeType.value], msg]
  saveTypeMessages(activeType.value)
}

const updateMessage = (msgId, updates) => {
  const msgs = allTypeMessages.value[activeType.value]
  const idx = msgs.findIndex(m => m.id === msgId)
  if (idx !== -1) {
    msgs[idx] = { ...msgs[idx], ...updates }
    allTypeMessages.value[activeType.value] = [...msgs]
    saveTypeMessages(activeType.value)
  }
}

const sendMessage = async () => {
  const content = inputContent.value.trim()
  if (!content || isTyping.value) return

  const svc = currentService.value
  messageId.value++

  const userMessage = {
    id: `msg_${messageId.value}`,
    type: 'user',
    content: content,
    timestamp: Date.now(),
    serviceType: activeType.value,
    serviceColor: svc.color
  }

  addMessage(userMessage)
  inputContent.value = ''
  scrollToBottom()

  isTyping.value = true
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const token = localStorage.getItem('token') || ''

  console.log('[AI Debug] 发送消息:', content, '| token长度:', token.length, '| userId:', userInfo.id)

  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({
        query: content,
        serviceType: activeType.value,
        userId: userInfo.id || 0
      })
    })

    console.log('[AI Debug] 响应状态:', response.status, '| ok:', response.ok, '| body:', !!response.body)

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    console.log('[AI Debug] 开始读取SSE流...')

    messageId.value++
    const aiMsgId = `msg_${messageId.value}`

    const aiMessage = {
      id: aiMsgId,
      type: 'ai',
      content: '',
      timestamp: Date.now(),
      serviceType: activeType.value,
      serviceIcon: svc.icon,
      serviceColor: svc.color,
      streaming: true,
      productCards: [],
      followUpQuestions: svc.followUpQuestions || []
    }
    addMessage(aiMessage)
    scrollToBottom()

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        console.log('[AI Debug] 读取器done=true, 退出循环')
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          try {
            const data = JSON.parse(line.slice(5))
            if (data.token) {
              aiMessage.content += data.token
              updateMessage(aiMsgId, { content: aiMessage.content })
              scrollToBottom()
            }
            if (data.response) {
              console.log('[AI Debug] 收到done响应, 长度:', data.response.length)
              aiMessage.content = data.response
              updateMessage(aiMsgId, { content: aiMessage.content, streaming: false })
              scrollToBottom()
            }
            if (data.productCards) {
              aiMessage.productCards = data.productCards.map(p => ({
                ...p,
                image: getImageUrl(p.image)
              }))
              updateMessage(aiMsgId, { productCards: aiMessage.productCards })
            }
            if (data.activityCards) {
              aiMessage.activityCards = data.activityCards
              updateMessage(aiMsgId, { activityCards: aiMessage.activityCards })
            }
          } catch (e) {
          }
        } else if (line.startsWith('event:done')) {
          updateMessage(aiMsgId, { streaming: false })
          scrollToBottom()
        } else if (line.startsWith('event:error')) {
          updateMessage(aiMsgId, { content: aiMessage.content || '抱歉，AI服务暂时不可用', streaming: false })
          scrollToBottom()
        }
      }
    }

    if (aiMessage.streaming) {
      updateMessage(aiMsgId, { streaming: false })
    }

    console.log('[AI Debug] SSE流结束, content长度:', aiMessage.content.length, '| content前50字:', aiMessage.content.substring(0, 50))

    if (!aiMessage.content) {
      console.warn('[AI Debug] content为空，显示本地智能回复')
      updateMessage(aiMsgId, { content: '您好，目前系统采用本地智能回复模式。有什么可以帮助您的？', streaming: false })
    }

  } catch (error) {
    console.error('[AI Debug] AI流式查询失败:', error.message, error)
    if (aiMessage.content) {
      updateMessage(aiMsgId, { streaming: false })
    } else {
      messageId.value++
      const errMsg = {
        id: `msg_${messageId.value}`,
        type: 'ai',
        content: '网络连接异常，请检查网络后重试',
        timestamp: Date.now(),
        serviceType: activeType.value,
        serviceIcon: svc.icon,
        serviceColor: svc.color,
        streaming: false
      }
      addMessage(errMsg)
    }
    scrollToBottom()
  } finally {
    isTyping.value = false
  }
}

const sendSuggestion = (content) => {
  inputContent.value = content
  sendMessage()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContent.value) {
      chatContent.value.scrollTop = chatContent.value.scrollHeight
    }
  })
}

const saveTypeMessages = (type) => {
  try {
    const msgs = (allTypeMessages.value[type] || []).filter(m => !m.streaming)
    localStorage.setItem(`ai_history_type_${type}`, JSON.stringify(msgs.slice(-50)))
  } catch (e) {}
}

const goProduct = (id) => {
  router.push(`/product/${id}`)
}

const goDiscount = (id) => {
  router.push(`/discount/${id}`)
}

const formatAmount = (val) => {
  if (val === null || val === undefined) return ''
  return String(val).replace(/\.0+$/, '')
}

const formatRate = (val) => {
  if (val === null || val === undefined) return ''
  return String(val).replace(/\.0+$/, '')
}
</script>

<style scoped>
.ai-page {
  height: calc(100vh - 80px);
  padding: 20px;
  box-sizing: border-box;
}

.ai-container {
  display: flex;
  height: 100%;
  max-width: 1100px;
  margin: 0 auto;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.08);
  background: #faf8f5;
}

.ai-sidebar {
  width: 210px;
  background: #fff;
  border-right: 1px solid #ede8df;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ede8df;
}

.sidebar-icon { font-size: 28px; }

.sidebar-title {
  font-size: 18px;
  font-weight: 700;
  color: #3d2b1a;
  margin: 0;
}

.service-types {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.service-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: transparent;
  border: 1.5px solid transparent;
  cursor: pointer;
  transition: all 0.25s;
  text-align: left;
  position: relative;
}

.service-btn:hover { background: #f5f0e8; }
.service-btn.active {
  background: var(--accent-bg);
  border-color: var(--accent);
}

.service-icon { font-size: 24px; flex-shrink: 0; }

.service-info { display: flex; flex-direction: column; gap: 2px; }

.service-name { font-size: 14px; font-weight: 600; color: #3d2b1a; }
.service-scene { font-size: 11px; color: #a89880; }

.active-dot {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
}

.sidebar-actions {
  padding-top: 20px;
  border-top: 1px solid #ede8df;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.clear-btn, .clear-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border-radius: 10px;
  background: transparent;
  border: 1px solid #e0d8cc;
  color: #a89880;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover:not(:disabled) { background: #fef2f2; border-color: #fecaca; color: #ef4444; }
.clear-all-btn:hover:not(:disabled) { background: #fef2f2; border-color: #fecaca; color: #dc2626; }
.clear-btn:disabled, .clear-all-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.sidebar-tip { text-align: center; font-size: 11px; color: #c4b99e; margin: 4px 0 0 0; }

.ai-main { flex: 1; display: flex; flex-direction: column; min-width: 0; background: #faf8f5; }

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #ede8df;
  flex-shrink: 0;
}

.header-icon { font-size: 24px; }

.header-info { display: flex; flex-direction: column; gap: 1px; }
.header-name { font-size: 15px; font-weight: 600; color: #3d2b1a; }
.header-desc { font-size: 12px; color: var(--accent); }

.header-badge {
  margin-left: auto;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.chat-body { flex: 1; overflow: hidden; display: flex; }

.chat-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.chat-content::-webkit-scrollbar { width: 5px; }
.chat-content::-webkit-scrollbar-track { background: transparent; }
.chat-content::-webkit-scrollbar-thumb { background: #ddd6cc; border-radius: 10px; }

.welcome-area {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
}

.welcome-card {
  text-align: center;
  background: #fff;
  border-radius: 20px;
  padding: 48px 40px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
  border: 1px solid #ede8df;
  max-width: 520px;
  width: 100%;
}

.welcome-emoji { font-size: 56px; }

.welcome-title { font-size: 22px; font-weight: 700; color: #3d2b1a; margin: 0 0 8px; }
.welcome-desc { font-size: 14px; color: #8c7b6b; line-height: 1.7; margin: 0 0 28px; }

.quick-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.quick-chip {
  padding: 8px 16px;
  border-radius: 20px;
  background: #f5f0e8;
  border: 1px solid #e8dfcf;
  color: #6b5740;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-chip:hover {
  background: var(--accent-light);
  border-color: var(--accent);
  color: var(--accent);
}

.message-row { display: flex; margin-bottom: 20px; }
.msg-user { justify-content: flex-end; }
.msg-system { justify-content: center; }

.system-bubble {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 20px;
  background: #f5f0e8;
  color: #a89880;
  font-size: 12px;
}

.user-bubble {
  max-width: 70%;
  background: linear-gradient(135deg, var(--accent, #8B5E3C), color-mix(in srgb, var(--accent, #8B5E3C) 70%, #000));
  color: #fff;
  padding: 12px 18px;
  border-radius: 16px 16px 4px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.user-bubble p { margin: 0; font-size: 14px; line-height: 1.6; }

.ai-bubble { display: flex; gap: 12px; max-width: 80%; align-items: flex-start; }
.ai-avatar { font-size: 28px; flex-shrink: 0; line-height: 1; }

.ai-body {
  background: #fff;
  padding: 14px 18px;
  border-radius: 4px 16px 16px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ede8df;
  border-left: 3px solid var(--accent, #8B5E3C);
}

.ai-content { font-size: 14px; color: #3d2b1a; line-height: 1.7; white-space: pre-wrap; }

.cursor-blink {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--accent, #8B5E3C);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.product-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }

.mini-product {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  background: #faf8f5;
  border: 1px solid #ede8df;
  cursor: pointer;
  transition: all 0.2s;
  align-items: flex-start;
}

.mini-product:hover {
  border-color: var(--accent);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.mini-product-img { width: 60px; height: 60px; border-radius: 8px; object-fit: cover; flex-shrink: 0; margin-top: 2px; }

.mini-product-body { flex: 1; display: flex; flex-direction: column; justify-content: center; min-width: 0; gap: 3px; }

.mini-product-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.mini-product-name { font-size: 13px; font-weight: 600; color: #3d2b1a; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }
.mini-product-sales { font-size: 11px; color: #e6733a; background: #fff3ed; padding: 1px 6px; border-radius: 4px; white-space: nowrap; flex-shrink: 0; }

.mini-product-category { font-size: 11px; color: #8B7355; margin: 0; }
.mini-product-reason { font-size: 11px; color: #777; margin: 0; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.mini-product-price { font-size: 15px; font-weight: 700; color: #8B5E3C; margin: 2px 0 0; }

.activity-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }

.activity-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fff5f0, #fff0e6);
  border: 1px solid #fcd5c4;
  cursor: pointer;
  transition: all 0.2s;
}

.activity-card:hover {
  border-color: #e6733a;
  box-shadow: 0 2px 10px rgba(230, 115, 58, 0.15);
  transform: translateY(-1px);
}

.activity-badge {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}

.badge-type-1 { background: #e6733a; }
.badge-type-2 { background: #d4380d; }
.badge-type-3 { background: #cf1322; }

.activity-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.activity-name { font-size: 13px; font-weight: 600; color: #3d2b1a; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.activity-rule { font-size: 12px; font-weight: 700; color: #d4380d; margin: 0; }
.activity-desc { font-size: 11px; color: #8c6b5a; margin: 2px 0 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.activity-arrow { font-size: 12px; color: #e6733a; white-space: nowrap; flex-shrink: 0; font-weight: 600; }

.follow-up-bar {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #ede8df;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.follow-up-label { font-size: 12px; color: #a89880; }
.follow-up-chip {
  padding: 5px 12px;
  border-radius: 14px;
  background: var(--accent-light);
  border: 1px solid transparent;
  color: var(--accent);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.follow-up-chip:hover { border-color: var(--accent); background: #fff; }

.input-area { background: #fff; border-top: 1px solid #ede8df; flex-shrink: 0; }

.input-bar { padding: 12px 24px 16px; }
.input-wrapper { display: flex; gap: 10px; align-items: center; }

.chat-input {
  flex: 1;
  padding: 12px 18px;
  border-radius: 24px;
  border: 1.5px solid #e0d8cc;
  font-size: 14px;
  color: #3d2b1a;
  outline: none;
  transition: all 0.2s;
  background: #faf8f5;
}

.chat-input:focus { border-color: var(--accent); background: #fff; }
.chat-input::placeholder { color: #b8af9e; }
.chat-input:disabled { background: #f5f0e8; cursor: not-allowed; }

.send-btn {
  padding: 10px 20px;
  border-radius: 24px;
  background: var(--accent, #8B5E3C);
  border: none;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) { filter: brightness(1.1); transform: scale(1.02); }
.send-btn:disabled { background: #e0d8cc; cursor: not-allowed; }

@media (max-width: 768px) {
  .ai-page { padding: 0; }
  .ai-container { border-radius: 0; flex-direction: column; }
  .ai-sidebar { width: 100%; flex-direction: row; padding: 12px; border-right: none; border-bottom: 1px solid #ede8df; flex-wrap: wrap; gap: 8px; }
  .sidebar-header { margin: 0; padding: 0; border: none; }
  .sidebar-title { display: none; }
  .service-types { flex-direction: row; flex: none; gap: 4px; }
  .service-btn { padding: 8px 10px; gap: 6px; }
  .service-scene { display: none; }
  .active-dot { display: none; }
  .sidebar-actions { border: none; padding: 0; flex-direction: row; }
  .welcome-card { padding: 32px 20px; }
  .user-bubble { max-width: 85%; }
  .ai-bubble { max-width: 90%; }
}
</style>