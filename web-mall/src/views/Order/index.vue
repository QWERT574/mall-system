<template>
  <div class="order-page ml-page-enter">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">订单管理</h1>
        <p class="page-subtitle">查看和管理您的所有订单</p>
      </div>
    </div>

    <div v-if="loading" class="skeleton-section">
      <div class="stat-grid">
        <div class="stat-skeleton" v-for="i in 4" :key="i">
          <div class="skeleton-icon ml-shimmer"></div>
          <div class="skeleton-body">
            <div class="ml-shimmer ml-skeleton-text short"></div>
            <div class="ml-shimmer ml-skeleton-text medium"></div>
          </div>
        </div>
      </div>
      <div class="recent-skeleton">
        <div class="ml-shimmer" style="height:28px;width:140px;border-radius:var(--radius-sm);margin-bottom:var(--spacing-5)"></div>
        <div class="recent-item-skeleton" v-for="i in 3" :key="i">
          <div class="ml-shimmer ml-skeleton-avatar"></div>
          <div class="skeleton-lines">
            <div class="ml-shimmer ml-skeleton-text long"></div>
            <div class="ml-shimmer ml-skeleton-text short"></div>
          </div>
        </div>
      </div>
    </div>

    <template v-else>
      <section class="stat-section">
        <div class="stat-grid ml-stagger">
          <div
            class="stat-card ml-card-lift"
            v-for="card in statCards"
            :key="card.key"
            :class="'theme-' + card.theme"
            @click="goToOrderList(card.status)"
          >
            <div class="stat-icon-area" :style="{ background: card.gradient }">
              <span class="stat-emoji">{{ card.icon }}</span>
            </div>
            <div class="stat-info">
              <div class="stat-number ml-count-up">{{ stats[card.key] }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
            <div class="stat-arrow">
              <span>→</span>
            </div>
          </div>
        </div>
      </section>

      <section class="recent-section">
        <div class="section-header">
          <div class="section-title-group">
            <h2 class="section-title">最近订单</h2>
            <p class="section-subtitle">最新3笔订单动态</p>
          </div>
          <button class="section-more ml-btn-press" @click="goToOrderList()">
            查看全部
            <span class="more-arrow">→</span>
          </button>
        </div>

        <div v-if="recentOrders.length > 0" class="recent-list">
          <div
            class="recent-item ml-card-lift"
            v-for="order in recentOrders"
            :key="order.id"
            @click="goToOrderDetail(order.id)"
          >
            <div class="recent-item-image">
              <img
                :src="getOrderImage(order)"
                alt=""
                @error="handleImageError"
              />
            </div>
            <div class="recent-item-info">
              <div class="recent-item-top">
                <span class="recent-item-name">{{ getOrderName(order) }}</span>
                <span class="recent-item-price">¥{{ order.totalPrice || order.total || '0.00' }}</span>
              </div>
              <div class="recent-item-bottom">
                <span class="recent-item-sn">{{ order.orderSn || order.id }}</span>
                <span class="status-badge" :class="'status-' + order.status">{{ getStatusText(order.status) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">📋</div>
          <p class="empty-text">暂无订单记录</p>
          <p class="empty-hint">快去挑选心仪的农产品吧</p>
          <button class="empty-btn ml-btn-press" @click="$router.push('/product/list')">
            去逛逛
          </button>
        </div>
      </section>

      <section class="actions-section">
        <div class="actions-grid">
          <button class="action-card ml-card-lift" @click="$router.push('/order-list')">
            <div class="action-icon" style="background: var(--gradient-brand)">
              <span>📦</span>
            </div>
            <div class="action-text">
              <span class="action-title">查看全部订单</span>
              <span class="action-desc">管理所有订单</span>
            </div>
          </button>
          <button class="action-card ml-card-lift" @click="$router.push('/aftersale')">
            <div class="action-icon" style="background: var(--gradient-warm)">
              <span>🛡️</span>
            </div>
            <div class="action-text">
              <span class="action-title">售后服务</span>
              <span class="action-desc">退换货申请</span>
            </div>
          </button>
          <button class="action-card ml-card-lift" @click="$router.push('/service')">
            <div class="action-icon" style="background: var(--gradient-nature)">
              <span>💬</span>
            </div>
            <div class="action-text">
              <span class="action-title">联系客服</span>
              <span class="action-desc">在线咨询帮助</span>
            </div>
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();
const loading = ref(false);
const recentOrders = ref([]);

const stats = reactive({
  pendingPayment: 0,
  pendingShipment: 0,
  pendingReceipt: 0,
  completed: 0
});

const statCards = [
  {
    key: 'pendingPayment',
    label: '待付款',
    icon: '💳',
    theme: 'warm-orange',
    status: 0,
    gradient: 'linear-gradient(135deg, var(--warm-orange), var(--warm-orange-dark))'
  },
  {
    key: 'pendingShipment',
    label: '待发货',
    icon: '📦',
    theme: 'accent',
    status: 1,
    gradient: 'linear-gradient(135deg, var(--accent), var(--accent-dark))'
  },
  {
    key: 'pendingReceipt',
    label: '待收货',
    icon: '🚚',
    theme: 'nature-green',
    status: 2,
    gradient: 'linear-gradient(135deg, var(--nature-green), var(--nature-green-dark))'
  },
  {
    key: 'completed',
    label: '已完成',
    icon: '✅',
    theme: 'info',
    status: 3,
    gradient: 'linear-gradient(135deg, var(--info), var(--color-info-dark))'
  }
];

const statusMap = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已发货',
  5: '已发货',
  6: '已取消'
};

const getStatusText = (status) => statusMap[status] || '未知';

const goToOrderList = (status) => {
  const query = {};
  if (status !== undefined) query.status = status;
  router.push({ path: '/order-list', query });
};

const goToOrderDetail = (orderId) => {
  router.push(`/payment/${orderId}`);
};

const getOrderImage = (order) => {
  if (order.items && order.items.length > 0) {
    return order.items[0].productImage || order.items[0].cover || '';
  }
  return '';
};

const getOrderName = (order) => {
  if (order.items && order.items.length > 0) {
    const name = order.items[0].productName || order.items[0].name || '';
    if (order.items.length > 1) {
      return name + ` 等${order.items.length}件商品`;
    }
    return name;
  }
  return '订单商品';
};

const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 80 80"><rect fill="%23f5f0e8" width="80" height="80"/><text x="40" y="44" text-anchor="middle" fill="%23d4a574" font-size="24">🌾</text></svg>');
};

const loadOrderStats = async () => {
  loading.value = true;
  try {
    const response = await api.get('/order/list', { params: { page: 1, size: 999 } });
    const orders = response.data?.list || response.list || [];
    const list = Array.isArray(orders) ? orders : [];

    stats.pendingPayment = list.filter(o => o.status === 0).length;
    stats.pendingShipment = list.filter(o => o.status === 1).length;
    stats.pendingReceipt = list.filter(o => o.status === 2).length;
    stats.completed = list.filter(o => o.status === 3).length;
  } catch (error) {
    console.error('加载订单统计失败:', error);
    ElMessage.error('加载订单统计失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const loadRecentOrders = async () => {
  try {
    const response = await api.get('/order/list', { params: { page: 1, size: 3 } });
    const orders = response.data?.list || response.list || [];
    recentOrders.value = Array.isArray(orders) ? orders : [];
  } catch (error) {
    console.error('加载最近订单失败:', error);
    recentOrders.value = [];
  }
};

onMounted(() => {
  loadOrderStats();
  loadRecentOrders();
});
</script>

<style scoped>
.order-page {
  max-width: var(--grid-max-width);
  margin: 0 auto;
  padding: var(--spacing-6) var(--spacing-5) var(--spacing-12);
}

.page-header {
  margin-bottom: var(--spacing-8);
}

.page-title {
  font-family: var(--font-display);
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-2);
}

.page-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  margin: 0;
}

.stat-section {
  margin-bottom: var(--spacing-10);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-5);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  background: var(--card-bg);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5) var(--spacing-6);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all var(--transition-spring);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  opacity: 0;
  transition: opacity var(--transition-smooth);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-card:hover {
  border-color: var(--color-primary-200);
}

