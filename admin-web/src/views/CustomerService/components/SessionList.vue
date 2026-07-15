<template>
  <div class="session-list" v-if="sessions.length">
    <div
      v-for="session in sessions"
      :key="session.id"
      class="session-item"
      :class="{ active: session.id === currentSessionId }"
      @click="$emit('select', session)"
    >
      <div class="session-avatar">
        <el-avatar :size="40" icon="UserFilled" />
        <span v-if="session.isOnline" class="online-dot"></span>
      </div>
      <div class="session-info">
        <div class="session-top">
          <span class="session-name">{{ session.userName || '用户' + session.userId }}</span>
          <span class="session-time">{{ formatTime(session.lastMessageAt) }}</span>
        </div>
        <div class="session-bottom">
          <span class="session-msg">{{ session.lastMessage || '暂无消息' }}</span>
          <el-badge
            v-if="(session.userUnread || 0) > 0"
            :value="session.userUnread"
            class="unread-badge"
          />
        </div>
        <div v-if="session.productName" class="session-product">
          <el-tag size="small">{{ session.productName }}</el-tag>
        </div>
      </div>
      <el-dropdown trigger="click" class="session-actions" @command="(cmd: string) => handleAction(cmd, session)">
        <el-button text :icon="MoreFilled" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="close">关闭会话</el-dropdown-item>
            <el-dropdown-item command="detail">查看详情</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
  <div v-else class="empty-list">
    <el-empty description="暂无会话" :image-size="80" />
  </div>
</template>

<script setup lang="ts">
import { MoreFilled, ChatDotRound } from '@element-plus/icons-vue'
import type { ChatSession } from '@/stores/chat'

defineProps<{
  sessions: ChatSession[]
  currentSessionId: number | null
}>()

const emit = defineEmits<{
  select: [session: ChatSession]
  close: [sessionId: number]
  detail: [sessionId: number]
}>()

function handleAction(cmd: string, session: ChatSession) {
  if (cmd === 'close') emit('close', session.id)
  if (cmd === 'detail') emit('detail', session.id)
}

function formatTime(time: string | null | undefined) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (d.toDateString() === now.toDateString())
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped lang="scss">
.session-list { overflow-y: auto; flex: 1; }

.session-item {
  display: flex;
  align-items: flex-start;
  padding: 14px 16px;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s;
  position: relative;

  &:hover { background: var(--color-bg-page); }
  &.active { background: #ecf5ff; }

  .session-avatar {
    position: relative;
    flex-shrink: 0;
    .online-dot {
      position: absolute;
      bottom: 2px; right: 2px;
      width: 8px; height: 8px;
      border-radius: 50%;
      background: #67c23a;
      border: 2px solid #fff;
    }
  }

  .session-info {
    flex: 1;
    min-width: 0;
    .session-top {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;
      .session-name { font-size: 14px; font-weight: 500; }
      .session-time { font-size: 11px; color: var(--color-text-tertiary); flex-shrink: 0; }
    }
    .session-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;
      .session-msg {
        font-size: 12px;
        color: var(--color-text-tertiary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 180px;
      }
    }
    .session-product { margin-top: 4px; }
  }

  .session-actions {
    opacity: 0;
    transition: opacity 0.2s;
  }
  &:hover .session-actions { opacity: 1; }
}

.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #c0c4cc;
  p { margin-top: 8px; font-size: 13px; }
}
</style>
