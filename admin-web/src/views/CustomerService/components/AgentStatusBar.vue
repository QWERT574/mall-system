<template>
  <div class="agent-status-bar">
    <div class="status-left">
      <el-avatar :size="36" icon="UserFilled" />
      <div class="status-info">
        <span class="agent-name">{{ session?.agentName || session?.sellerName || '客服' }}</span>
        <span class="connection-status" :class="{ connected: connected }">
          <span class="dot"></span>
          {{ connected ? '已连接' : '连接中...' }}
        </span>
      </div>
    </div>
    <div class="status-right">
      <el-button v-if="session?.agentId" text @click="$emit('transfer')">
        <el-icon><Switch /></el-icon> 转接
      </el-button>
      <el-button text type="danger" @click="$emit('close')">
        关闭会话
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { SwitchButton } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const connected = computed(() => chatStore.connected)

defineProps<{
  session: any
  agents: any[]
  isTyping: boolean
  typingUserId: number | null
}>()

defineEmits<{
  transfer: []
  close: []
}>()
</script>

<style scoped lang="scss">
.agent-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .status-left {
    display: flex;
    align-items: center;
    gap: 10px;

    .status-info {
      display: flex;
      flex-direction: column;
      .agent-name { font-size: 14px; font-weight: 500; }
      .connection-status {
        font-size: 11px;
        color: var(--color-text-tertiary);
        display: flex;
        align-items: center;
        gap: 4px;
        .dot {
          width: 6px; height: 6px;
          border-radius: 50%;
          background: #e6a23c;
        }
        &.connected .dot { background: #67c23a; }
      }
    }
  }

  .status-right {
    display: flex;
    gap: 8px;
  }
}
</style>
