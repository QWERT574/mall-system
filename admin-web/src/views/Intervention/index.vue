<template>
  <div class="intervention-page">
    <div class="page-header">
      <h2>人工介入管理</h2>
      <div class="stats-cards">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value pending">{{ stats.pending }}</div>
          <div class="stat-label">待处理</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value processing">{{ stats.processing }}</div>
          <div class="stat-label">处理中</div>
        </el-card>
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value completed">{{ stats.completed }}</div>
          <div class="stat-label">已解决</div>
        </el-card>
      </div>
    </div>

    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="issueType" label="问题类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="issueTypeTag(row.issueType)" size="small">{{ issueTypeText(row.issueType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="买家" min-width="90" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.buyerName || ('用户#' + row.userId) }}
          </template>
        </el-table-column>
        <el-table-column label="商家" min-width="90" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.sellerName || row.sellerShopName || ('商家#' + row.sellerId) }}
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.productName || (row.productId ? '商品#' + row.productId : '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button
              size="small"
              type="warning"
              :disabled="row.status !== 0"
              @click="handleAssign(row)"
            >
              分配
            </el-button>
            <el-button
              size="small"
              type="success"
              :disabled="row.status === 2 || row.status === 3"
              @click="handleProcess(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="介入详情" width="700px" :close-on-click-modal="false">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="ID" :span="1">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="1">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="问题类型" :span="1">
          <el-tag :type="issueTypeTag(currentRow.issueType)" size="small">{{ issueTypeText(currentRow.issueType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态" :span="1">
          <el-tag :type="statusTag(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="买家" :span="1">{{ currentRow.buyerName || ('用户#' + currentRow.userId) }}</el-descriptions-item>
        <el-descriptions-item label="商家" :span="1">{{ currentRow.sellerName || currentRow.sellerShopName || ('商家#' + currentRow.sellerId) }}</el-descriptions-item>
        <el-descriptions-item label="订单号" :span="1">{{ currentRow.orderNo || (currentRow.orderId ? '订单#' + currentRow.orderId : '-') }}</el-descriptions-item>
        <el-descriptions-item label="商品" :span="1">{{ currentRow.productName || (currentRow.productId ? '商品#' + currentRow.productId : '-') }}</el-descriptions-item>
        <el-descriptions-item label="纠纷金额" :span="1">{{ currentRow.amount || currentRow.orderAmount ? '¥' + (currentRow.amount || currentRow.orderAmount || 0).toFixed(2) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="1">{{ currentRow.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">
          <p style="margin: 0; white-space: pre-wrap">{{ currentRow.description || '无' }}</p>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.evidenceImages" label="证据图片" :span="2">
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <el-image
              v-for="(img, idx) in currentRow.evidenceImages.split(',')"
              :key="idx"
              :src="img"
              :preview-src-list="currentRow.evidenceImages.split(',')"
              fit="cover"
              style="width: 80px; height: 80px; border-radius: 4px;"
            />
          </div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.result" label="处理结果" :span="2">
          <p style="margin: 0; white-space: pre-wrap">{{ currentRow.result }}</p>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.adminRemark" label="管理员备注" :span="2">
          <p style="margin: 0; white-space: pre-wrap">{{ currentRow.adminRemark }}</p>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="processVisible" title="处理介入申请" width="500px" :close-on-click-modal="false">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="处理结果">
          <el-select v-model="processForm.status" placeholder="选择状态" style="width: 100%">
            <el-option label="已解决" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="管理员备注">
          <el-input
            v-model="processForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入处理备注"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProcess">确认处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配管理员" width="400px" :close-on-click-modal="false">
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="管理员ID">
          <el-input-number v-model="assignForm.adminId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getInterventionList,
  getInterventionDetail,
  getInterventionStats,
  processIntervention,
  assignAdmin,
  type InterventionRecord
} from '@/api/intervention'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<InterventionRecord[]>([])
const total = ref(0)
const detailVisible = ref(false)
const processVisible = ref(false)
const assignVisible = ref(false)
const currentRow = ref<InterventionRecord | null>(null)

const stats = reactive({
  pending: 0,
  processing: 0,
  completed: 0
})

const queryParams = reactive({
  page: 1,
  size: 10,
  status: undefined as number | undefined
})

const processForm = reactive({
  status: 2,
  remark: ''
})

const assignForm = reactive({
  adminId: 1
})

function statusTag(status: number): 'success' | 'primary' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 0: return 'danger'
    case 1: return 'warning'
    case 2: return 'success'
    case 3: return 'info'
    default: return 'info'
  }
}

function statusLabel(status: number): string {
  switch (status) {
    case 0: return '待处理'
    case 1: return '处理中'
    case 2: return '已解决'
    case 3: return '已关闭'
    default: return '未知'
  }
}

function issueTypeTag(type: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    '售后纠纷': 'danger',
    '商品投诉': 'warning',
    '商家投诉': 'warning',
    '订单问题': 'primary',
    'chat_dispute': 'danger',
    'quality': 'warning',
    'logistics': 'primary',
    'service': 'danger',
    'attitude': 'warning',
    'other': 'info',
    '其他': 'info'
  }
  return map[type] || 'info'
}

function issueTypeText(type: string): string {
  const map: Record<string, string> = {
    'chat_dispute': '聊天纠纷',
    'quality': '商品质量',
    'logistics': '物流问题',
    'service': '售后服务',
    'attitude': '商家态度',
    'other': '其他问题',
    '售后纠纷': '售后纠纷',
    '商品投诉': '商品投诉',
    '商家投诉': '商家投诉',
    '订单问题': '订单问题',
    '其他': '其他'
  }
  return map[type] || type
}

function resetQuery() {
  queryParams.status = undefined
  queryParams.page = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getInterventionList({
      page: queryParams.page,
      size: queryParams.size,
      status: queryParams.status
    })
    const data = res
    if (data) {
      tableData.value = data.records || []
      total.value = data.total || 0
    }
  } catch (e: any) {
    ElMessage.error('获取列表失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await getInterventionStats()
    const data = res
    if (data) {
      stats.pending = data.pending || 0
      stats.processing = data.processing || 0
      stats.completed = data.completed || 0
    }
  } catch {
    console.warn('获取统计数据失败')
  }
}

async function viewDetail(row: InterventionRecord) {
  try {
    const detail = await getInterventionDetail(row.id)
    currentRow.value = detail || row
  } catch {
    currentRow.value = row
  }
  detailVisible.value = true
}

function handleProcess(row: InterventionRecord) {
  currentRow.value = row
  processForm.status = 2
  processForm.remark = ''
  processVisible.value = true
}

function handleAssign(row: InterventionRecord) {
  currentRow.value = row
  assignForm.adminId = row.adminId || 1
  assignVisible.value = true
}

async function submitProcess() {
  if (!currentRow.value) return
  submitting.value = true
  try {
    const adminId = assignForm.adminId || 1
    await processIntervention(currentRow.value.id, {
      status: processForm.status,
      remark: processForm.remark,
      adminId
    })
    ElMessage.success('处理成功')
    processVisible.value = false
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error('处理失败: ' + (e.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

async function submitAssign() {
  if (!currentRow.value) return
  submitting.value = true
  try {
    await assignAdmin(currentRow.value.id, assignForm.adminId)
    ElMessage.success('分配成功')
    assignVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error('分配失败: ' + (e.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<style scoped>
.intervention-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.stats-cards {
  display: flex;
  gap: 16px;
}

.stat-card {
  min-width: 140px;
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
}

.stat-value.pending {
  color: #e74c3c;
}

.stat-value.processing {
  color: #f39c12;
}

.stat-value.completed {
  color: #27ae60;
}

.stat-label {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.filter-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
