<template>
  <div class="activity-list-page">
    <div class="page-header">
      <h1 class="page-title">🌾 助农活动</h1>
      <p class="page-subtitle">参与助农活动，助力乡村振兴</p>
    </div>

    <div class="filter-section">
      <el-input
        v-model="searchQuery"
        placeholder="搜索活动名称"
        clearable
        @input="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      
      <el-select v-model="filterStatus" placeholder="活动状态" clearable @change="loadActivities" class="filter-select">
        <el-option label="进行中" :value="1" />
        <el-option label="已结束" :value="2" />
        <el-option label="未开始" :value="0" />
      </el-select>
    </div>

    <div class="activity-grid">
      <div 
        class="activity-card" 
        v-for="activity in activities" 
        :key="activity.id"
        @click="goToDetail(activity.id)"
      >
        <div class="card-image-wrapper">
          <img :src="activity.coverImage || ''" :alt="activity.name" class="card-image" loading="lazy" decoding="async" />
          <div class="status-tag" :class="getStatusClass(activity.status)">
            {{ getStatusText(activity.status) }}
          </div>
        </div>
        <div class="card-content">
          <h3 class="card-title">{{ activity.name }}</h3>
          <p class="card-desc">{{ activity.description }}</p>
          <div class="card-info">
            <div class="info-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatDate(activity.startTime) }} - {{ formatDate(activity.endTime) }}</span>
            </div>
            <div class="info-item">
              <el-icon><Location /></el-icon>
              <span>{{ activity.location }}</span>
            </div>
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span>{{ activity.currentParticipants || 0 }}/{{ activity.maxParticipants || 0 }}人参与</span>
            </div>
          </div>
          <div class="card-footer">
            <el-progress 
              :percentage="calculateProgress(activity)" 
              :stroke-width="8"
              :show-text="false"
              :color="getProgressColor(activity)"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-section" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[6, 12, 24, 36]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadActivities"
        @current-change="loadActivities"
      />
    </div>

    <el-empty v-if="!loading && activities.length === 0" description="暂无活动" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, Calendar, Location, User } from '@element-plus/icons-vue';
import api from '@/utils/api';

const router = useRouter();

const loading = ref(false);
const activities = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(12);
const searchQuery = ref('');
const filterStatus = ref(null);

const loadActivities = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    };
    
    if (filterStatus.value !== null) {
      params.status = filterStatus.value;
    }
    
    if (searchQuery.value) {
      params.keyword = searchQuery.value;
    }
    
    const res = await api.get('/activity/list', { params });
    
    if (res.code === 200 || res.code === 0) {
      activities.value = res.data.records || [];
      total.value = res.data.total || 0;
    } else {
      ElMessage.error(res.message || '加载活动列表失败');
    }
  } catch (error) {
    console.error('加载活动列表失败:', error);
    ElMessage.error('加载活动列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadActivities();
};

const goToDetail = (id) => {
  router.push(`/activity/${id}`);
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

const calculateProgress = (activity) => {
  const max = activity.maxParticipants || 1;
  const current = activity.currentParticipants || 0;
  return Math.min(100, Math.round((current / max) * 100));
};

const getProgressColor = (activity) => {
  const max = activity.maxParticipants || 1;
  const current = activity.currentParticipants || 0;
  const ratio = current / max;
  
  if (ratio >= 0.9) return '#f56c6c';
  if (ratio >= 0.7) return '#e6a23c';
  return '#67c23a';
};

onMounted(() => {
  loadActivities();
});
</script>

<style scoped>
.activity-list-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 120px);
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 32px;
  color: #1a1a1a;
  margin-bottom: 10px;
  font-weight: 600;
}

.page-subtitle {
  font-size: 16px;
  color: #666;
}

.filter-section {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
  justify-content: center;
}

.search-input {
  width: 400px;
}

.filter-select {
  width: 150px;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 25px;
  margin-bottom: 30px;
}

.activity-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.card-image-wrapper {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.activity-card:hover .card-image {
  transform: scale(1.05);
}

.status-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  backdrop-filter: blur(4px);
}

.status-not-started {
  background: rgba(103, 194, 58, 0.9);
}

.status-ongoing {
  background: rgba(64, 158, 255, 0.9);
}

.status-ended {
  background: rgba(144, 147, 153, 0.9);
}

.card-content {
  padding: 20px;
}

.card-title {
  font-size: 18px;
  color: #1a1a1a;
  margin-bottom: 10px;
  font-weight: 600;
  line-height: 1.4;
}

.card-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-info {
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #666;
}

.info-item .el-icon {
  color: #409eff;
  font-size: 16px;
}

.card-footer {
  margin-top: 10px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .activity-grid {
    grid-template-columns: 1fr;
  }
  
  .filter-section {
    flex-direction: column;
    align-items: center;
  }
  
  .search-input {
    width: 100%;
  }
}
</style>
