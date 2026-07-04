<template>
  <div class="review-management-container">
    <div class="page-header">
      <h2 class="page-title">评价管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索评价内容"
          style="width: 300px"
          clearable
          @clear="loadReviews"
        >
          <template #append>
            <el-button @click="loadReviews">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 评价统计 -->
    <div class="stats-cards">
      <el-card shadow="hover">
        <div class="stat-card">
          <div class="stat-label">总评价数</div>
          <div class="stat-value">{{ stats.total }}</div>
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="stat-card">
          <div class="stat-label">好评数</div>
          <div class="stat-value good">{{ stats.goodCount }}</div>
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="stat-card">
          <div class="stat-label">中评数</div>
          <div class="stat-value medium">{{ stats.mediumCount }}</div>
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="stat-card">
          <div class="stat-label">差评数</div>
          <div class="stat-value bad">{{ stats.badCount }}</div>
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="stat-card">
          <div class="stat-label">平均评分</div>
          <div class="stat-value rating">
            <el-rate v-model="stats.averageRating" disabled :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
            <span>{{ stats.averageRating.toFixed(1) }}</span>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="stat-card">
          <div class="stat-label">已回复</div>
          <div class="stat-value">{{ stats.repliedCount }}</div>
        </div>
      </el-card>
    </div>

    <!-- 评价列表 -->
    <el-card class="review-list-card">
      <div class="review-list">
        <div 
          v-for="review in reviews" 
          :key="review.id"
          class="review-item"
        >
          <div class="review-header">
            <div class="reviewer-info">
              <div class="reviewer-avatar">
                <el-icon :size="24"><User /></el-icon>
              </div>
              <div class="reviewer-name">
                {{ review.anonymous === 1 ? '匿名用户' : (review.userName || '用户') }}
              </div>
            </div>
            <div class="review-meta">
              <el-rate v-model="review.rating" disabled :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
              <span class="review-time">{{ formatDate(review.createdAt) }}</span>
            </div>
          </div>
          
          <div class="review-content">
            <div class="review-text">{{ review.content }}</div>
            
            <!-- 评价图片 -->
            <div class="review-images" v-if="review.images">
              <el-image
                v-for="(img, idx) in parseImages(review.images)"
                :key="idx"
                :src="img"
                :preview-src-list="[img]"
                class="review-image"
                fit="cover"
              />
            </div>
          </div>
          
          <!-- 商家回复 -->
          <div class="seller-reply-section" v-if="review.reply">
            <div class="reply-header">
              <el-icon><Shop /></el-icon>
              <span class="reply-label">商家回复</span>
              <span class="reply-time">{{ formatDate(review.reply.createdAt) }}</span>
            </div>
            <div class="reply-content">{{ review.reply.replyContent || review.reply.content }}</div>
          </div>
          
          <!-- 回复操作 -->
          <div class="reply-actions" v-else>
            <el-button 
              type="primary" 
              size="small"
              @click="openReplyDialog(review)"
            >
              回复评价
            </el-button>
          </div>
        </div>
        
        <!-- 空状态 -->
        <el-empty v-if="reviews.length === 0" description="暂无评价" />
      </div>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadReviews"
          @size-change="loadReviews"
        />
      </div>
    </el-card>

    <!-- 回复对话框 -->
    <el-dialog
      v-model="showReplyDialog"
      title="回复评价"
      :width="dialogWidth"
      :close-on-click-modal="false"
    >
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="评价内容">
          <div class="review-preview">{{ currentReview?.content }}</div>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容，礼貌回复可以提升用户好感度哦~"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showReplyDialog = false">取消</el-button>
          <el-button type="primary" @click="submitReply" :loading="submitting">提交回复</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { Search, User, Shop } from '@element-plus/icons-vue';
import { useSellerStore } from '@/stores/seller';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/utils/api';

const sellerStore = useSellerStore();
const reviews = ref([]);
const stats = reactive({
  total: 0,
  goodCount: 0,
  mediumCount: 0,
  badCount: 0,
  averageRating: 0,
  repliedCount: 0
});
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const searchKeyword = ref('');
const showReplyDialog = ref(false);
const currentReview = ref(null);
const replyForm = ref({
  reviewId: null,
  content: ''
});
const submitting = ref(false);

