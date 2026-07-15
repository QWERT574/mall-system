<template>
  <div class="seller-review-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商家审核管理</span>
          <el-button type="primary" @click="loadSellerList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="statistics-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ statistics.total }}</div>
            <div class="stat-label">商家总数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card pending">
            <div class="stat-value">{{ statistics.pending }}</div>
            <div class="stat-label">待审核</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card approved">
            <div class="stat-value">{{ statistics.approved }}</div>
            <div class="stat-label">已通过</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card rejected">
            <div class="stat-value">{{ statistics.rejected }}</div>
            <div class="stat-label">已拒绝</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="审核状态">
          <el-select v-model="filterForm.isVerified" placeholder="全部" clearable @change="loadSellerList">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable @change="loadSellerList">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input 
            v-model="filterForm.keyword" 
            placeholder="商家名称/手机号/联系人" 
            clearable
            @keyup.enter="loadSellerList"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSellerList">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 商家列表表格 -->
      <el-table :data="sellerList" v-loading="loading" style="width: 100%" border>
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="companyName" label="店铺名称" width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="审核状态" width="110">
          <template #default="scope">
            <el-tag :type="getVerifyType(scope.row.isVerified)">
              {{ getVerifyText(scope.row.isVerified) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleViewDetail(scope.row)"
            >
              详情
            </el-button>
            <el-button
              v-if="scope.row.isVerified === 0"
              link
              type="success"
              size="small"
              @click="handleApprove(scope.row)"
            >
              通过
            </el-button>
            <el-button
              v-if="scope.row.isVerified === 0"
              link
              type="danger"
              size="small"
              @click="handleReject(scope.row)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="scope.row.isVerified === 1"
              link
              type="warning"
              size="small"
              @click="handleDisable(scope.row)"
            >
              禁用
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              link
              type="success"
              size="small"
              @click="handleEnable(scope.row)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
    
    <!-- 商家详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="商家详情"
      :width="detailDialogWidth"
      destroy-on-close
    >
      <el-tabs v-model="detailActiveTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border v-if="currentSeller">
            <el-descriptions-item label="ID">{{ currentSeller.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ currentSeller.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ currentSeller.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ currentSeller.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ currentSeller.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="店铺名称">{{ currentSeller.companyName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="店铺地址">{{ currentSeller.companyAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ currentSeller.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentSeller.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <el-tag :type="getVerifyType(currentSeller.isVerified)">
                {{ getVerifyText(currentSeller.isVerified) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag :type="currentSeller.status === 1 ? 'success' : 'danger'">
                {{ currentSeller.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDate(currentSeller.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ formatDate(currentSeller.verifiedAt) }}</el-descriptions-item>
            <el-descriptions-item label="审核信息" :span="2">
              {{ currentSeller.verificationInfo || '暂无审核信息' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        
        <el-tab-pane label="资质信息" name="qualification">
          <div v-if="currentSeller && currentSeller.qualifications" class="qualification-section">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="营业执照">
                <div v-if="currentSeller.qualifications.businessLicense">
                  <el-image
                    :src="currentSeller.qualifications.businessLicense"
                    :preview-src-list="[currentSeller.qualifications.businessLicense]"
                    fit="cover"
                    style="width: 200px; height: 140px; border-radius: 4px;"
                  />
                </div>
                <span v-else class="empty-text">未上传</span>
              </el-descriptions-item>
              <el-descriptions-item label="法人身份证（正面）">
                <div v-if="currentSeller.qualifications.idCardFront">
                  <el-image
                    :src="currentSeller.qualifications.idCardFront"
                    :preview-src-list="[currentSeller.qualifications.idCardFront]"
                    fit="cover"
                    style="width: 200px; height: 140px; border-radius: 4px;"
                  />
                </div>
                <span v-else class="empty-text">未上传</span>
              </el-descriptions-item>
              <el-descriptions-item label="法人身份证（反面）">
                <div v-if="currentSeller.qualifications.idCardBack">
                  <el-image
                    :src="currentSeller.qualifications.idCardBack"
                    :preview-src-list="[currentSeller.qualifications.idCardBack]"
                    fit="cover"
                    style="width: 200px; height: 140px; border-radius: 4px;"
                  />
                </div>
                <span v-else class="empty-text">未上传</span>
              </el-descriptions-item>
              <el-descriptions-item label="经营许可证">
                <div v-if="currentSeller.qualifications.permitLicense">
                  <el-image
                    :src="currentSeller.qualifications.permitLicense"
                    :preview-src-list="[currentSeller.qualifications.permitLicense]"
                    fit="cover"
                    style="width: 200px; height: 140px; border-radius: 4px;"
                  />
                </div>
                <span v-else class="empty-text">未上传</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="暂无资质信息" />
        </el-tab-pane>
        
        <el-tab-pane label="审核记录" name="history">
          <el-timeline v-if="auditHistory.length > 0">
            <el-timeline-item
              v-for="(item, index) in auditHistory"
              :key="index"
              :type="item.action === 'approve' ? 'success' : item.action === 'reject' ? 'danger' : 'primary'"
              :timestamp="formatDate(item.createdAt)"
            >
              <el-card shadow="never" class="audit-card">
                <div class="audit-header">
                  <el-tag 
                    :type="item.action === 'approve' ? 'success' : item.action === 'reject' ? 'danger' : 'primary'"
                    size="small"
                  >
                    {{ item.action === 'approve' ? '通过' : item.action === 'reject' ? '拒绝' : '修改' }}
                  </el-tag>
                  <span class="audit-admin">操作人：{{ item.adminName || '系统' }}</span>
                </div>
                <p class="audit-remark">{{ item.remark || '无备注' }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审核记录" />
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="currentSeller && currentSeller.isVerified === 0" 
          type="success" 
          @click="handleApproveFromDetail"
        >
          通过审核
        </el-button>
        <el-button 
          v-if="currentSeller && currentSeller.isVerified === 0" 
          type="danger" 
          @click="handleRejectFromDetail"
        >
          拒绝审核
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 拒绝原因对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝审核"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因，将通知给商家"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确定拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const sellerList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentSeller = ref(null)
const detailActiveTab = ref('basic')
const auditHistory = ref([])

// 统计数据
const statistics = reactive({
  total: 0,
  pending: 0,
  approved: 0,
  rejected: 0
})

const filterForm = reactive({
  isVerified: null,
  status: null,
  keyword: ''
})

const rejectForm = reactive({
  reason: ''
})

// 加载商家列表
const loadSellerList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (filterForm.isVerified !== null && filterForm.isVerified !== undefined) {
      params.isVerified = filterForm.isVerified
    }
    if (filterForm.status !== null && filterForm.status !== undefined) {
      params.status = filterForm.status
    }
    if (filterForm.keyword) {
      params.keyword = filterForm.keyword
    }
    
    const response = await request.get('/seller/list', { params })
    if (response) {
      sellerList.value = response.list || []
      total.value = response.total || 0
    }
  } catch (error) {
    console.error('加载商家列表失败:', error)
    ElMessage.error('加载商家列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const response = await request.get('/seller/statistics')
    if (response) {
      Object.assign(statistics, response || {})
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 重置筛选
const handleReset = () => {
  filterForm.isVerified = null
  filterForm.status = null
  filterForm.keyword = ''
  pageNum.value = 1
  loadSellerList()
}

// 通过审核
const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要通过商家"${row.companyName || row.nickname || row.username}"的审核吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await request.post(`/seller/approve/${row.id}`)
    if (response) {
      ElMessage.success('审核通过成功')
      loadSellerList()
      loadStatistics()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核通过失败:', error)
      ElMessage.error('审核通过失败')
    }
  }
}

// 拒绝审核
const handleReject = (row) => {
  currentSeller.value = row
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  
  try {
    const response = await request.post(
      `/seller/reject/${currentSeller.value.id}`,
      { reason: rejectForm.reason }
    )

    if (response) {
      ElMessage.success('审核拒绝成功')
      rejectDialogVisible.value = false
      loadSellerList()
      loadStatistics()
    }
  } catch (error) {
    console.error('审核拒绝失败:', error)
    ElMessage.error('审核拒绝失败')
  }
}

// 禁用商家
const handleDisable = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要禁用商家"${row.companyName || row.nickname || row.username}"吗？禁用后商家将无法登录。`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await request.post(`/seller/disable/${row.id}`)
    if (response) {
      ElMessage.success('禁用成功')
      loadSellerList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('禁用失败:', error)
      ElMessage.error('禁用失败')
    }
  }
}

// 启用商家
const handleEnable = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要启用商家"${row.companyName || row.nickname || row.username}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    const response = await request.post(`/seller/enable/${row.id}`)
    if (response) {
      ElMessage.success('启用成功')
      loadSellerList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('启用失败:', error)
      ElMessage.error('启用失败')
    }
  }
}

// 查看详情
const handleViewDetail = async (row) => {
  currentSeller.value = row
  detailActiveTab.value = 'basic'
  detailDialogVisible.value = true
  
  // 加载审核记录
  try {
    const response = await request.get(`/seller/audit-history/${row.id}`)
    if (response) {
      auditHistory.value = response || []
    }
  } catch (error) {
    console.error('加载审核记录失败:', error)
    auditHistory.value = []
  }
}

// 从详情页通过审核
const handleApproveFromDetail = () => {
  if (currentSeller.value) {
    handleApprove(currentSeller.value)
    detailDialogVisible.value = false
  }
}

// 从详情页拒绝审核
const handleRejectFromDetail = () => {
  if (currentSeller.value) {
    detailDialogVisible.value = false
    handleReject(currentSeller.value)
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取审核状态类型
const getVerifyType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取审核状态文本
const getVerifyText = (status) => {
  const textMap = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return textMap[status] || '未知'
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadSellerList()
}

const handlePageChange = (page) => {
  pageNum.value = page
  loadSellerList()
}

onMounted(() => {
  loadSellerList()
  loadStatistics()
})
</script>

<style scoped>
.seller-review-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.statistics-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-card .stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary-500);
  margin-bottom: 8px;
}

.stat-card .stat-label {
  font-size: 14px;
  color: #666;
}

.stat-card.pending .stat-value {
  color: #E6A23C;
}

.stat-card.approved .stat-value {
  color: #67C23A;
}

.stat-card.rejected .stat-value {
  color: var(--color-error);
}

.filter-form {
  margin-bottom: 20px;
  padding: 20px;
  background: var(--color-bg-page);
  border-radius: 4px;
}

.empty-text {
  color: #999;
  font-size: 14px;
}

.qualification-section {
  padding: 10px;
}

.audit-card {
  margin-bottom: 10px;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.audit-admin {
  font-size: 12px;
  color: #999;
}

.audit-remark {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-descriptions-item__label) {
  font-weight: 500;
  width: 120px;
}

@media (max-width: 768px) {
  .seller-review-container {
    padding: 12px;
  }
  .statistics-row :deep(.el-col-6) {
    max-width: 50%;
    flex: 0 0 50%;
  }
  .stat-card {
    padding: 12px;
  }
  .stat-card .stat-value {
    font-size: 24px;
  }
  .filter-form {
    padding: 12px;
  }
  .filter-form :deep(.el-form-item) {
    margin-right: 0;
    width: 100%;
  }
  .filter-form :deep(.el-input),
  .filter-form :deep(.el-select) {
    width: 100% !important;
  }
  :deep(.el-table) {
    font-size: 12px;
  }
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 6px 0;
  }
  :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .seller-review-container {
    padding: 8px;
  }
  .statistics-row :deep(.el-col-6) {
    max-width: 100%;
    flex: 0 0 100%;
  }
  :deep(.el-card__body) {
    padding: 10px;
  }
}
</style>
