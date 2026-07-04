<template>
  <div class="order-list-container ml-page-enter">
    <h2 class="page-title">订单列表</h2>

    <div class="filter-bar">
      <div class="search-box">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索订单号"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <button @click="handleSearch" class="search-btn">搜索</button>
      </div>

      <div class="filter-group">
        <select v-model="statusFilter" @change="handleFilter" class="filter-select">
          <option value="">全部状态</option>
          <option value="0">待付款</option>
          <option value="1">待发货</option>
          <option value="2">待收货</option>
          <option value="3">已完成</option>
          <option value="4">已取消</option>
        </select>

        <button v-if="hasFilter" @click="resetFilter" class="reset-btn">重置</button>
      </div>
    </div>

    <div v-if="loading" class="skeleton-wrapper">
      <div class="skeleton-card" v-for="i in 3" :key="i">
        <div class="skeleton-header"></div>
        <div class="skeleton-body">
          <div class="skeleton-img"></div>
          <div class="skeleton-lines">
            <div class="skeleton-line w70"></div>
            <div class="skeleton-line w40"></div>
          </div>
        </div>
        <div class="skeleton-footer"></div>
      </div>
    </div>

    <div v-else-if="orders.length === 0" class="empty-state">
      <svg class="empty-illustration" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="40" y="60" width="120" height="90" rx="8" stroke="var(--color-gray-300)" stroke-width="2" fill="var(--color-gray-50)"/>
        <path d="M40 80 L100 120 L160 80" stroke="var(--color-gray-300)" stroke-width="2" fill="none"/>
        <circle cx="100" cy="40" r="20" stroke="var(--color-gray-300)" stroke-width="2" fill="var(--color-gray-50)"/>
        <path d="M94 40 L100 46 L108 36" stroke="var(--color-primary-500)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
      </svg>
      <p class="empty-text">暂无订单</p>
      <button @click="router.push('/')" class="empty-action">去逛逛</button>
    </div>

    <div v-else>
      <div class="order-list">
        <div class="order-item" v-for="order in orders" :key="order.id" @click="viewDetail(order.id)">
          <div class="order-header">
            <span>订单号：{{ order.orderSn || order.id }}</span>
            <span :class="['order-status', 'status-badge', getStatusClass(order.status)]">{{ getStatusText(order.status) }}</span>
          </div>
          <div class="order-items">
            <div class="order-product" v-for="item in order.items" :key="item.id">
              <img :src="item.productImage || item.cover" :alt="item.productName || item.name" loading="lazy" decoding="async" />
              <div class="product-info">
                <h4>{{ item.productName || item.name }}</h4>
                <p>¥{{ item.price }} × {{ item.quantity || item.count }}</p>
              </div>
            </div>
          </div>
          <div class="order-footer">
            <span class="total">合计：¥{{ order.totalPrice || order.total }}</span>
            <div class="action-buttons">
              <button v-if="order.status === 0" @click.stop="goToPayment(order.id)" class="btn-pay">立即支付</button>
              <button @click.stop="viewDetail(order.id)" class="btn-detail">查看详情</button>
              <button v-if="order.status === 0" @click.stop="cancelOrder(order.id)" class="btn-cancel">取消订单</button>
              <button v-if="order.status === 2" @click.stop="confirmOrder(order.id)" class="btn-confirm">确认收货</button>
              <button v-if="order.status === 3" @click.stop="reviewOrder(order)" class="btn-review">评价</button>
              <button @click.stop="contactService(order)" class="btn-service">客服</button>
              <button v-if="order.status >= 2" @click.stop="applyAfterSale(order)" class="btn-aftersale">售后</button>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button class="page-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();
const orders = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const totalPages = ref(1);
const total = ref(0);
const pageSize = 10;

const searchKeyword = ref('');
const statusFilter = ref('');

const hasFilter = computed(() => {
  return searchKeyword.value || statusFilter.value;
});

const getStatusText = (status) => {
  const statusMap = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' };
  return statusMap[status] || '未知';
};

const getStatusClass = (status) => {
  const classMap = { 0: 'status-pending', 1: 'status-paid', 2: 'status-shipped', 3: 'status-completed', 4: 'status-canceled' };
  return classMap[status] || '';
};

const goToPayment = (orderId) => {
  router.push(`/payment/${orderId}`);
};

const viewDetail = (orderId) => {
  router.push(`/payment/${orderId}`);
};

const cancelOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定要取消订单吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    });
    const res = await api.post(`/order/cancel/${orderId}`);
    if (res.code === 0) {
      ElMessage.success('订单已取消');
      loadOrders();
    } else {
      ElMessage.error(res.message || '取消订单失败');
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('取消订单失败，请稍后重试');
    }
  }
};

const confirmOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定已收到商品？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    });
    const res = await api.post(`/order/confirm/${orderId}`);
    if (res.code === 0) {
      ElMessage.success('订单已完成');
      loadOrders();
    } else {
      ElMessage.error(res.message || '确认收货失败');
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('确认收货失败，请稍后重试');
    }
  }
};

