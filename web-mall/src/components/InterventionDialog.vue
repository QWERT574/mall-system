<template>
  <el-dialog
    v-model="visible"
    title="申请管理员介入"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="intervention-form">
      <el-form :model="form" label-width="80px">
        <el-form-item label="订单号">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-select v-model="form.issueType" placeholder="请选择问题类型">
            <el-option label="商品质量问题" value="quality" />
            <el-option label="物流问题" value="logistics" />
            <el-option label="售后服务问题" value="service" />
            <el-option label="商家态度问题" value="attitude" />
            <el-option label="其他问题" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述您遇到的问题"
          />
        </el-form-item>
        <el-form-item label="凭证图片">
          <el-upload
            action="/api/upload/chat"
            list-type="picture-card"
            :auto-upload="true"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '@/utils/api'

const props = defineProps({
  modelValue: Boolean,
  seller: { type: Object, default: null },
  productId: { type: [String, Number], default: null },
  productName: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'submitted'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

const submitting = ref(false)
const uploadedUrls = ref([])

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const form = ref({
  orderNo: '',
  issueType: '',
  description: ''
})

const handleUploadSuccess = (response) => {
  if (response.code === 0 && response.data) {
    uploadedUrls.value.push(response.data.url || response.data)
  }
}

const handleUploadRemove = (file) => {
  const url = file.response?.data?.url || file.response?.data
  const idx = uploadedUrls.value.indexOf(url)
  if (idx > -1) uploadedUrls.value.splice(idx, 1)
}

const getIssueTypeLabel = (type) => {
  const map = {
    quality: '商品质量问题',
    logistics: '物流问题',
    service: '售后服务问题',
    attitude: '商家态度问题',
    other: '其他问题'
  }
  return map[type] || type
}

const handleSubmit = async () => {
  const { orderNo, issueType, description } = form.value
  if (!issueType || !description) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    submitting.value = true
    const res = await api.post('/admin/intervention', {
      userId: JSON.parse(localStorage.getItem('userInfo') || '{}').id,
      sellerId: props.seller?.id,
      productId: props.productId,
      issueType,
      title: getIssueTypeLabel(issueType) + ' - ' + (props.productName || '商品'),
      description,
      orderNo: orderNo || null,
      evidenceImages: uploadedUrls.value.length > 0 ? uploadedUrls.value.join(',') : null
    })

    if (res.code === 0 || res.code === 200) {
      ElMessage.success('申请已提交，管理员会尽快处理')
      visible.value = false
      form.value = { orderNo: '', issueType: '', description: '' }
      uploadedUrls.value = []
      emit('submitted')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (error) {
    console.error('提交申请失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

watch(visible, (v) => {
  if (!v) {
    form.value = { orderNo: '', issueType: '', description: '' }
    uploadedUrls.value = []
  }
})
</script>

<style scoped>
.intervention-form {
  padding: 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
