<template>
  <el-dialog
    :model-value="visible"
    title="转接会话"
    width="450px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form label-width="80px">
      <el-form-item label="当前客服">
        <el-tag>{{ session?.agentName || session?.sellerName || '未分配' }}</el-tag>
      </el-form-item>
      <el-form-item label="转接至">
        <el-select v-model="selectedAgentId" placeholder="选择目标客服" style="width:100%">
          <el-option
            v-for="agent in filteredAgents"
            :key="agent.id"
            :label="`${agent.agentName} (${agent.skillGroup}) - 服务中:${agent.currentSessions}`"
            :value="agent.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="转接原因">
        <el-input v-model="reason" placeholder="可选：说明转接原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :disabled="!selectedAgentId" @click="handleConfirm">
        确认转接
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
  session: any
  agents: any[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [params: { sessionId: number; fromAgentId: number; toAgentId: number; reason?: string }]
}>()

const selectedAgentId = ref<number | null>(null)
const reason = ref('')

const filteredAgents = computed(() => {
  if (!props.session) return props.agents || []
  return (props.agents || []).filter(a =>
    a.id !== props.session.agentId && a.status === 1)
})

function handleConfirm() {
  if (!selectedAgentId.value || !props.session) {
    ElMessage.warning('请选择目标客服')
    return
  }
  emit('confirm', {
    sessionId: props.session.id,
    fromAgentId: props.session.agentId || props.session.sellerId || 0,
    toAgentId: selectedAgentId.value,
    reason: reason.value
  })
}
</script>
