<template>
  <el-dialog
    v-model="visible"
    title="发表评价"
    width="700px"
    :close-on-click-modal="false"
    class="write-review-dialog"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="评分">
        <el-rate v-model="form.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
      </el-form-item>
      <el-form-item label="评价">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          placeholder="分享您的使用感受，帮助其他买家了解商品..."
        />
      </el-form-item>
      <el-form-item label="图片">
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
      <el-form-item label="匿名">
        <el-switch v-model="form.anonymous" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交评价</el-button>
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
  productId: { type: [String, Number], default: null },
  currentUserId: { type: [Number, String], default: null }
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
  rating: 5,
  content: '',
  anonymous: false
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

const handleSubmit = async () => {
  if (!form.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }

  if (!props.currentUserId) {
    ElMessage.warning('请先登录后再发表评价')
    return
  }

  try {
    submitting.value = true

    await api.createReview({
      productId: props.productId,
      userId: props.currentUserId,
      rating: form.value.rating,
      content: form.value.content,
      anonymous: form.value.anonymous ? 1 : 0,
      images: uploadedUrls.value.length > 0 ? JSON.stringify(uploadedUrls.value) : null
    })

    ElMessage.success('评价提交成功！')
    visible.value = false
    form.value = { rating: 5, content: '', anonymous: false }
    uploadedUrls.value = []
    emit('submitted')
  } catch (error) {
    console.error('提交评价失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

watch(visible, (v) => {
  if (!v) {
    form.value = { rating: 5, content: '', anonymous: false }
    uploadedUrls.value = []
  }
})
</script>

<style scoped>
.write-review-dialog .el-dialog__body {
  padding-top: 20px;
}

.write-review-dialog .el-form-item {
  margin-bottom: 20px;
}

.write-review-dialog .el-rate {
  font-size: 24px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
