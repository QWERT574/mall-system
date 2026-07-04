<template>
  <div class="activity-detail-page" v-if="activity">
    <div class="activity-header">
      <div class="header-content">
        <div class="activity-cover">
          <img :src="activity.coverImage || ''" :alt="activity.name" />
        </div>
        <div class="activity-basic-info">
          <h1 class="activity-title">{{ activity.name }}</h1>
          <div class="activity-meta">
            <span class="status-tag" :class="getStatusClass(activity.status)">
              {{ getStatusText(activity.status) }}
            </span>
            <span class="view-count" v-if="activity.viewCount">
              <el-icon><View /></el-icon> {{ activity.viewCount }}次浏览
            </span>
          </div>
          <p class="activity-description">{{ activity.description }}</p>
          
          <div class="activity-stats">
            <div class="stat-item">
              <el-icon><User /></el-icon>
              <div class="stat-content">
                <span class="stat-value">{{ activity.currentParticipants || 0 }}/{{ activity.maxParticipants || 0 }}</span>
                <span class="stat-label">参与人数</span>
              </div>
            </div>
            <div class="stat-item">
              <el-icon><Calendar /></el-icon>
              <div class="stat-content">
                <span class="stat-value">{{ formatDate(activity.startTime) }}</span>
                <span class="stat-label">开始时间</span>
              </div>
            </div>
            <div class="stat-item">
              <el-icon><Location /></el-icon>
              <div class="stat-content">
                <span class="stat-value">{{ activity.location }}</span>
                <span class="stat-label">活动地点</span>
              </div>
            </div>
          </div>

          <div class="action-buttons" v-if="activity.status === 1">
            <el-button 
              type="primary" 
              size="large" 
              @click="joinActivity"
              :loading="joining"
              :disabled="isJoined || !canJoin"
            >
              {{ isJoined ? '已参与' : '立即参与' }}
            </el-button>
            <el-button size="large" @click="shareActivity">
              <el-icon><Share /></el-icon> 分享活动
            </el-button>
          </div>
          
          <el-alert
            v-if="!canJoin && activity.status === 1"
            type="warning"
            :title="getCannotJoinReason()"
            :closable="false"
            show-icon
          />
        </div>
      </div>
    </div>

    <div class="activity-content">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="活动详情" name="detail">
          <div class="detail-section">
            <h3 class="section-title">活动介绍</h3>
            <div class="detail-content" v-html="activity.details"></div>
            
            <div class="image-gallery" v-if="activity.images">
              <el-image
                v-for="(img, index) in activity.images.split(',')"
                :key="index"
                :src="img.trim()"
                :preview-src-list="activity.images.split(',').map(i => i.trim())"
                :initial-index="index"
                fit="cover"
                class="gallery-image"
              />
            </div>
          </div>

          <div class="rules-section">
            <h3 class="section-title">活动规则</h3>
            <div class="rules-content" v-html="activity.rules"></div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="参与名单" name="participants">
          <div class="participants-section">
            <div class="participant-list">
              <div v-if="participants.length > 0" class="list-grid">
                <div 
                  class="participant-item" 
                  v-for="participant in participants" 
                  :key="participant.id"
                >
                  <el-avatar :src="participant.avatar" :size="50" />
                  <div class="participant-info">
                    <div class="participant-name">{{ participant.nickname }}</div>
                    <div class="participant-time">{{ formatDateTime(participant.joinTime) }}</div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无参与者" />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="相关商品" name="products" v-if="relatedProducts.length > 0">
          <div class="products-section">
            <div class="product-grid">
              <div 
                class="product-card" 
                v-for="product in relatedProducts" 
                :key="product.id"
                @click="goToProduct(product.id)"
              >
                <img :src="product.mainImage" :alt="product.name" class="product-image" />
                <div class="product-info">
                  <h4 class="product-name">{{ product.name }}</h4>
                  <div class="product-price">¥{{ product.price }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>

  <div v-else-if="!loading" class="not-found">
    <el-empty description="活动不存在或已下架" />
    <el-button type="primary" @click="$router.push('/activity/list')">返回活动列表</el-button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Calendar, Location, View, Share } from '@element-plus/icons-vue';
import api from '@/utils/api';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const joining = ref(false);
const activity = ref(null);
const participants = ref([]);
const relatedProducts = ref([]);
const activeTab = ref('detail');
const isJoined = ref(false);

const canJoin = computed(() => {
  if (!activity.value) return false;
  const current = activity.value.currentParticipants || 0;
  const max = activity.value.maxParticipants || 0;
  return current < max;
});

const loadActivity = async () => {
  loading.value = true;
  try {
    const res = await api.get(`/activity/${route.params.id}`);
    if (res.code === 0 || res.code === 200) {
      activity.value = res.data;
      
      if (activity.value.relatedProductIds) {
        await loadRelatedProducts(activity.value.relatedProductIds);
      }
      
      await checkIfJoined();
    } else {
      ElMessage.error(res.message || '加载活动详情失败');
    }
  } catch (error) {
    console.error('加载活动详情失败:', error);
    ElMessage.error('加载活动详情失败');
  } finally {
    loading.value = false;
  }
};