.stat-icon-area {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-glow-sm);
  transition: transform var(--transition-spring);
}

.stat-card:hover .stat-icon-area {
  transform: scale(1.08) rotate(3deg);
}

.stat-emoji {
  font-size: 26px;
  line-height: 1;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-number {
  font-family: var(--font-display);
  font-size: var(--font-size-3xl);
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: var(--spacing-1);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  font-weight: 500;
}

.stat-arrow {
  color: var(--text-tertiary);
  font-size: var(--font-size-lg);
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--transition-base);
}

.stat-card:hover .stat-arrow {
  opacity: 1;
  transform: translateX(0);
}

.theme-warm-orange .stat-card::before,
.stat-card.theme-warm-orange::before {
  background: linear-gradient(90deg, var(--warm-orange), var(--warm-orange-dark));
}

.stat-card.theme-warm-orange .stat-number {
  color: var(--warm-orange-dark);
}

.theme-accent .stat-card::before,
.stat-card.theme-accent::before {
  background: linear-gradient(90deg, var(--accent), var(--accent-dark));
}

.stat-card.theme-accent .stat-number {
  color: var(--accent-dark);
}

.theme-nature-green .stat-card::before,
.stat-card.theme-nature-green::before {
  background: linear-gradient(90deg, var(--nature-green), var(--nature-green-dark));
}

.stat-card.theme-nature-green .stat-number {
  color: var(--nature-green-dark);
}

.theme-info .stat-card::before,
.stat-card.theme-info::before {
  background: linear-gradient(90deg, var(--info), var(--color-info-dark));
}

