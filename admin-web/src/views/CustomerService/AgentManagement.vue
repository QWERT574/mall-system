<template>
  <div class="agent-management">
    <div class="page-header">
      <h2>客服管理</h2>
      <el-button type="primary" @click="showAddDialog = true">添加客服</el-button>
    </div>

    <el-table :data="agents" stripe style="width:100%">
      <el-table-column prop="agentName" label="客服名称" min-width="120" />
      <el-table-column prop="agentCode" label="工号" width="100" />
      <el-table-column prop="skillGroup" label="技能组" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ groupLabel(row.skillGroup) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentSessions" label="当前会话" width="90" align="center" />
      <el-table-column prop="totalServed" label="累计服务" width="90" align="center" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="toggleStatus(row)">
            {{ row.status === 1 ? '下线' : '上线' }}
          </el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAgents, updateAgentStatus } from '@/api/customerService'

const agents = ref<any[]>([])
const showAddDialog = ref(false)

onMounted(async () => {
  await loadAgents()
})

async function loadAgents() {
  try {
    const res = await listAgents()
    agents.value = res || []
  } catch (e) {
    console.error('[AgentMgmt] Failed to load agents:', e)
  }
}

async function toggleStatus(agent: any) {
  const newStatus = agent.status === 1 ? 0 : 1
  try {
    await updateAgentStatus(agent.id, newStatus)
    agent.status = newStatus
    ElMessage.success(newStatus === 1 ? '已上线' : '已下线')
  } catch (e) {
    ElMessage.error('状态更新失败')
  }
}

function handleDelete(agent: any) {
  ElMessage.info('请联系管理员执行删除操作')
}

function statusLabel(status: number) {
  const map: Record<number, string> = { 0: '离线', 1: '在线', 2: '忙碌', 3: '离开' }
  return map[status] || '未知'
}

function statusType(status: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning', 3: '' }
  return map[status] || 'info'
}

function groupLabel(group: string) {
  const map: Record<string, string> = {
    pre_sale: '售前', after_sale: '售后', tech_support: '技术支持'
  }
  return map[group] || group || '-'
}
</script>

<style scoped lang="scss">
.agent-management {
  padding: 20px;
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h2 { margin: 0; }
  }
}
</style>