const reviewOrder = (order) => {
  router.push(`/review/order/${order.id}`);
};

const contactService = (order) => {
  const firstItem = order.items && order.items[0]
  const sellerId = firstItem?.sellerId
  const productId = firstItem?.productId
  const query = {}
  if (sellerId) query.sellerId = sellerId
  if (productId) query.productId = productId
  if (order.id) query.orderId = order.id
  router.push({ path: '/service', query })
};

const applyAfterSale = (order) => {
  router.push(`/aftersale/create?orderId=${order.id}`);
};

const handleSearch = () => {
  currentPage.value = 1;
  loadOrders();
};

const handleFilter = () => {
  currentPage.value = 1;
  loadOrders();
};

const resetFilter = () => {
  searchKeyword.value = '';
  statusFilter.value = '';
  currentPage.value = 1;
  loadOrders();
};

const changePage = (page) => {
  currentPage.value = page;
  loadOrders();
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const loadOrders = async () => {
  loading.value = true;
  try {
    const params = { page: currentPage.value, size: pageSize };
    if (statusFilter.value) params.status = statusFilter.value;
    if (searchKeyword.value) params.keyword = searchKeyword.value;

    const response = await api.get('/order/list', { params });

    const list = response.list || response.data?.list || [];
    orders.value = list;
    total.value = response.total || response.data?.total || 0;
    totalPages.value = response.pages || response.data?.pages || 1;
    currentPage.value = response.current || response.data?.current || 1;
  } catch (error) {
    console.error('加载订单失败:', error);
    orders.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadOrders();
});
</script>

<style scoped>
.order-list-container { max-width: 960px; margin: 0 auto; padding: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: var(--text-primary); margin: 0 0 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-gray-200); }

.filter-bar {
  background: var(--card-bg); padding: 12px; margin-bottom: 12px;
  display: flex; flex-direction: column; gap: 10px;
  border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm);
}

.search-box { display: flex; gap: 0; }
.search-input {
  flex: 1; padding: 7px 12px; border: 1px solid var(--color-gray-300); border-right: none;
  border-radius: var(--radius-sm) 0 0 var(--radius-sm); font-size: 14px; outline: none;
  background: var(--card-bg); color: var(--text-primary);
}
.search-input:focus { border-color: var(--accent); }
.search-btn {
  padding: 7px 16px; background: var(--accent); color: var(--color-text-inverse); border: 1px solid var(--accent);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0; cursor: pointer; font-size: 14px;
}
.search-btn:hover { background: var(--accent-light); }

.filter-group { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.filter-select {
  padding: 6px 28px 6px 10px; border: 1px solid var(--color-gray-300); border-radius: var(--radius-sm);
  font-size: 14px; background: var(--card-bg); cursor: pointer; outline: none; color: var(--text-primary);
}
.filter-select:focus { border-color: var(--accent); }
.reset-btn {
  padding: 6px 14px; background: var(--card-bg); color: var(--text-secondary); border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-sm); cursor: pointer; font-size: 14px;
}
.reset-btn:hover { color: var(--accent); border-color: var(--accent); }

.order-item {
  background: var(--card-bg); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm);
  margin-bottom: 10px; overflow: hidden; cursor: pointer; transition: all var(--transition-base);
}
.order-item:hover { border-color: var(--accent); box-shadow: var(--shadow-glow-sm); }

.order-header {
  display: flex; justify-content: space-between; padding: 10px 14px;
  background: var(--color-gray-50); font-size: 13px; color: var(--text-secondary); border-bottom: 1px solid var(--color-gray-200);
}
.order-status { font-weight: 500; font-size: 13px; }

.status-badge {
  padding: 2px 10px; border-radius: var(--radius-full); font-size: 12px; font-weight: 500;
}
.status-badge.status-pending { color: var(--color-orange-500); background: var(--color-orange-50); }
.status-badge.status-paid { color: var(--color-primary-500); background: var(--color-primary-50); }
.status-badge.status-shipped { color: var(--color-green-500); background: var(--color-green-50); }
.status-badge.status-completed { color: var(--color-info-dark); background: var(--color-info-light); }
.status-badge.status-canceled { color: var(--color-gray-500); background: var(--color-gray-100); }

.order-items { background: var(--card-bg); }
.order-product { display: flex; gap: 12px; padding: 12px 14px; border-bottom: 1px solid var(--color-gray-100); }
.order-product:last-child { border-bottom: none; }
.order-product img { width: 64px; height: 64px; object-fit: cover; border-radius: 2px; background: var(--color-gray-50); border: 1px solid var(--color-gray-200); }
.product-info h4 { font-size: 14px; font-weight: 400; margin: 0 0 4px; color: var(--text-primary); }
.product-info p { color: var(--text-tertiary); font-size: 13px; margin: 0; }

