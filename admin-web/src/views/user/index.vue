<template>
  <div class="user-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="请输入昵称/手机号" clearable />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="queryParams.userType" placeholder="请选择类型" clearable>
            <el-option label="普通用户" :value="0" />
            <el-option label="商品提供方" :value="1" />
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
            <el-option label="已注销" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="用户ID" width="80" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="userType" label="用户类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getUserTypeType(row.userType)">
              {{ getUserTypeText(row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isVerified" label="认证状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isVerified === 1 ? 'success' : 'info'">
              {{ row.isVerified === 1 ? '已认证' : '未认证' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === -1 ? 'danger' : 'info'">
              {{ row.status === 1 ? '启用' : row.status === -1 ? '已注销' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="success" size="small" @click="handleVerify(row)" v-if="row.userType === 1 && row.isVerified === 0">
              审核
            </el-button>
            <el-button link type="warning" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeactivate(row)" v-if="row.status !== -1 && row.userType !== 2">
              注销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
    
    <el-dialog v-model="verifyDialogVisible" title="商家审核" width="500px">
      <el-form :model="verifyForm" :rules="verifyRules" ref="verifyFormRef" label-width="100px">
        <el-form-item label="审核结果" prop="status">
          <el-radio-group v-model="verifyForm.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="拒绝原因" prop="rejectReason" v-if="verifyForm.status === 2">
          <el-input
            v-model="verifyForm.rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleVerifyConfirm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="用户详细信息" :width="detailDialogWidth" destroy-on-close>
      <div class="user-detail-container">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border class="info-section">
          <template #title>
            <div class="section-title">
              <el-icon><User /></el-icon> 基本信息
            </div>
          </template>
          <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentUser.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ currentUser.gender === 1 ? '男' : currentUser.gender === 2 ? '女' : '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="用户类型">
            <el-tag :type="getUserTypeType(currentUser.userType)">
              {{ getUserTypeText(currentUser.userType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="认证状态">
            <el-tag :type="currentUser.isVerified === 1 ? 'success' : 'warning'">
              {{ currentUser.isVerified === 1 ? '已认证' : '未认证' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="账号状态">
            <el-tag :type="currentUser.status === 1 ? 'success' : currentUser.status === -1 ? 'danger' : 'info'">
              {{ currentUser.status === 1 ? '正常' : currentUser.status === -1 ? '已注销' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="2">{{ currentUser.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="最后登录" :span="2">{{ currentUser.lastLoginTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 商家信息（仅商家角色显示） -->
        <el-descriptions :column="2" border class="info-section" v-if="currentUser.userType === 1">
          <template #title>
            <div class="section-title">
              <el-icon><Shop /></el-icon> 商家信息
            </div>
          </template>
          <el-descriptions-item label="店铺名称">{{ currentUser.shopName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentUser.contactName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentUser.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="店铺地址">{{ currentUser.shopAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="营业执照" :span="2">
            <el-image
              v-if="currentUser.licenseImage"
              :src="currentUser.licenseImage"
              :preview-src-list="[currentUser.licenseImage]"
              fit="contain"
              style="width: 120px; height: 80px; border-radius: 4px;"
            />
            <span v-else style="color: #999;">未上传</span>
          </el-descriptions-item>
          <el-descriptions-item label="店铺简介" :span="2">{{ currentUser.shopDescription || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 统计信息 -->
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="8">
            <el-card shadow="never" class="stat-mini">
              <div class="stat-label">订单总数</div>
              <div class="stat-value">{{ currentUser.orderCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="stat-mini">
              <div class="stat-label">消费总额</div>
              <div class="stat-value">¥{{ (currentUser.totalSpent || 0).toFixed(2) }}</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="stat-mini">
              <div class="stat-label">发布商品</div>
              <div class="stat-value">{{ currentUser.productCount || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleEditUser" v-if="currentUser.userType !== 2">
          编辑信息
        </el-button>
        <el-button 
          :type="currentUser.status === 1 ? 'danger' : 'success'" 
          @click="handleToggleStatusFromDetail"
        >
          {{ currentUser.status === 1 ? '禁用账号' : '启用账号' }}
        </el-button>
        <el-button 
          type="danger"
          @click="handleDeactivateFromDetail"
          v-if="currentUser.status !== -1 && currentUser.userType !== 2"
        >
          注销账号
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, verifySupplier, updateUserStatus, deactivateUser } from '@/api/user'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const verifyDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const verifyFormRef = ref()
const currentUser = ref<any>({})

const queryParams = reactive({
  page: 1,
  pageSize: 20,
  keyword: '',
  userType: undefined as number | undefined,
  status: undefined as number | undefined
})

const verifyForm = reactive({
  userId: 0,
  status: 1,
  rejectReason: ''
})

const verifyRules = {
  status: [
    { required: true, message: '请选择审核结果', trigger: 'change' }
  ],
  rejectReason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
    const data = res.data || res
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.page = 1
  fetchData()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.userType = undefined
  queryParams.status = undefined
  queryParams.page = 1
  fetchData()
}

const getUserTypeText = (userType: number) => {
  const typeMap: Record<number, string> = {
    0: '普通用户',
    1: '商品提供方',
    2: '管理员'
  }
  return typeMap[userType] || '未知'
}

const getUserTypeType = (userType: number): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const typeMap: Record<number, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    0: 'info',
    1: 'warning',
    2: 'danger'
  }
  return typeMap[userType] || 'info'
}

const handleDetail = (row: any) => {
  currentUser.value = { ...row }
  detailDialogVisible.value = true
}

const handleEditUser = () => {
  ElMessage.info('编辑用户功能开发中')
}

const handleToggleStatusFromDetail = async () => {
  try {
    await ElMessageBox.confirm(
      `确认${currentUser.value.status === 1 ? '禁用' : '启用'}该用户吗?`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateUserStatus(currentUser.value.id, currentUser.value.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    currentUser.value.status = currentUser.value.status === 1 ? 0 : 1
    fetchData()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleVerify = (row: any) => {
  verifyForm.userId = row.id
  verifyForm.status = 1
  verifyForm.rejectReason = ''
  verifyDialogVisible.value = true
}

const handleVerifyConfirm = async () => {
  if (!verifyFormRef.value) return
  
  await verifyFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        await verifySupplier(verifyForm.userId, verifyForm.status, verifyForm.rejectReason)
        ElMessage.success('审核成功')
        verifyDialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error('审核失败:', error)
      }
    }
  })
}

const handleToggleStatus = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认${row.status === 1 ? '禁用' : '启用'}该用户吗?`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchData()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDeactivate = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认注销用户「${row.nickname || row.username || row.id}」的账号吗？此操作不可恢复！`,
      '危险操作',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await deactivateUser(row.id)
    ElMessage.success('账号已注销')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('注销失败:', error)
    }
  }
}

const handleDeactivateFromDetail = async () => {
  try {
    await ElMessageBox.confirm(
      `确认注销用户「${currentUser.value.nickname || currentUser.value.username || currentUser.value.id}」的账号吗？此操作不可恢复！`,
      '危险操作',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await deactivateUser(currentUser.value.id)
    ElMessage.success('账号已注销')
    currentUser.value.status = -1
    detailDialogVisible.value = false
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('注销失败:', error)
    }
  }
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}

fetchData()
</script>

<style scoped>
.user-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-detail-container {
  max-height: 60vh;
  overflow-y: auto;
}

.info-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-primary-500);
}

.stat-mini {
  text-align: center;
  background-color: var(--color-bg-page);
  border-radius: 8px;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 22px;
  font-weight: bold;
  color: var(--color-text-primary);
}

@media (max-width: 768px) {
  .user-container {
    padding: 12px;
  }
  .search-form :deep(.el-form-item) {
    margin-right: 0;
    width: 100%;
  }
  .search-form :deep(.el-input),
  .search-form :deep(.el-select) {
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
  .stat-value {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .user-container {
    padding: 8px;
  }
  :deep(.el-card__body) {
    padding: 10px;
  }
  :deep(.el-col-8) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
