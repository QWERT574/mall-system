<template>
  <div class="review-container">
    <h2 class="page-title">订单评价</h2>
    
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!order" class="empty">订单不存在</div>
    <div v-else class="review-content">
      <!-- 订单信息 -->
      <div class="order-info">
        <h3>订单信息</h3>
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ order.orderSn || '#' + order.id }}</span>
        </div>
        <div class="info-item">
          <span class="label">下单时间：</span>
          <span class="value">{{ formatDateTime(order.createdAt || order.createTime) }}</span>
        </div>
        <div class="info-item">
          <span class="label">订单金额：</span>
          <span class="value total">¥{{ order.totalPrice || 0 }}</span>
        </div>
      </div>
      
      <!-- 商品列表 -->
      <div class="product-list">
        <h3>商品列表</h3>
        <div class="product-item" v-for="item in order.items" :key="item.id">
          <img :src="item.cover || item.productImage" :alt="item.productName" />
          <div class="product-info">
            <h4>{{ item.productName }}</h4>
            <p class="price">¥{{ item.price }} × {{ item.quantity }}</p>
            
            <!-- 评价输入 -->
            <div class="review-input">
              <div class="rating">
                <span>评分：</span>
                <el-rate v-model="item.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
              </div>
              <el-input
                v-model="item.comment"
                type="textarea"
                :rows="3"
                placeholder="请输入您对商品的评价..."
                maxlength="500"
                show-word-limit
              />
              <div class="image-upload">
                <el-upload
                  action="/api/upload/chat"
                  list-type="picture-card"
                  :auto-upload="true"
                  :on-success="(res, file) => handleUploadSuccess(res, file, item)"
                  :on-remove="(file) => handleUploadRemove(file, item)"
                  :limit="3"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
                <div class="upload-tip">最多上传 3 张图片</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 提交按钮 -->
      <div class="submit-section">
        <el-button type="primary" size="large" @click="submitReview" :loading="submitting">
          提交评价
        </el-button>
        <el-button size="large" @click="goBack">返回</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import api from '@/utils/api';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(false);
const submitting = ref(false);

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-';
  if (Array.isArray(dateStr)) {
    const [year, month, day, hour, minute, second] = dateStr;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return '-';
  return date.toLocaleString('zh-CN');
};

// 处理图片上传
const handleUploadSuccess = (response, file, item) => {
  if (!item.imageUrls) item.imageUrls = [];
  const url = response?.data?.url || response?.url || '';
  if (url) item.imageUrls.push(url);
};

const handleUploadRemove = (file, item) => {
  if (!item.imageUrls) return;
  const url = file?.response?.data?.url || file?.response?.url || file?.url || '';
  const idx = item.imageUrls.indexOf(url);
  if (idx > -1) item.imageUrls.splice(idx, 1);
};

// 加载订单详情
const loadOrder = async () => {
  loading.value = true;
  try {
    const orderId = route.params.orderId;
    const response = await api.get(`/order/detail/${orderId}`);
    
    if (response.code === 0) {
      order.value = response.data;
      // 初始化商品评价数据
      if (order.value.items) {
        order.value.items.forEach(item => {
          item.rating = 5;
          item.comment = '';
          item.images = [];
        });
      }
    } else {
      ElMessage.error(response.message || '加载订单失败');
    }
  } catch (error) {
    console.error('加载订单失败:', error);
    ElMessage.error('加载订单失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 提交评价
const submitReview = async () => {
  if (!order.value || !order.value.items) {
    ElMessage.error('订单信息不完整');
    return;
  }
  
  // 验证评价
  for (const item of order.value.items) {
    if (!item.comment || item.comment.trim() === '') {
      ElMessage.error('请输入对商品的评价');
      return;
    }
  }
  
  submitting.value = true;
  try {
    // 提交评价数据
    const reviewData = {
      orderId: order.value.id,
      items: order.value.items.map(item => ({
        productId: item.productId,
        rating: item.rating,
        comment: item.comment,
        images: (item.imageUrls || []).join(',')
      }))
    };
    
    const response = await api.post('/review/submit', reviewData);
    
    if (response.code === 0) {
      ElMessage.success('评价提交成功！');
      setTimeout(() => {
        router.push('/order-list');
      }, 1500);
    } else {
      ElMessage.error(response.message || '评价提交失败');
    }
  } catch (error) {
    console.error('提交评价失败:', error);
    ElMessage.error('评价提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
};

// 返回
const goBack = () => {
  router.back();
};

onMounted(() => {
  loadOrder();
});
</script>

<style scoped>
.review-container { max-width: 800px; margin: 0 auto; padding: 20px; }
.page-title { font-size: 28px; font-weight: 600; color: #333; margin-bottom: 30px; text-align: center; }
.order-info, .product-list { background: white; border-radius: 12px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.order-info h3, .product-list h3 { font-size: 18px; font-weight: 600; margin-bottom: 15px; color: #333; border-bottom: 2px solid #f0f0f0; padding-bottom: 10px; }
.info-item { display: flex; margin-bottom: 10px; font-size: 14px; }
.info-item .label { color: #666; width: 100px; }
.info-item .value { color: #333; }
.info-item .value.total { font-size: 18px; font-weight: 600; color: #ff6b6b; }
.product-item { display: flex; gap: 15px; padding: 15px 0; border-bottom: 1px solid #f0f0f0; }
.product-item:last-child { border-bottom: none; }
.product-item img { width: 100px; height: 100px; object-fit: cover; border-radius: 8px; }
.product-info { flex: 1; }
.product-info h4 { font-size: 16px; margin-bottom: 8px; }
.product-info .price { color: #ff6b6b; font-size: 16px; font-weight: 600; margin-bottom: 15px; }
.review-input { background: #f9f9f9; padding: 15px; border-radius: 8px; }
.rating { display: flex; align-items: center; margin-bottom: 10px; }
.rating span { color: #666; margin-right: 10px; }
.image-upload { margin-top: 10px; }
.upload-tip { color: #999; font-size: 12px; margin-top: 5px; }
.submit-section { text-align: center; padding: 20px; }
.submit-section button { margin: 0 10px; }
.loading, .empty { text-align: center; padding: 60px; color: #999; font-size: 16px; }

@media (max-width: 768px) {
  .review-container { padding: 12px; }
  .page-title { font-size: 22px; margin-bottom: 20px; }
  .product-item { flex-direction: column; gap: 10px; }
  .product-item img { width: 80px; height: 80px; }
  .info-item .label { width: 80px; }
  :deep(.el-upload--picture-card) { width: 80px; height: 80px; }
  :deep(.el-upload-list__item) { width: 80px; height: 80px; }
}

@media (max-width: 480px) {
  .review-container { padding: 8px; }
  .page-title { font-size: 18px; margin-bottom: 14px; }
  .order-info, .product-list { padding: 14px; }
  .order-info h3, .product-list h3 { font-size: 16px; }
  .product-item img { width: 64px; height: 64px; }
  .product-info h4 { font-size: 14px; }
  .product-info .price { font-size: 14px; }
  .review-input { padding: 10px; }
  .info-item { font-size: 13px; }
  .info-item .label { width: 70px; }
  .submit-section { padding: 14px; }
}
</style>