.order-footer {
  padding: 10px 14px; display: flex; justify-content: space-between;
  align-items: center; border-top: 1px solid var(--color-gray-200);
}
.total { font-size: 15px; font-weight: 500; color: var(--color-error); }
.action-buttons { display: flex; gap: 8px; }
.action-buttons button {
  padding: 5px 14px; border: 1px solid var(--color-gray-300); border-radius: var(--radius-sm);
  cursor: pointer; font-size: 13px; background: var(--card-bg); color: var(--text-primary);
  transition: all var(--transition-fast);
}
.btn-pay { background: var(--accent); color: var(--color-text-inverse); border-color: var(--accent); }
.btn-pay:hover { background: var(--accent-light); }
.btn-detail:hover { color: var(--accent); border-color: var(--accent); }
.btn-cancel { color: var(--color-error); border-color: var(--color-error); }
.btn-cancel:hover { background: var(--color-error); color: var(--color-text-inverse); }
.btn-confirm { background: var(--nature-green); color: var(--color-text-inverse); border-color: var(--nature-green); }
.btn-confirm:hover { background: var(--nature-green-light); }
.btn-review { color: var(--accent); border-color: var(--accent); }
.btn-review:hover { background: var(--accent); color: var(--color-text-inverse); }
.btn-service:hover { color: var(--accent); border-color: var(--accent); }
.btn-aftersale { padding: 5px 12px; font-size: 13px; border: 1px solid var(--warm-orange); background: var(--card-bg); color: var(--warm-orange); border-radius: var(--radius-sm); cursor: pointer; }
.btn-aftersale:hover { color: var(--color-text-inverse); background: var(--warm-orange); }

.pagination {
  display: flex; justify-content: center; align-items: center;
  gap: 16px; padding: 20px 0; margin-top: 8px;
}
.page-btn {
  padding: 6px 16px; border: 1px solid var(--color-gray-300); border-radius: var(--radius-sm);
  background: var(--card-bg); cursor: pointer; font-size: 14px; color: var(--text-primary);
}
.page-btn:hover:not(:disabled) { color: var(--accent); border-color: var(--accent); }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-info { font-size: 14px; color: var(--text-secondary); }

.skeleton-wrapper { display: flex; flex-direction: column; gap: 10px; }
.skeleton-card {
  background: var(--card-bg); border: 1px solid var(--color-gray-200); border-radius: var(--radius-sm);
  overflow: hidden;
}
.skeleton-header {
  height: 40px; background: var(--color-gray-50); border-bottom: 1px solid var(--color-gray-200);
  background-image: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-50) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.skeleton-body { display: flex; gap: 12px; padding: 12px 14px; }
.skeleton-img {
  width: 64px; height: 64px; border-radius: 2px; background: var(--color-gray-100);
  background-image: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-50) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.skeleton-lines { flex: 1; display: flex; flex-direction: column; gap: 8px; justify-content: center; }
.skeleton-line {
  height: 14px; border-radius: 4px; background: var(--color-gray-100);
  background-image: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-50) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.skeleton-line.w70 { width: 70%; }
.skeleton-line.w40 { width: 40%; }
.skeleton-footer {
  height: 44px; background: var(--color-gray-50); border-top: 1px solid var(--color-gray-200);
  background-image: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-50) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-state { text-align: center; padding: 60px 20px; }
.empty-illustration { width: 160px; height: 160px; margin: 0 auto 20px; }
.empty-text { font-size: 16px; color: var(--text-tertiary); margin: 0 0 20px; }
.empty-action {
  display: inline-block; padding: 10px 28px; background: var(--accent); color: var(--color-text-inverse);
  border: none; border-radius: var(--radius-full); font-size: 14px; font-weight: 500;
  cursor: pointer; transition: all var(--transition-base);
}
.empty-action:hover { background: var(--accent-dark); transform: translateY(-2px); box-shadow: var(--shadow-glow-md); }

@media (max-width: 768px) {
  .order-list-container { padding: 10px; }
  .filter-bar { padding: 10px; }
  .filter-group { flex-direction: column; align-items: stretch; }
  .filter-select { width: 100%; }
  .order-header { flex-direction: column; gap: 4px; align-items: flex-start; }
  .order-product { flex-direction: column; gap: 8px; }
  .order-product img { width: 56px; height: 56px; }
  .order-footer { flex-direction: column; gap: 10px; align-items: flex-start; }
  .action-buttons { flex-wrap: wrap; width: 100%; }
  .action-buttons button { flex: 1; min-width: 0; padding: 5px 8px; font-size: 12px; }
  .btn-aftersale { flex: 1; min-width: 0; padding: 5px 8px; font-size: 12px; }
}

@media (max-width: 480px) {
  .order-list-container { padding: 6px; }
  .page-title { font-size: 16px; }
  .order-header { font-size: 12px; padding: 8px 10px; }
  .order-product { padding: 10px; }
  .product-info h4 { font-size: 13px; }
  .product-info p { font-size: 12px; }
  .order-footer { padding: 8px 10px; }
  .total { font-size: 14px; }
  .action-buttons button { font-size: 11px; padding: 4px 6px; }
  .btn-aftersale { font-size: 11px; padding: 4px 6px; }
  .pagination { gap: 10px; padding: 14px 0; }
  .page-btn { padding: 5px 10px; font-size: 13px; }
  .page-info { font-size: 13px; }
}
</style>
