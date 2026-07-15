<template>
  <div class="aftersale-container">
    <h2 class="page-title">售后服务</h2>

    <div class="tabs">
      <div class="tab-item"
           :class="{ active: activeTab === 'all' }"
           @click="switchTab('all')">全部</div>
      <div class="tab-item"
           :class="{ active: activeTab === '0' }"
           @click="switchTab('0')">待处理</div>
      <div class="tab-item"
           :class="{ active: activeTab === '1' }"
           @click="switchTab('1')">处理中</div>
      <div class="tab-item"
           :class="{ active: activeTab === '2' }"
           @click="switchTab('2')">已解决</div>
      <div class="tab-item"
           :class="{ active: activeTab === '3' }"
           @click="switchTab('3')">已关闭</div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">
      <div class="empty-icon">📦</div>
      <p>暂无售后记录</p>
      <button class="btn-back" @click="goToOrders">去订单列表</button>
    </div>
    <div v-else class="aftersale-list">
      <div class="aftersale-card" v-for="item in list" :key="item.id" @click="viewDetail(item.id)">
        <div class="card-header">
          <span class="service-type">{{ serviceTypeText(item.serviceType) }}</span>
          <span :class="['status-tag', statusClass(item.status)]">{{ statusText(item.status) }}</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="label">售后编号：</span>
            <span class="value">{{ item.id }}</span>
          </div>
          <div class="info-row">
            <span class="label">申请时间：</span>
            <span class="value">{{ formatTime(item.createdAt) }}</span>
          </div>
          <div class="info-row" v-if="item.refundAmount">
            <span class="label">退款金额：</span>
            <span class="value price">¥{{ item.refundAmount }}</span>
          </div>
          <div class="reason-text">{{ item.reason }}</div>
        </div>
        <div class="card-footer">
          <span class="arrow">查看详情 →</span>
        </div>
      </div>
    </div>

    <div class="pagination" v-if="total > size">
      <span :class="{ disabled: current <= 1 }" @click="changePage(current - 1)">上一页</span>
      <span class="page-info">{{ current }}/{{ pages }}</span>
      <span :class="{ disabled: current >= pages }" @click="changePage(current + 1)">下一页</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '@/utils/api';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const list = ref([]);
const loading = ref(false);
const activeTab = ref('all');
const current = ref(1);
const size = ref(10);
const total = ref(0);
const pages = ref(0);

const serviceTypeText = (type) => {
  const map = { 1: '退货退款', 2: '换货', 3: '维修' };
  return map[type] || '其他';
};

const statusText = (status) => {
  const map = { 0: '待处理', 1: '处理中', 2: '已解决', 3: '已关闭' };
  return map[status] || '未知';
};

const statusClass = (status) => {
  const map = { 0: 'status-pending', 1: 'status-processing', 2: 'status-resolved', 3: 'status-closed' };
  return map[status] || '';
};

const formatTime = (time) => {
  if (!time) return '';
  if (Array.isArray(time)) {
    return `${time[0]}-${String(time[1]).padStart(2, '0')}-${String(time[2]).padStart(2, '0')} ${String(time[3]).padStart(2, '0')}:${String(time[4]).padStart(2, '0')}`;
  }
  return time.replace('T', ' ').substring(0, 16);
};

const switchTab = (tab) => {
  activeTab.value = tab;
  current.value = 1;
  loadList();
};

const changePage = (page) => {
  if (page < 1 || page > pages.value) return;
  current.value = page;
  loadList();
};

const loadList = async () => {
  const userStore = useUserStore();
  const userId = userStore.userInfo?.id;
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }

  loading.value = true;
  try {
    const statusFilter = activeTab.value === 'all' ? null : parseInt(activeTab.value);
    const res = await api.getAfterSalesByUserIdWithPage(userId, current.value, size.value, statusFilter);
    if (res.code === 0) {
      list.value = res.data.records || [];
      total.value = res.data.total || 0;
      pages.value = res.data.pages || 0;
    } else {
      ElMessage.error(res.message || '加载失败');
    }
  } catch (err) {
    console.error('加载售后列表失败:', err);
    ElMessage.error('加载售后列表失败');
  } finally {
    loading.value = false;
  }
};

const viewDetail = (id) => {
  router.push(`/aftersale/${id}`);
};

const goToOrders = () => {
  router.push('/order-list');
};

onMounted(() => {
  loadList();
});
</script>

<style scoped>
.aftersale-container { max-width: 800px; margin: 0 auto; padding: 16px; }
.page-title { font-size: 20px; font-weight: 600; color: #1a1a1a; margin: 0 0 16px; }

.tabs { display: flex; gap: 0; margin-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.tab-item {
  padding: 10px 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}
.tab-item:hover { color: #1890ff; }
.tab-item.active { color: #1890ff; border-bottom-color: #1890ff; font-weight: 500; }

.aftersale-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  overflow: hidden;
}
.aftersale-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.service-type { font-size: 14px; font-weight: 500; color: #333; }

.status-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}
.status-pending { background: #fff7e6; color: #d48806; }
.status-processing { background: #e6f7ff; color: #1890ff; }
.status-resolved { background: #f6ffed; color: #52c41a; }
.status-closed { background: #f5f5f5; color: #999; }

.card-body { padding: 12px 16px; }
.info-row { margin-bottom: 6px; font-size: 13px; }
.label { color: #999; }
.value { color: #333; }
.price { color: #f5222d; font-weight: 500; }
.reason-text {
  font-size: 13px;
  color: #666;
  margin-top: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  padding: 8px 16px;
  border-top: 1px solid #f5f5f5;
  text-align: right;
}
.arrow { font-size: 13px; color: #1890ff; }

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}
.pagination span {
  padding: 6px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
}
.pagination span.disabled { color: #d9d9d9; cursor: not-allowed; }
.pagination .page-info { border: none; color: #666; cursor: default; }

.loading, .empty { text-align: center; padding: 60px 0; color: #999; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty p { font-size: 14px; margin-bottom: 16px; }
.btn-back {
  padding: 8px 24px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.btn-back:hover { background: #40a9ff; }

@media (max-width: 768px) {
  .aftersale-container { padding: 10px; }
  .tabs { flex-wrap: wrap; }
  .tab-item { padding: 8px 14px; font-size: 13px; }
  .card-header { flex-direction: column; align-items: flex-start; gap: 6px; }
  .info-row { display: flex; flex-direction: column; gap: 2px; }
  .pagination span { padding: 5px 10px; font-size: 12px; }
}

@media (max-width: 480px) {
  .aftersale-container { padding: 6px; }
  .page-title { font-size: 17px; }
  .tab-item { padding: 6px 10px; font-size: 12px; }
  .card-body { padding: 10px 12px; }
  .card-header { padding: 10px 12px; }
  .card-footer { padding: 6px 12px; }
  .info-row { font-size: 12px; }
  .reason-text { font-size: 12px; }
  .arrow { font-size: 12px; }
}
</style>