const loadRelatedProducts = async (productIds) => {
  try {
    const ids = productIds.split(',').map(id => id.trim());
    const res = await api.get('/product/list-by-ids', {
      params: { ids: ids.join(',') }
    });
    if (res.code === 0 || res.code === 200) {
      relatedProducts.value = res.data || [];
    }
  } catch (error) {
    console.error('加载相关商品失败:', error);
  }
};

const checkIfJoined = async () => {
  try {
    const res = await api.get(`/activity/${route.params.id}/joined`);
    if (res.code === 0 || res.code === 200) {
      isJoined.value = res.data || false;
    }
  } catch (error) {
    console.error('检查参与状态失败:', error);
  }
};

const joinActivity = async () => {
  if (!canJoin.value) {
    ElMessage.warning('活动已满员');
    return;
  }

  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  if (!userInfo.id) {
    ElMessage.warning('请先登录后再参与活动');
    return;
  }

  joining.value = true;
  try {
    const res = await api.post(`/activity/${route.params.id}/join`, {
      userId: userInfo.id,
      participantName: userInfo.nickname || userInfo.username || '匿名用户',
      participantPhone: userInfo.phone || ''
    });
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('参与成功');
      isJoined.value = true;
      if (activity.value) {
        activity.value.currentParticipants = (activity.value.currentParticipants || 0) + 1;
      }
      loadParticipants();
    } else {
      ElMessage.error(res.message || '参与失败');
    }
  } catch (error) {
    console.error('参与活动失败:', error);
    ElMessage.error(error.response?.data?.msg || '参与失败');
  } finally {
    joining.value = false;
  }
};

const loadParticipants = async () => {
  try {
    const res = await api.get(`/activity/${route.params.id}/participants`, {
      params: { page: 1, size: 20 }
    });
    if (res.code === 0 || res.code === 200) {
      participants.value = res.data.records || [];
    }
  } catch (error) {
    console.error('加载参与者列表失败:', error);
  }
};

const shareActivity = () => {
  const url = window.location.href;
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板');
  }).catch(() => {
    ElMessage.warning('复制失败，请手动复制');
  });
};

const goToProduct = (id) => {
  router.push(`/product/${id}`);
};

const getStatusClass = (status) => {
  const map = {
    0: 'status-not-started',
    1: 'status-ongoing',
    2: 'status-ended'
  };
  return map[status] || '';
};

const getStatusText = (status) => {
  const map = {
    0: '未开始',
    1: '进行中',
    2: '已结束'
  };
  return map[status] || '未知';
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const getCannotJoinReason = () => {
  if (!activity.value) return '';
  const current = activity.value.currentParticipants || 0;
  const max = activity.value.maxParticipants || 0;
  if (current >= max) {
    return '活动参与人数已满';
  }
  return '暂时无法参与';
};

onMounted(() => {
  loadActivity();
  loadParticipants();
});
</script>

<style scoped>
.activity-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.activity-header {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.header-content {
  display: grid;
  grid-template-columns: 500px 1fr;
  gap: 30px;
  padding: 30px;
}

.activity-cover {
  border-radius: 8px;
  overflow: hidden;
  height: 400px;
}

.activity-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.activity-basic-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.activity-title {
  font-size: 28px;
  color: #1a1a1a;
  font-weight: 600;
  margin: 0;
}

.activity-meta {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
}

.status-not-started {
  background: #67c23a;
}

.status-ongoing {
  background: #409eff;
}

.status-ended {
  background: var(--color-text-tertiary);
}

.view-count {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #666;
}

.activity-description {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.activity-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-item .el-icon {
  font-size: 28px;
  color: #409eff;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 18px;
  color: #1a1a1a;
  font-weight: 600;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.action-buttons {
  display: flex;
  gap: 15px;
  padding-top: 10px;
}

.activity-content {
  background: #fff;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.detail-section,
.rules-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 20px;
  color: #1a1a1a;
  margin-bottom: 20px;
  font-weight: 600;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}

.detail-content,
.rules-content {
  font-size: 15px;
  color: #333;
  line-height: 1.8;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
  margin-top: 20px;
}

.gallery-image {
  width: 100%;
  height: 200px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.3s;
}

.gallery-image:hover {
  transform: scale(1.05);
}

.participants-section {
  padding: 20px 0;
}

.list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.participant-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  background: var(--color-bg-page);
  border-radius: 8px;
}

.participant-info {
  flex: 1;
}

.participant-name {
  font-size: 15px;
  color: #1a1a1a;
  font-weight: 500;
  margin-bottom: 5px;
}

.participant-time {
  font-size: 13px;
  color: #999;
}

.products-section {
  padding: 20px 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.product-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.product-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.product-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 15px;
  color: #1a1a1a;
  margin-bottom: 10px;
  font-weight: 500;
}

.product-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: 600;
}

.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 20px;
}

@media (max-width: 992px) {
  .header-content {
    grid-template-columns: 1fr;
  }
  
  .activity-cover {
    height: 300px;
  }
  
  .activity-stats {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>
