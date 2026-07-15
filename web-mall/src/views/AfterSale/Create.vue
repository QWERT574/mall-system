<template>
  <div class="create-container">
    <div class="back-bar" @click="goBack">
      <span class="back-arrow">←</span>
      <span>返回</span>
    </div>

    <h2 class="page-title">申请售后</h2>

    <div v-if="loading" class="loading">加载中...</div>
    <template v-else>
      <div class="order-info">
        <h3 class="section-subtitle">订单信息</h3>
        <div class="info-row">
          <span class="label">订单号：</span>
          <span>{{ orderId }}</span>
        </div>
        <div class="info-row" v-if="firstItem">
          <span class="label">商品：</span>
          <span>{{ firstItem.productName || '商品' }}</span>
        </div>
        <div class="info-row" v-if="firstItem">
          <span class="label">金额：</span>
          <span class="price">¥{{ (firstItem.price * firstItem.quantity).toFixed(2) || '0.00' }}</span>
        </div>
      </div>

      <div class="form-section">
        <h3 class="section-subtitle">售后类型</h3>
        <div class="type-select">
          <div class="type-item" :class="{ active: form.serviceType === 1 }" @click="form.serviceType = 1">
            <span class="type-icon">💰</span>
            <span class="type-name">退货退款</span>
            <span class="type-desc">已收货，申请退款</span>
          </div>
          <div class="type-item" :class="{ active: form.serviceType === 2 }" @click="form.serviceType = 2">
            <span class="type-icon">🔄</span>
            <span class="type-name">换货</span>
            <span class="type-desc">商品有问题，申请换货</span>
          </div>
          <div class="type-item" :class="{ active: form.serviceType === 3 }" @click="form.serviceType = 3">
            <span class="type-icon">🔧</span>
            <span class="type-name">维修</span>
            <span class="type-desc">商品故障，申请维修</span>
          </div>
        </div>
      </div>

      <div class="form-section">
        <h3 class="section-subtitle">联系电话</h3>
        <input v-model="form.contactPhone" class="form-input" placeholder="请输入联系电话" type="tel" maxlength="11" />
      </div>

      <div class="form-section">
        <h3 class="section-subtitle">售后原因 <span class="required">*</span></h3>
        <textarea v-model="form.reason" class="form-textarea" placeholder="请详细描述您遇到的问题，如：商品有破损、功能故障等" rows="4"></textarea>
      </div>

      <div class="form-section">
        <h3 class="section-subtitle">凭证图片（可选）</h3>
        <div class="image-upload">
          <el-upload
            action="/api/upload/chat"
            list-type="picture-card"
            :auto-upload="true"
            :limit="5"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            accept=".jpg,.jpeg,.png,.webp"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">支持 jpg/png/webp 格式，最多5张</div>
            </template>
          </el-upload>
        </div>
      </div>

      <div class="form-actions">
        <button class="btn-submit" :disabled="submitting || !form.reason.trim() || !form.serviceType" @click="submitAfterSale">
          {{ submitting ? '提交中...' : '提交申请' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import api from '@/utils/api';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const orderId = route.query.orderId || '';
const loading = ref(false);
const submitting = ref(false);
const orderItem = ref(null);
const firstItem = computed(() => orderItem.value?.items?.[0] || null);

const form = reactive({
  serviceType: 0,
  reason: '',
  contactPhone: '',
  imagesList: []
});

const loadOrderDetail = async () => {
  if (!orderId) return;
  try {
    const res = await api.getOrderDetail(orderId);
    if (res.code === 0) {
      orderItem.value = res.data;
      if (res.data.contactPhone) {
        form.contactPhone = res.data.contactPhone;
      }
    }
  } catch (err) {
    console.error('加载订单信息失败:', err);
  }
};

const handleUploadSuccess = (response) => {
  const url = response?.data?.url || response?.url || ''
  if (url) form.imagesList.push(url)
}

const handleUploadRemove = (file) => {
  const url = file?.response?.data?.url || file?.response?.url || file?.url || ''
  const idx = form.imagesList.indexOf(url)
  if (idx > -1) form.imagesList.splice(idx, 1)
}

const submitAfterSale = async () => {
  if (!userStore.userInfo?.id) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  if (!form.serviceType) {
    ElMessage.warning('请选择售后类型');
    return;
  }
  if (!form.reason.trim()) {
    ElMessage.warning('请填写售后原因');
    return;
  }

  if (!orderId || !firstItem.value?.productId) {
    ElMessage.error('订单信息不完整，请重试');
    return;
  }

  submitting.value = true;
  try {
    const data = {
      orderId: parseInt(orderId),
      userId: userStore.userInfo.id,
      productId: firstItem.value.productId,
      serviceType: form.serviceType,
      reason: form.reason.trim(),
      contactPhone: form.contactPhone.trim() || userStore.userInfo.phone || '',
      images: form.imagesList.length > 0 ? JSON.stringify(form.imagesList) : ''
    };

    console.log('提交售后申请数据:', data);
    const res = await api.createAfterSale(data);
    
    if (res.code === 0 && res.data) {
      ElMessage.success('售后申请已提交成功');
      setTimeout(() => {
        router.push(`/aftersale/${res.data.id}`);
      }, 1000);
    } else {
      ElMessage.error(res.message || '提交失败：' + (typeof res.data === 'string' ? res.data : JSON.stringify(res.data)));
    }
  } catch (err) {
    console.error('提交售后申请失败:', err);
    const errorMsg = err.response?.data?.message || err.message || '网络异常，请检查连接';
    ElMessage.error('提交失败：' + errorMsg);
  } finally {
    submitting.value = false;
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  loadOrderDetail();
});
</script>

<style scoped>
.create-container { max-width: 680px; margin: 0 auto; padding: 16px; }
.back-bar { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #666; font-size: 14px; margin-bottom: 12px; }
.back-bar:hover { color: #1890ff; }
.back-arrow { font-size: 18px; }

.page-title { font-size: 20px; font-weight: 600; color: #1a1a1a; margin: 0 0 20px; }

.order-info {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}
.section-subtitle { font-size: 15px; font-weight: 500; color: #333; margin: 0 0 12px; }
.info-row { font-size: 14px; color: #333; margin-bottom: 6px; }
.info-row .label { color: #999; }
.price { color: #f5222d; font-weight: 500; }

.form-section { margin-bottom: 20px; }
.required { color: #f5222d; }

.type-select { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.type-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.type-item:hover { border-color: #1890ff; }
.type-item.active { border-color: #1890ff; background: #e6f7ff; }
.type-icon { display: block; font-size: 28px; margin-bottom: 6px; }
.type-name { display: block; font-size: 14px; font-weight: 500; color: #333; }
.type-desc { display: block; font-size: 12px; color: #999; margin-top: 4px; }

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}
.form-input:focus { border-color: #1890ff; }

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
}
.form-textarea:focus { border-color: #1890ff; }

.image-upload { display: flex; flex-direction: column; gap: 10px; }
.image-input-row { display: flex; gap: 8px; }
.image-input-row .form-input { flex: 1; }
.btn-add {
  padding: 10px 16px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}
.btn-add:hover { background: #40a9ff; }

.image-preview { display: flex; flex-wrap: wrap; gap: 8px; }
.preview-item { position: relative; }
.preview-img { width: 72px; height: 72px; object-fit: cover; border-radius: 4px; border: 1px solid #f0f0f0; }
.remove-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  background: #ff4d4f;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
}

.form-actions { padding: 16px 0 32px; }
.btn-submit {
  width: 100%;
  padding: 12px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
}
.btn-submit:disabled { background: #d9d9d9; cursor: not-allowed; }
.btn-submit:hover:not(:disabled) { background: #40a9ff; }

.loading { text-align: center; padding: 60px 0; color: #999; }

@media (max-width: 768px) {
  .create-container { padding: 10px; }
  .type-select { grid-template-columns: 1fr; }
  .form-input, .form-textarea { font-size: 16px; }
  :deep(.el-upload--picture-card) { width: 80px; height: 80px; }
  :deep(.el-upload-list__item) { width: 80px; height: 80px; }
}

@media (max-width: 480px) {
  .create-container { padding: 6px; }
  .page-title { font-size: 17px; }
  .order-info { padding: 12px; }
  .form-section { margin-bottom: 14px; }
  .section-subtitle { font-size: 14px; }
  .type-item { padding: 12px 8px; }
  .type-icon { font-size: 24px; }
  .type-name { font-size: 13px; }
  .type-desc { font-size: 11px; }
  .btn-submit { font-size: 14px; padding: 10px; }
}
</style>