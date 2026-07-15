<template>
  <div class="activity-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>活动管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            创建活动
          </el-button>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="活动类型">
          <el-select v-model="queryParams.activityType" placeholder="请选择类型" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="大宗采购" :value="1" />
            <el-option label="农场参观" :value="2" />
            <el-option label="实地观光" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="筹备中" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
            <el-option label="已取消" :value="3" />
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
        <el-table-column prop="id" label="活动 ID" width="80" />
        <el-table-column prop="name" label="活动名称" min-width="150" />
        <el-table-column prop="coverImage" label="封面图片" width="100">
          <template #default="{ row }">
            <el-image 
              v-if="row.coverImage"
              :src="row.coverImage" 
              :preview-src-list="[row.coverImage]"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="activityType" label="活动类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getActivityTypeType(row.activityType)">
              {{ getActivityTypeText(row.activityType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="活动地点" min-width="150" />
        <el-table-column prop="maxParticipants" label="最大人数" width="100" />
        <el-table-column prop="currentParticipants" label="当前人数" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="status" label="活动状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="推荐" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRecommended === 1 ? 'success' : 'info'">
              {{ row.isRecommended === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="success" size="small" @click="handleParticipants(row)">
              参与者
            </el-button>
            <el-button link type="warning" size="small" @click="handleRecommend(row)">
              {{ row.isRecommended === 1 ? '取消推荐' : '推荐' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
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
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" :width="dialogWidth">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动类型" prop="activityType">
          <el-select v-model="formData.activityType" placeholder="请选择活动类型">
            <el-option label="大宗采购" :value="1" />
            <el-option label="农场参观" :value="2" />
            <el-option label="实地观光" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述"
          />
        </el-form-item>
        <el-form-item label="活动地点" prop="location">
          <el-input v-model="formData.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="最大人数" prop="maxParticipants">
          <el-input-number v-model="formData.maxParticipants" :min="0" placeholder="请输入最大参与人数" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="formData.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            placeholder="请选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="封面图片" prop="coverImage">
          <el-input v-model="formData.coverImage" placeholder="请输入封面图片 URL 或点击上方上传图片" />
        </el-form-item>
        <el-form-item label="活动图片">
          <el-input 
            v-model="formData.images" 
            type="textarea" 
            :rows="3"
            placeholder="请输入多张图片 URL，用逗号分隔" 
          />
        </el-form-item>
        <el-form-item label="是否推荐" prop="isRecommended">
          <el-switch v-model="formData.isRecommended" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="推荐排序" prop="recommendOrder" v-if="formData.isRecommended === 1">
          <el-input-number v-model="formData.recommendOrder" :min="0" placeholder="数字越小越靠前" />
        </el-form-item>
        <el-form-item label="活动状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择活动状态">
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActivityList, createActivity, updateActivity, deleteActivity } from '@/api/activity'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('创建活动')
const formRef = ref()
const isEdit = ref(false)

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => { window.addEventListener('resize', onResize) })
onBeforeUnmount(() => { window.removeEventListener('resize', onResize) })
const dialogWidth = computed(() => windowWidth.value < 768 ? '90%' : '700px')

const queryParams = reactive({
  page: 1,
  pageSize: 20,
  status: null,
  activityType: null
})

const formData = reactive({
  id: 0,
  name: '',
  description: '',
  activityType: 1,
  location: '',
  maxParticipants: 0,
  startTime: '',
  endTime: '',
  coverImage: '',
  images: '',
  isRecommended: 0,
  recommendOrder: 999,
  status: 1
})

const formRules = {
  name: [
    { required: true, message: '请输入活动名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入活动描述', trigger: 'blur' }
  ],
  activityType: [
    { required: true, message: '请选择活动类型', trigger: 'change' }
  ],
  location: [
    { required: true, message: '请输入活动地点', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getActivityList(queryParams)
    const data = res.data || res
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取活动列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.page = 1
  fetchData()
}

const handleReset = () => {
  queryParams.status = null
  queryParams.activityType = null
  queryParams.page = 1
  fetchData()
}

const getActivityTypeText = (type: number) => {
  const typeMap = {
    1: '大宗采购',
    2: '农场参观',
    3: '实地观光'
  }
  return typeMap[type] || '未知'
}

const getActivityTypeType = (type: number) => {
  const typeMap = {
    1: 'primary',
    2: 'success',
    3: 'warning'
  }
  return typeMap[type] || 'info'
}

const getStatusText = (status: number) => {
  const statusMap = {
    0: '筹备中',
    1: '进行中',
    2: '已结束',
    3: '已取消'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status: number) => {
  const typeMap = {
    0: 'info',
    1: 'primary',
    2: 'success',
    3: 'danger'
  }
  return typeMap[status] || 'info'
}

const handleAdd = () => {
  dialogTitle.value = '创建活动'
  isEdit.value = false
  Object.assign(formData, {
    id: 0,
    name: '',
    description: '',
    activityType: 1,
    location: '',
    maxParticipants: 0,
    startTime: '',
    endTime: '',
    coverImage: '',
    images: '',
    isRecommended: 0,
    recommendOrder: 999,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑活动'
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleParticipants = (row: any) => {
  ElMessage.info(`查看活动 "${row.name}" 的参与者列表，功能待完善`)
}

const handleRecommend = async (row: any) => {
  try {
    const newRecommendStatus = row.isRecommended === 1 ? 0 : 1
    await updateActivity(row.id, { ...row, isRecommended: newRecommendStatus })
    ElMessage.success(newRecommendStatus === 1 ? '推荐成功' : '取消推荐成功')
    fetchData()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该活动吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteActivity(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除活动失败:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await updateActivity(formData.id, formData)
          ElMessage.success('更新成功')
        } else {
          await createActivity(formData)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error('提交失败:', error)
      }
    }
  })
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
.activity-container {
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

@media (max-width: 768px) {
  .activity-container {
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
}

@media (max-width: 480px) {
  .activity-container {
    padding: 8px;
  }
  .card-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
  :deep(.el-card__body) {
    padding: 10px;
  }
}
</style>
