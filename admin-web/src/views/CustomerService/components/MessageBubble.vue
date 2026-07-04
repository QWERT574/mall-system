<template>
  <div class="message-bubble" :class="{ mine: isMine, auto: message.isAutoReply }">
    <el-avatar v-if="!isMine" :size="32" icon="UserFilled" class="avatar" />
    <div class="bubble-content">
      <div v-if="message.isAutoReply" class="auto-tag">
        <el-icon><MagicStick /></el-icon> AI助手
      </div>

      <div v-if="message.messageType === 2 && message.imageUrl" class="image-msg">
        <el-image
          :src="message.imageUrl"
          :preview-src-list="[message.imageUrl]"
          fit="cover"
          style="max-width:240px; max-height:320px; border-radius:8px;"
        />
      </div>

      <div v-else-if="message.messageType === 3" class="file-msg">
        <div class="file-card" @click="handleDownload">
          <el-icon :size="24"><Document /></el-icon>
          <div class="file-info">
            <span class="file-name">{{ message.fileName || '文件' }}</span>
            <span class="file-size">{{ formatSize(message.fileSize) }}</span>
          </div>
          <el-icon><Download /></el-icon>
        </div>
      </div>

      <div v-else class="text-msg">
        {{ message.content }}
      </div>

      <div class="bubble-meta">
        <span class="time">{{ formatTime(message.createdAt) }}</span>
        <span v-if="isMine" class="status">
          <span v-if="message.status === 'sending' || message.status === 'sent'">
            <el-icon :size="14"><CircleCheckFilled /></el-icon>
          </span>
          <span v-if="message.isRead" class="read-status">已读</span>
        </span>
      </div>
    </div>
    <el-avatar v-if="isMine" :size="32" icon="UserFilled" class="avatar" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { MagicStick, Document, Download, CircleCheckFilled } from '@element-plus/icons-vue'

const props = defineProps<{
  message: any
  isMine: boolean
}>()

function formatTime(time: string | null | undefined) {
  if (!time) return ''
  const d = new Date(time)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatSize(bytes: number | null | undefined) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}

function handleDownload() {
  if (props.message.imageUrl) {
    window.open(props.message.imageUrl, '_blank')
  }
}
</script>

<style scoped lang="scss">
.message-bubble {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;

  &.mine {
    flex-direction: row-reverse;
  }

  .avatar { flex-shrink: 0; }

  .bubble-content {
    max-width: 70%;
    position: relative;

    .auto-tag {
      font-size: 11px;
      color: #67c23a;
      margin-bottom: 4px;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .text-msg {
      background: #fff;
      border-radius: 8px;
      padding: 10px 14px;
      font-size: 14px;
      line-height: 1.5;
      word-break: break-word;
      box-shadow: 0 1px 2px rgba(0,0,0,0.04);
    }

    .bubble-meta {
      display: flex;
      justify-content: flex-end;
      gap: 6px;
      margin-top: 4px;
      font-size: 11px;
      color: var(--color-text-tertiary);

      .read-status { color: #67c23a; }
    }
  }

  &.mine .text-msg {
    background: #409eff;
    color: #fff;
  }

  &.auto .text-msg {
    background: #f0f9eb;
    border: 1px solid #e1f3d8;
  }

  .file-card {
    display: flex;
    align-items: center;
    gap: 12px;
    background: #fff;
    border-radius: 8px;
    padding: 12px 16px;
    cursor: pointer;
    box-shadow: 0 1px 2px rgba(0,0,0,0.04);
    min-width: 220px;

    .file-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      .file-name { font-size: 13px; color: var(--color-text-primary); }
      .file-size { font-size: 11px; color: var(--color-text-tertiary); }
    }

    &:hover { background: var(--color-bg-page); }
  }

  .image-msg {
    cursor: pointer;
    img:hover { opacity: 0.9; }
  }
}
</style>
