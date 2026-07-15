<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="500px"
    :close-on-click-modal="false"
    class="cs-dialog"
  >
    <div class="cs-container">
      <div class="cs-messages" ref="messageContainer">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['cs-message', msg.type]"
        >
          <div class="cs-avatar" v-if="msg.type === 'merchant' || msg.type === 'admin'">
            <el-icon :size="20">
              <component :is="msg.type === 'admin' ? 'Star' : 'Shop'" />
            </el-icon>
          </div>
          <div class="cs-content">
            <div class="cs-text">{{ msg.content }}</div>
            <div class="cs-time">{{ msg.time }}</div>
          </div>
          <div class="cs-avatar user-avatar" v-if="msg.type === 'user'">
            <el-icon :size="20"><User /></el-icon>
          </div>
        </div>
        <div v-if="isTyping" class="cs-message merchant">
          <div class="cs-avatar">
            <el-icon :size="20"><Shop /></el-icon>
          </div>
          <div class="cs-content cs-typing">
            <span>.</span><span>.</span><span>.</span>
          </div>
        </div>
      </div>
      <div class="cs-input">
        <el-input
          v-model="inputText"
          placeholder="输入消息..."
          @keyup.enter="handleSend"
        >
          <template #append>
            <el-button @click="handleSend" :disabled="!inputText.trim()">
              <el-icon><Promotion /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { Shop, Star, User, Promotion } from '@element-plus/icons-vue'
import api from '@/utils/api'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, default: '在线客服' },
  chatType: { type: String, default: 'merchant' },
  seller: { type: Object, default: null },
  productId: { type: [String, Number], default: null }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

const messages = ref([])
const inputText = ref('')
const isTyping = ref(false)
const messageContainer = ref(null)
let msgId = 0
let afterSaleId = null

watch(() => props.modelValue, (v) => {
  if (v) {
    initMessages()
  }
})

const getCurrentTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

const initMessages = () => {
  msgId = 0
  messages.value = []
  const welcomeMsg = props.chatType === 'merchant'
    ? `您好！我是${props.seller?.name || '商家'}的客服，请问有什么可以帮您？`
    : '您好！我是平台管理员，请问有什么可以帮您？'
  messages.value.push({
    id: ++msgId,
    type: props.chatType,
    content: welcomeMsg,
    time: getCurrentTime()
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

const getAutoReply = (content) => {
  const keywords = {
    '你好': `您好！欢迎光临${props.seller?.name || '本店'}，请问有什么可以帮您？`,
    '价格': '我们的商品都是产地直供，价格实惠，保证新鲜。页面显示的就是当前价格哦。',
    '配送': '我们支持全国配送，一般情况下 2-3 天送达，偏远地区可能需要 3-5 天。',
    '质量': '我们的商品都是新鲜采摘/生产，保证质量。如有问题，支持 7 天无理由退换货。',
    '优惠': '关注我们的店铺可以领取优惠券，还有满减活动哦！',
    '发票': '支持开具电子发票，下单时可以选择发票类型。',
    '售后': '如有任何问题，请及时与我们联系，我们会第一时间为您解决。',
    '发货': '一般情况下，下单后 24 小时内发货，节假日顺延。',
    '退货': '支持 7 天无理由退换货，商品需保持完好，不影响二次销售。',
    '换货': '如需换货，请先申请退货退款，然后重新下单即可。'
  }
  for (const [key, value] of Object.entries(keywords)) {
    if (content.includes(key)) return value
  }
  return '您好，感谢您的咨询。我们的客服会尽快回复您，请耐心等待。您也可以先看看商品详情和评价。'
}

const createAfterSaleService = async () => {
  try {
    const response = await api.createAfterSale({
      orderId: 0,
      productId: props.productId,
      sellerId: props.seller?.id,
      userId: 1,
      issueType: 'consult',
      description: '商品咨询',
      status: 0
    })
    if (response.code === 0) {
      afterSaleId = response.data.id
    }
  } catch (error) {
    afterSaleId = Date.now()
  }
}

const handleSend = async () => {
  const content = inputText.value.trim()
  if (!content) return

  try {
    if (!afterSaleId) {
      await createAfterSaleService()
    }

    messages.value.push({
      id: ++msgId,
      type: 'user',
      content,
      time: getCurrentTime()
    })
    inputText.value = ''
    scrollToBottom()

    await api.sendChatMessage({
      afterSaleId,
      senderId: 1,
      senderType: 1,
      content,
      messageType: 1
    })

    isTyping.value = true
    setTimeout(() => {
      isTyping.value = false
      messages.value.push({
        id: ++msgId,
        type: 'merchant',
        content: getAutoReply(content),
        time: getCurrentTime()
      })
      scrollToBottom()
    }, 1000 + Math.random() * 1000)
  } catch (error) {
    console.error('发送消息失败:', error)
  }
}
</script>

<style scoped>
.cs-dialog .cs-container {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.cs-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f9f5;
  border-radius: 8px;
  margin-bottom: 15px;
}

.cs-message {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
}

.cs-message.user {
  flex-direction: row-reverse;
}

.cs-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 10px;
  flex-shrink: 0;
}

.cs-avatar.user-avatar {
  background: #2196f3;
}

.cs-content {
  max-width: 70%;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.cs-text {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 5px;
}

.cs-time {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.cs-message.user .cs-content {
  background: var(--accent);
}

.cs-message.user .cs-text {
  color: white;
}

.cs-typing {
  display: flex;
  gap: 3px;
  padding: 15px;
}

.cs-typing span {
  width: 8px;
  height: 8px;
  background: var(--accent);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.cs-typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.cs-typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.cs-input {
  margin-top: 10px;
}

@media (max-width: 768px) {
  .cs-dialog {
    width: 90% !important;
  }
}
</style>