.stat-card.theme-info .stat-number {
  color: var(--color-info-dark);
}

.recent-section {
  margin-bottom: var(--spacing-10);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--spacing-6);
}

.section-title-group {
  flex: 1;
}

.section-title {
  font-family: var(--font-display);
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-1);
}

.section-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  margin: 0;
}

.section-more {
  background: transparent;
  color: var(--accent-dark);
  padding: var(--spacing-2) var(--spacing-5);
  border-radius: var(--radius-full);
  border: 2px solid var(--accent);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 600;
  transition: all var(--transition-base);
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
}

.section-more:hover {
  background: var(--accent);
  color: white;
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow-md);
}

.more-arrow {
  transition: transform var(--transition-fast);
}

.section-more:hover .more-arrow {
  transform: translateX(3px);
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.recent-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  background: var(--card-bg);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4) var(--spacing-5);
  cursor: pointer;
  transition: all var(--transition-spring);
}

.recent-item:hover {
  border-color: var(--color-primary-200);
}

.recent-item-image {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-1);
}

.recent-item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recent-item-info {
  flex: 1;
  min-width: 0;
}

.recent-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2);
  gap: var(--spacing-3);
}

.recent-item-name {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-item-price {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--warm-orange);
  flex-shrink: 0;
}

.recent-item-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-3);
}

.recent-item-sn {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: 600;
  flex-shrink: 0;
  letter-spacing: 0.02em;
}

.status-badge.status-0 {
  background: var(--color-orange-50);
  color: var(--warm-orange-dark);
}

.status-badge.status-1 {
  background: var(--color-primary-50);
  color: var(--accent-dark);
}

.status-badge.status-2 {
  background: var(--color-green-50, #f0f9f0);
  color: var(--nature-green-dark);
}

.status-badge.status-3 {
  background: var(--color-info-light);
  color: var(--color-info-dark);
}

.status-badge.status-4,
.status-badge.status-5 {
  background: var(--color-primary-50);
  color: var(--accent-dark);
}

.status-badge.status-6 {
  background: var(--color-gray-100);
  color: var(--text-tertiary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-12) var(--spacing-6);
  background: var(--card-bg);
  border: 1px dashed var(--border);
  border-radius: var(--radius-lg);
}

.empty-icon {
  font-size: 56px;
  margin-bottom: var(--spacing-4);
  opacity: 0.6;
}

.empty-text {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-2);
}

.empty-hint {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  margin-bottom: var(--spacing-6);
}

.empty-btn {
  background: var(--gradient-brand);
  color: white;
  border: none;
  padding: var(--spacing-3) var(--spacing-8);
  border-radius: var(--radius-full);
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--shadow-glow-sm);
  transition: all var(--transition-spring);
}

.empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow-md);
}

.actions-section {
  margin-bottom: var(--spacing-8);
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-5);
}

.action-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  background: var(--card-bg);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5) var(--spacing-6);
  cursor: pointer;
  font-family: var(--font-body);
  text-align: left;
  transition: all var(--transition-spring);
}

.action-card:hover {
  border-color: var(--color-primary-200);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
  box-shadow: var(--shadow-glow-sm);
  transition: transform var(--transition-spring);
}

.action-card:hover .action-icon {
  transform: scale(1.08);
}

.action-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.skeleton-section {
  margin-bottom: var(--spacing-10);
}

.stat-grid .stat-skeleton {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  background: var(--card-bg);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5) var(--spacing-6);
}

.skeleton-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.skeleton-body {
  flex: 1;
}

.recent-skeleton {
  margin-top: var(--spacing-10);
}

.recent-item-skeleton {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  background: var(--card-bg);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4) var(--spacing-5);
  margin-bottom: var(--spacing-4);
}

.skeleton-lines {
  flex: 1;
}

@media (max-width: 1024px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .actions-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .order-page {
    padding: var(--spacing-4) var(--spacing-3) var(--spacing-8);
  }

  .page-title {
    font-size: var(--font-size-2xl);
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--spacing-3);
  }

  .stat-card {
    padding: var(--spacing-4);
    gap: var(--spacing-3);
  }

  .stat-icon-area {
    width: 44px;
    height: 44px;
  }

  .stat-emoji {
    font-size: 22px;
  }

  .stat-number {
    font-size: var(--font-size-2xl);
  }

  .actions-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-3);
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-3);
  }

  .recent-item {
    padding: var(--spacing-3) var(--spacing-4);
  }

  .recent-item-image {
    width: 52px;
    height: 52px;
  }
}

@media (max-width: 480px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--spacing-2);
  }

  .stat-card {
    flex-direction: column;
    align-items: flex-start;
    padding: var(--spacing-3);
    gap: var(--spacing-2);
  }

  .stat-arrow {
    display: none;
  }

  .stat-number {
    font-size: var(--font-size-xl);
  }

  .stat-label {
    font-size: var(--font-size-xs);
  }
}
</style>