const isMobile = ref(window.innerWidth <= 768)
const updateIsMobile = () => { isMobile.value = window.innerWidth <= 768; }
onMounted(() => { window.addEventListener('resize', updateIsMobile); })
onUnmounted(() => { window.removeEventListener('resize', updateIsMobile); })
const dialogWidth = computed(() => isMobile.value ? '90%' : '600px')

// 获取商家 ID（从 Pinia store）
const getSellerId = () => {
  const sellerInfo = sellerStore.sellerInfo;
  if (sellerInfo && sellerInfo.id) {
    return sellerInfo.id;
  }
  // 如果没有登录，返回 null
  return null;
};

// 加载评价列表
const loadReviews = async () => {
  const sellerId = getSellerId();
  
  if (!sellerId) {
    ElMessage.error('请先登录');
    return;
  }
  
  try {
    const response = await api.getSellerReviews(sellerId, {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    });
    
    reviews.value = response.records || [];
      total.value = response.total || 0;
      
      await loadStats();
  } catch (error) {
    console.error('加载评价失败:', error);
    ElMessage.error({
      message: error.response?.data?.message || '加载评价失败，请稍后重试',
      duration: 3000
    });
  }
};

// 加载统计信息
const loadStats = async () => {
  const sellerId = getSellerId();
  
  if (!sellerId) {
    return;
  }
  
  try {
    const response = await api.getSellerReviewStats(sellerId);
    
    const data = response;
    stats.total = data.total || 0;
    stats.goodCount = data.goodCount || 0;
    stats.mediumCount = data.mediumCount || 0;
    stats.badCount = data.badCount || 0;
    stats.averageRating = data.averageRating || 0;
    stats.repliedCount = data.repliedCount || 0;
  } catch (error) {
    console.error('加载统计失败:', error);
  }
};

// 打开回复对话框
const openReplyDialog = (review) => {
  currentReview.value = review;
  replyForm.value = {
    reviewId: review.id,
    content: ''
  };
  showReplyDialog.value = true;
};

// 提交回复
const submitReply = async () => {
  if (!replyForm.value.content.trim()) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  
  const sellerId = getSellerId();
  if (!sellerId) {
    ElMessage.error('请先登录');
    return;
  }

  try {
    submitting.value = true;
    
    const response = await api.createReply({
      reviewId: currentReview.value.id,
      replyContent: replyForm.value.content,
      replyType: 1, // 1-商家回复
      replyBy: sellerId
    });
    
    if (response) {
      ElMessage.success({
        message: '回复成功！',
        duration: 2000
      });
      showReplyDialog.value = false;
      
      // 重新加载评价列表
      await loadReviews();
    } else {
      ElMessage.error({
        message: response.message || '提交失败',
        duration: 3000
      });
    }
  } catch (error) {
    console.error('提交回复失败:', error);
    ElMessage.error({
      message: error.response?.data?.message || '提交失败，请稍后重试',
      duration: 3000
    });
  } finally {
    submitting.value = false;
  }
};

// 解析图片
const parseImages = (imagesStr) => {
  if (!imagesStr) return [];
  try {
    if (typeof imagesStr === 'string') {
      return JSON.parse(imagesStr);
    }
    return imagesStr;
  } catch (e) {
    return [];
  }
};

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now - date;
  
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  
  if (diff < minute) {
    return '刚刚';
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前';
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前';
  } else {
    return date.toLocaleDateString('zh-CN');
  }
};

onMounted(() => {
  loadReviews();
});
</script>

<style scoped>
.review-management-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-value.good {
  color: #67C23A;
}

.stat-value.medium {
  color: #E6A23C;
}

.stat-value.bad {
  color: var(--color-error);
}

.stat-value.rating {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.review-list-card {
  margin-bottom: 20px;
}

.review-list {
  min-height: 400px;
}

.review-item {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reviewer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-500) 0%, var(--color-primary-600) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.reviewer-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.review-meta {
  display: flex;
  align-items: center;
  gap: 15px;
}

.review-time {
  font-size: 13px;
  color: #999;
}

.review-content {
  margin-bottom: 15px;
}

.review-text {
  font-size: 15px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 10px;
}

.review-images {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.review-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  cursor: pointer;
}

.seller-reply-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-top: 15px;
  border-left: 3px solid #4CAF50;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.reply-label {
  font-weight: 600;
}

.reply-time {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

.reply-content {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.reply-actions {
  margin-top: 15px;
}

.pagination {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
}

.review-preview {
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
  color: #666;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
  }
}
</style>
